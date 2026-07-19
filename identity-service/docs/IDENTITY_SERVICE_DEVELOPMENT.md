# identity-service Geliştirme Süreci

Bu doküman, `identity-service`'in baştan bugüne kadar hangi adımlarla, hangi gerekçelerle geliştirildiğini anlatır. Kod içermez; her adımda ne yapıldığı ve **neden** o şekilde yapıldığı anlatılır.

---

## 1. Mimari Çelişkinin Çözülmesi: Keycloak'ı İkame Etmek mi, Tamamlamak mı?

Sistem tasarım dokümanı, identity-service'in kendi login/JWT/refresh-token-rotation mekanizmasını üreten bağımsız bir kimlik sağlayıcı olmasını öngörüyordu. Ancak geliştirmeye başlanan andaki gerçek durum farklıydı: `order-service`, `payment-service`, `usage-service` gibi tüm servisler **zaten** Keycloak üzerinden OAuth2/JWT kullanıyordu; Keycloak realm'i (`telcocrm-gygy5`), her servis için service-account client'ları ve frontend için Authorization Code flow ile birlikte kurulu ve çalışır durumdaydı. Dokümanın öngördüğü modelin aynen uygulanması, gateway/order-service/payment-service/bff-server/frontend'deki **tüm** kimlik doğrulama zincirinin yeniden yazılmasını gerektirecek, büyük ve riskli bir migration anlamına geliyordu.

Bu çelişki koda geçilmeden önce netleştirildi ve iki seçenek arasında karar verildi:

- **(a) Keycloak'ı tamamen ikame eden bağımsız servis** — dokümanın orijinal önerisi, ama mevcut mimariyle doğrudan çatışıyor.
- **(b) Keycloak'ın yanında/üstünde tamamlayıcı servis** — identity-service yalnızca User/Role/Permission yönetimini (CRUD + iş kuralları) üstlenir; login, JWT üretimi, refresh token rotation Keycloak'ta kalır.

**(b) seçildi.** Gerekçe: mevcut mimari zaten çalışıyor ve dört farklı bileşen (gateway, order-service, payment-service, bff-server) bu mimariye bağımlı; bunların hepsini aynı anda değiştirmek, identity-service'in asıl katma değeri olan "User/Role/Permission yönetimi" ihtiyacına kıyasla orantısız bir risk taşıyor. Bu kararın doğrudan sonucu olarak, dokümandaki "JWT üretimi" ve "reuse detection" maddeleri **kapsam dışı** bırakıldı — bunlar zaten Keycloak'ın sorumluluğunda.

### Refresh Token Rotation ve Reuse Detection: Keycloak'ın Native Session Mekanizması

"Reuse detection", identity-service'in (veya başka bir bileşenin) ayrıca inşa ettiği bir blacklist yapısı değil — realm seviyesinde iki ayarla (`revokeRefreshToken: true`, `refreshTokenMaxReuse: 2`) etkinleştirilen, Keycloak'ın kendi `UserSession` state'i üzerinden yürüyen native bir davranış olacak şekilde tasarlandı. Mekanizma şöyle işliyor: her refresh grant'inde eski refresh token rotate edilir (yeni bir refresh token üretilir, eskisi geçersiz sayılır); eski (rotate edilmiş) bir refresh token, izin verilen tekrar kullanım sayısının ötesinde kullanılmaya çalışılırsa Keycloak `invalid_grant` / "Maximum allowed refresh token reuse exceeded" ile reddeder **ve o refresh token'ın bağlı olduğu `UserSession`'ı komple düşürür** — bu session'a ait tüm token'lar (o oturumdaki access + refresh) tek seferde geçersiz olur. Bu iptal **session bazlı**: kullanıcının başka bir cihazda/tarayıcıda açık olan ayrı bir oturumu (ayrı bir `UserSession`), reuse tespit edilen oturumdan bağımsız olarak etkilenmeden çalışmaya devam eder.

Bu mekanizma tamamen Keycloak'ın kendi iç session state'inde (Infinispan cache, gerekirse JDBC persistence) yönetiliyor — Redis'in bu sürece hiçbir katkısı yok. Bu, (b) kararının doğal bir uzantısı: login/JWT/refresh-token-rotation Keycloak'ta kaldığı için, rotation'ın güvenlik garantisi olan reuse detection da Keycloak'ın realm ayarları üzerinden, identity-service tarafında ek kod/altyapı (Redis dahil) gerektirmeden sağlanıyor.

**`refreshTokenMaxReuse: 2` neden sıfır değil.** Rotation'ın güvenlik amacı, çalınmış bir refresh token'ın sessizce kullanılabilmesini engellemek — bunun için "reuse" kavramının var olması (yani bir token'ın belirli bir kullanım sayısından sonra kesin olarak geçersiz sayılması) yeterli, kullanım sayısının **tam olarak 1** olması şart değil. bff-server tarayıcıdan gelen istekleri Keycloak'a arka planda relay ederken, tek bir sayfa geçişi (örn. Overview) birden fazla API çağrısını **paralel** ateşleyebiliyor; bu paralel çağrıların hepsi, henüz rotate olmamış aynı refresh token'ı eşzamanlı okuyabiliyor. `refreshTokenMaxReuse: 0` ile bu tamamen normal, kötü niyetli olmayan eşzamanlılık dahi "reuse" sayılıp reddediliyor ve session'ın düşmesine (kullanıcının beklenmedik şekilde login'e atılmasına) yol açabiliyor. `2` değeri, bu tür kısa süreli/az sayıdaki eşzamanlı çakışmalara **sınırlı bir tolerans** tanıyor; reuse detection'ın kendisi (yani "sonsuza kadar/istenildiği kadar reuse serbest" değil, "belirli bir sınırın ötesi kesin reddedilir ve session düşer" garantisi) aynen korunuyor — sadece sınır 1 yerine 3 (orijinal kullanım + 2 tekrar) oluyor.

**`refreshTokenMaxReuse: 2` tek başına yeterli değildi — asıl kök neden bff-server'daydı.** İlk canlı Playwright testinde bu ayar tek başına denendi: Overview sayfası 5 paralel istek attığında eşzamanlı çakışma sıklığı gözle görülür şekilde azaldı (4 paralel reddedilen istekten 1-2'ye düştü) ama **ortadan kalkmadı** — N tane gerçekten eşzamanlı istek aynı bayat refresh token'ı okuduğunda, `tolerans+1`'den fazlası (5 paralel istekte en az 2 tanesi) yine reddediliyordu. Kök neden bu ayarda değil, bff-server'daydı: Spring Cloud Gateway'in `TokenRelay` filtresi (bff-server `spring-cloud-starter-gateway-server-webmvc` kullanıyor, yani blocking/servlet mimarisi — reactive/WebFlux değil), proxy edilen **her** istekte `OAuth2AuthorizedClientManager.authorize()`'ı kendi servlet thread'inden, hiçbir senkronizasyon olmadan çağırıyordu. Overview'un 5 paralel isteği 5 ayrı servlet thread'i olarak aynı anda bu metodu çağırınca, hepsi aynı bayat refresh token'ı görüp eşzamanlı refresh deniyordu — `refreshTokenMaxReuse` toleransı sadece bu temel sorunun belirtisini hafifletiyordu, sebebini çözmüyordu.

**Asıl çözüm: bff-server'da session bazlı refresh senkronizasyonu.** `bff-server/.../config/SessionSynchronizedAuthorizedClientManager.java`, Spring Boot'un auto-configure ettiği `DefaultOAuth2AuthorizedClientManager`'ı sarmalayıp `authorize()` çağrılarını **HTTP session ID bazında** bir `ReentrantLock` ile serileştiriyor (`SecurityConfig`'te bu wrapper, standart auto-configured bean'in yerine geçecek şekilde `@Bean` olarak tanımlandı). Aynı session'dan gelen paralel istekler artık aynı anda değil, sırayla `authorize()`'a giriyor: ilk thread refresh'i tamamladıktan sonra, sırayla lock alan diğer thread'ler delegate'e girdiğinde token zaten taze olduğu için (expiry kontrolü delegate içinde yapılıyor) ikinci bir Keycloak çağrısı yapılmadan aynı access token paylaşılmış oluyor — yani "reuse" durumu, kaynağında, hiç oluşmuyor. Bu, ekstra bir Future/Mono paylaşım mekanizması kurmaya gerek bırakmıyor; mevcut expiry-check mantığı mutual exclusion ile birleşince istenen davranışı doğal olarak veriyor.

Canlı Playwright testi bu düzeltmeyle 4 kez tekrarlandı (accessTokenLifespan 60 saniyeye indirilip, login sonrası token'ın süresinin dolmasını bekleyip Overview'un 5 paralel isteği tetiklendi): **4/4 çalıştırmada sıfır reddedilen istek, sıfır beklenmedik logout**; bff-server logunda bu süre boyunca hiç `invalid_grant`/`OAuth2AuthorizationException` görülmedi. `refreshTokenMaxReuse: 2` ayarı kaldırılmadı, ama artık birincil koruma değil — asıl garanti session bazlı serileştirmeden geliyor; bu ayar yalnızca ek bir güvenlik payı (ör. lock mekanizmasının kapsamadığı, session'sız/çok cihazlı beklenmedik senaryolar) olarak realm'de bırakıldı.

Bilinen sınır: lock'ları tutan `ConcurrentHashMap<String, Lock>` şu an süre bazlı bir eviction yapmıyor — JVM ömrü boyunca görülen her benzersiz session ID için bir giriş tutuyor. İç kullanım için (sınırlı sayıda CRM kullanıcısı, 8 saatlik session timeout) bu büyüme pratikte önemsiz kabul edildi; kullanıcı sayısı önemli ölçüde artarsa Caffeine tabanlı `expireAfterAccess` cache'e geçilebilir.

## 2. Kapsam Netleştirmesi: Keycloak Senkronizasyon Yönü ve Permission Sınırı

(b) kararının ardından iki alt karar daha netleştirildi:

- **Senkronizasyon yönü**: identity-service, Role/Permission verisinin **kaynağı** oldu. Bir kullanıcı veya rol identity-service'te oluşturulduğunda/atandığında, bu bilgi Keycloak'a (Admin REST API üzerinden) **tek yönlü** olarak yayılır. Keycloak → identity-service yönünde senkron gerekmez, çünkü login/credential akışına identity-service hiç karışmaz.
- **Permission'ların kapsamı**: Permission (ince taneli yetki), yalnızca identity-service'in **kendi** endpoint'lerini korumak için kullanılacak şekilde sınırlandı — JWT claim'i olarak taşınmayacak. Gerekçe: alternatif (tüm servislerin permission-bazlı yetkilendirme yapması) Keycloak protocol mapper'larının veya gateway'in her istekte identity-service'e senkron sorgu atmasının eklenmesini gerektirirdi; bu, mevcut order-service/payment-service kodunun hiç değişmeden Keycloak realm role'üne göre `@PreAuthorize` yapmaya devam edebildiği daha basit alternatife kıyasla orantısız bir kapsam genişlemesiydi.

## 3. Domain Katmanı: Entity, Enum, Repository

order-service/payment-service'teki kalıp birebir takip edildi: Lombok (`@Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor/@Builder`), `UUID` birincil anahtar (`GenerationType.UUID`), JPA Auditing (`@CreatedDate`/`@LastModifiedDate`) ile otomatik zaman damgası.

Kurulan varlıklar:

- **User**: sistem/personel hesaplarını ve müşteri self-servis kullanıcılarını (opsiyonel `customerId` referansıyla) temsil eder. **TCKN alanı bilinçli olarak eklenmedi** — bu bilgi zaten customer-service'te tutuluyor; tekrar (duplication) yaratmamak için identity-service kendi PII kopyasını tutmuyor.
- **Role**, **Permission**: adı/açıklaması olan basit referans varlıklar.
- **UserRole**, **RolePermission**: explicit join entity'ler (`OrderItem`'daki gibi) — `assignedAt`/`assignedBy` audit alanlarını taşıyabilmek için düz `@ManyToMany` yerine ayrı entity olarak modellendi.
- **IdentityAuditLog**: Order/PaymentAuditLog'dan farklı olarak, tek bir aggregate'e değil üç farklı entity tipine (User/Role/Permission) hizmet ettiği için generic bir şema kullanıldı: `entityType`, `entityId`, `action`, `detail`, `performedBy`.
- **OutboxEvent**, **ProcessedEvent**: order-service/payment-service ile birebir aynı yapı.

`UserStatus` enum'u (ACTIVE/INACTIVE/SUSPENDED) eklendi. Repository katmanı YAGNI ilkesiyle yalnızca kesin ihtiyaç duyulan sorgu metodlarını içerecek şekilde sınırlandı.

**Zaman damgası tipi tutarlılığı**: order-service/customer-service/notification-service gibi çoğunluk `LocalDateTime` kullanırken payment-service `Instant` kullanıyor (tek istisna). identity-service, baskın konvansiyona uyarak entity'lerinde `LocalDateTime`, ama tüm servislerde ortak olan `OutboxEvent`/`ProcessedEvent` için `Instant` kullandı.

## 4. Flyway Migration ve Seed Rol Verisi

`V1__init_identity_tables.sql` ile 8 tablo (users, roles, permissions, user_roles, role_permissions, identity_audit_logs, outbox, processed_events) oluşturuldu — `ddl-auto: update` değil, versiyonlanmış migration. `user_roles` ve `role_permissions` üzerinde `UNIQUE(user_id, role_id)` / `UNIQUE(role_id, permission_id)` kısıtları eklendi (aynı atamanın iki kez yapılmasını veritabanı seviyesinde engellemek için).

Aktör tablosundaki 7 rol seed data olarak eklendi: `CUSTOMER`, `CALL_CENTER_AGENT`, `FIELD_DEALER`, `MARKETING_MANAGER`, `SYSTEM_ADMIN`, `BILLING_OPERATOR`, `SYSTEM_SERVICE`. Migration hatasız uygulandığı ve 7 rolün tabloya eklendiği canlı olarak doğrulandı.

Bu adımda ayrıca identity-service'in daha önce boş bir iskelet olduğu fark edildi (`pom.xml`/`application.yml` vardı ama Java kaynak kodu yoktu) — `spring-boot-starter-data-jpa`, `postgresql`, `spring-boot-starter-flyway` gibi eksik bağımlılıklar eklendi; `configs/identity-service/application-dev.yml`'e datasource/JPA/Flyway ayarları yazıldı.

## 5. Exception Katmanı

order-service/payment-service'teki `BaseException` (abstract, `RuntimeException`, `HttpStatus` + `errorCode` taşıyan) ve `GlobalExceptionHandler` (`@RestControllerAdvice`, RFC 7807 `ProblemDetail` formatı) birebir taşındı — `errorCode.toLowerCase(Locale.ROOT)` kullanılarak, product-catalog-service'te daha önce karşılaşılan Türkçe locale bug'ından (`"I".toLowerCase()` → `"ı"` üretmesi, `Locale.ROOT` verilmezse) kaçınıldı.

7 özel exception eklendi: `UserNotFoundException` (404), `DuplicateUsernameException`/`DuplicateEmailException`/`RoleAlreadyAssignedException` (409), `RoleNotFoundException`/`PermissionNotFoundException` (404), `KeycloakSyncException` (503 — Keycloak Admin API çağrısı başarısız olduğunda kullanılacak).

`GlobalExceptionHandler`'ın referans alınan haliyle birebir aynı olması için, kullanıcının orijinal talep listesinde olmayan ama order-service/payment-service'te mevcut olan `ObjectOptimisticLockingFailureException` ve `IllegalArgumentException` handler'ları da eklendi.

## 6. DTO / Mapper / Rules / Service / Controller Katmanı

**DTO**: `dto/request` (CreateUserRequest, UpdateUserRequest, AssignRoleRequest, CreateRoleRequest, CreatePermissionRequest, AssignPermissionRequest) ve `dto/response` (UserResponse, RoleResponse, PermissionResponse) — record + jakarta.validation, order-service stiliyle.

**Mapper**: `User`/`Role` entity'lerinde roller/permission'lar için geri-ilişki (`@OneToMany`) olmadığı için (bilinçli tasarım — domain katmanında gereksiz bidirectional ilişki eklenmedi), MapStruct mapper'ları `toResponse(entity, List<String>)` iki parametreli imza kullanıyor; servis katmanı rol/permission isimlerini repository'den ayrıca çekip mapper'a veriyor.

**Rules**: `UserStateRules`, tek bir kural içeriyor — sadece `ACTIVE` durumundaki bir kullanıcıya rol atanabilir. Aşırı karmaşıklaştırılmadı; `UpdateUserRequest`'te durum değişikliği endpoint'i olmadığı için (bu, kapsam dışı bırakıldı) başka bir geçiş kuralına ihtiyaç yoktu.

**Service/Controller**: `UserService`, `RoleService`, `PermissionService` (+ Impl) ve karşılık gelen ince controller'lar oluşturuldu. Her serviste `@Transactional`, audit log çağrıları `OrderAuditService` pattern'iyle (`performedBy`: `SecurityContextHolder`'dan JWT `preferred_username`, yoksa `"SYSTEM"`).

### User Oluşturmanın Keycloak'a Bağımlı Olmaması ve `keycloakUserId`'nin Async Doldurulması

Bu, tasarımın en kritik kararlarından biri: **User oluşturma işlemi Keycloak'ın erişilebilir olmasına bağımlı değil.** `UserServiceImpl.createUser` akışı şöyle işliyor:

1. `User` entity'si DB'ye kaydedilir (`keycloakUserId` başlangıçta `null`).
2. Keycloak senkronizasyonu **denenir** — başarılı olursa `keycloakUserId` doldurulup tekrar kaydedilir.
3. Başarısız olursa `KeycloakSyncException` yakalanır, **sadece loglanır** — kullanıcı DB'de kalıcı olarak var olmaya devam eder, işlem geri alınmaz (rollback yok).

Gerekçe: Keycloak, identity-service için **dışsal bir bağımlılık**. Eğer User oluşturma işlemi Keycloak'ın o an ayakta/erişilebilir olmasına bağımlı olsaydı, Keycloak'taki geçici bir kesinti (ağ sorunu, yeniden başlatma, yanlış yapılandırılmış bir service-account client) identity-service'in **temel CRUD işlevini** felç ederdi — oysa identity-service'in birincil sorumluluğu kendi veritabanındaki User/Role/Permission verisinin doğruluğu, Keycloak senkronizasyonu ise ikincil, "best-effort" bir eş zamanlama. Bu yüzden `keycloakUserId` alanı entity'de nullable bırakıldı ve **asenkron/best-effort olarak** doldurulacak şekilde tasarlandı — "asenkron" burada bir mesaj kuyruğu anlamında değil, "User'ın varlığından bağımsız, sonradan da tamamlanabilir bir alan" anlamında kullanılıyor. Aynı best-effort prensip rol atamasında da uygulandı: `keycloakUserId` henüz `null`'sa (yani kullanıcı hiç senkronize olmadıysa), realm role senkronizasyonu hiç denenmeden atlanıp loglanıyor.

Bu kararın pratikte doğruluğu, geliştirme sürecinde bizzat doğrulandı: identity-service'in kendi Keycloak client'ı henüz kurulmadığı bir aşamada `POST /api/v1/users` çağrıldığında, Keycloak senkronizasyonu `401 invalid_client` ile başarısız oldu, ama kullanıcı DB'de sorunsuz oluştu ve `keycloakUserId: null` olarak API'den döndü — sistem tam olarak tasarlandığı gibi davrandı.

### Rol/Kullanıcı/Permission Yönetiminin `SYSTEM_ADMIN` Yetkisiyle Sınırlandırılması

identity-service, sistem genelindeki User/Role/Permission verisinin tek kaynağı olduğu için buradaki yazma işlemleri diğer servislerdeki sıradan bir CRUD endpoint'inden daha geniş bir etki alanına sahip: bir role yeni bir permission eklemek ya da bir kullanıcıya yeni bir rol atamak, dolaylı olarak sistemdeki her servisin yetkilendirme kararını etkileyebilir. Bölüm 4'te seed edilen 7 realm role'ünden biri olan `SYSTEM_ADMIN`, tam olarak bu sorumluluk için tanımlandı — Keycloak realm'indeki açıklaması "Sistem genelinde kullanıcı, rol ve yetki yönetimini yürütür" — yani identity-service'in state değiştiren endpoint'lerini koruyacak yetki, genel bir `admin` etiketi değil, seed data'daki bu spesifik "Sistem Yöneticisi" rolü olacak şekilde baştan tasarlandı. Bu yüzden state değiştiren tüm endpoint'ler — `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `POST /api/v1/users/{id}/roles`, `POST /api/v1/roles`, `POST /api/v1/roles/{name}/permissions`, `POST /api/v1/permissions` — controller katmanında `@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")` ile korunuyor. Bu, order-service/product-catalog-service/ticket-service'teki sistem-yönetimi endpoint'leriyle (KYC onayı, tarife yayınlama, ticket atama) aynı iki katmanlı desen: `SecurityConfig`'teki `.anyRequest().authenticated()` taban kuralı yalnızca "geçerli bir JWT var mı" sorusuna cevap veriyor, method-level `@PreAuthorize` ise "bu JWT'nin sahibi bu spesifik işlemi yapabilir mi" sorusuna — sistem yönetimi işlevlerine yalnızca `SYSTEM_ADMIN` yetkisine sahip kullanıcıların erişebilmesi ilkesi, path bazlı bir kural yerine bilinçli olarak controller/method seviyesinde uygulanıyor, çünkü aynı `/api/v1/users` altında hem `SYSTEM_ADMIN`'e özel yazma işlemleri hem de daha geniş bir kitleye açık okuma işlemleri bir arada yaşıyor.

Buna karşılık salt-okunur listeleme endpoint'leri (`GET /api/v1/users`, `GET /api/v1/users/{id}`, `GET /api/v1/roles`, `GET /api/v1/permissions`) yalnızca `authenticated()` seviyesinde bırakıldı — herhangi bir authenticated kullanıcının, örneğin bir rol/permission seçiciyi doldururken hangi rollerin ve permission'ların var olduğunu görebilmesi gerekiyor; bu bilgi tek başına bir yetki yükseltme riski taşımadığı için `SYSTEM_ADMIN` kısıtına gerek duyulmadı.

`GlobalExceptionHandler`'daki `AuthorizationDeniedException` handler'ı (bkz. bölüm 5), `@PreAuthorize` reddettiğinde 403 + RFC 7807 `ProblemDetail` gövdesi döndürülmesini zaten karşılıyor — bu iki mekanizma (yetkilendirme kuralı ve onun hata gövdesi) tasarım aşamasından itibaren birlikte düşünüldü.

## 7. Keycloak Admin API Entegrasyonu

`KeycloakAdminClient` (Feign) eklendi — Keycloak Admin REST API'sine (`/admin/realms/{realm}`) konuşan bir interface: `createUser`, `getRealmRole`, `assignRealmRole`.

Bu client'ın kimlik doğrulaması, order-service'teki `FeignJwtInterceptor`'dan **bilinçli olarak farklı** tasarlandı: `FeignJwtInterceptor`, gelen kullanıcı JWT'sini downstream servise (örn. customer-service) olduğu gibi forward ediyor (pass-through authorization) — bu, normal mikroservisler-arası çağrılar için doğru. Ama Keycloak Admin API, `realm-management` client'ının `manage-users`/`view-users` gibi **yönetimsel** rollerini gerektiriyor; sıradan bir son-kullanıcı JWT'sinde bu roller asla bulunmaz. Bu yüzden `KeycloakAdminClientConfig` — bilinçli olarak `@Configuration` **değil**, sadece `KeycloakAdminClient`'a `configuration =` ile bağlı bir Feign-specific config sınıfı — her zaman ayrı bir admin service-account token'ı (`client_credentials`, `keycloak-admin` registration'ı) kullanan bir `RequestInterceptor` sağlıyor; gelen kullanıcı JWT'sini hiç forward etmiyor.

`KeycloakSyncService`, bu Feign çağrılarını Resilience4j `@CircuitBreaker` ile sarıyor (CustomerClient'taki fallback-method deseniyle aynı) ve başarısızlıkta `KeycloakSyncException` fırlatıyor. Bilinçli olarak **outbox/event-driven bir consumer değil**, `UserService`'ten senkron çağrılan basit bir yardımcı servis olarak tasarlandı — retry mekanizması eklenmedi; bu, sonraki bir adımın konusu olarak bırakıldı.

## 8. Keycloak Realm'inde Service-Account Client'ının Kurulması

`KeycloakAdminClient`'ın gerçekten çalışabilmesi için Keycloak realm'inde bir `identity-service` client'ı ve bu client'ın service-account'una `realm-management` client'ının rollerinin atanması gerekiyordu. `keyclock/realm-export.json`'a:

- `identity-service` client'ı eklendi (order-service/payment-service ile aynı yapıda: `serviceAccountsEnabled: true`, `publicClient: false`, `standardFlowEnabled: false`, `clientAuthenticatorType: client-secret`, secret `identity-service-secret` — `application-dev.yml`'deki registration ile eşleşecek şekilde).
- Bu client'ın service-account'una (`service-account-identity-service` pseudo-user'ı) `realm-management` client role'leri atandı.

**Canlı doğrulama sırasında iki ek eksik ortaya çıktı ve düzeltildi** (kullanıcının orijinal talebinin ötesine geçildi, çünkü özellik bunlar olmadan gerçekten çalışmıyordu):

1. **7 rol Keycloak'ta realm role olarak hiç tanımlı değildi** — yalnızca identity-service'in kendi Postgres'indeki `roles` tablosunda vardı. Bir realm role'ü Keycloak'ta fiilen var olmadan bir kullanıcıya atanamaz; bu yüzden aynı 7 rol (`CUSTOMER`, `CALL_CENTER_AGENT`, ...) aynı isim/açıklamalarla Keycloak realm role'ü olarak da eklendi.
2. **`manage-users`/`view-users` bir realm role'ün tanımını okumaya yetmiyordu** — bu roller yalnızca kullanıcı yönetimi kapsamını kapsıyor; realm-level role tanımlarını sorgulamak (`GET /roles/{name}`) `view-realm` yetkisini gerektiriyor. Bu, `403 Forbidden` hatasıyla canlı testte tespit edildi ve `view-realm` de service-account'a eklendi.

Ayrıca Keycloak container'ının `start-dev` modunda gömülü (embedded) bir veritabanı kullandığı ve `docker compose restart` ile realm'in **yeniden import edilmediği** ("bayat realm verisi" sorunu) fark edildi — container'ın kendisinin (yalnızca restart değil) `--force-recreate` ile yeniden oluşturulması gerektiği belgelendi.

Bu düzeltmelerin ardından tam senkronizasyon canlı olarak doğrulandı: `POST /api/v1/users` sonrası `keycloakUserId` gerçek bir Keycloak kullanıcı ID'siyle doldu; `POST /api/v1/users/{id}/roles` sonrası Keycloak Admin API (`GET /admin/realms/telcocrm-gygy5/users/{id}/role-mappings/realm`) atanan realm role'ü gerçekten döndürdü.

## 9. Gözlemlenebilirlik (Observability)

order-service ile birebir aynı observability stack'i eklendi: OpenTelemetry tracing (Zipkin exporter), Micrometer Prometheus registry, ECS formatlı structured logging (Logback). `docker-compose.yml`'deki promtail mount'una `identity-service/logs` dizini, `promtail-config.yaml`'a karşılık gelen `job` eklendi.

Canlı doğrulama: bir isteğin Zipkin'de `identity-service` servisi altında trace olarak göründüğü, `/actuator/prometheus`'un 200 döndüğü, log dosyasının ECS JSON formatında olduğu ve Loki'den `{job="identity-service"}` sorgusuyla gerçek zamanlı erişilebildiği doğrulandı.

## 10. Debezium CDC

`docker/connectors/identity-outbox-connector.json` eklendi — order-outbox-connector.json ile aynı yapıda (`EventRouter` transform, `outbox` tablosunu izleyen Postgres connector), `database.dbname: identity`, `topic.prefix: identity`. `identity-db` zaten `postgres-common` YAML anchor'ını (`wal_level=logical`) kullandığı için ek bir docker-compose değişikliği gerekmedi. `register-connectors.sh` zaten generic olduğu için (connectors/ klasörünü tarıyor) yeni bir kod değişikliği gerektirmedi — `debezium-init` container'ı yeniden çalıştırılarak connector kaydedildi.

Canlı doğrulama: bir `User` oluşturulduğunda outbox tablosuna `UserCreatedEvent` yazıldığı, Debezium'un bunu `user-created-topic`'e gerçek zamanlı ilettiği bir Kafka consumer ile doğrulandı.

## 11. Testler

order-service/payment-service'teki JUnit 5 + Mockito kalıbı birebir takip edilerek 114 unit test yazıldı: entity davranışları, mapper'lar (MapStruct-üretilen implementasyonlar), exception sınıfları, `GlobalExceptionHandler`, `rules` (UserStateRules), event record'ları (`of()` factory metodları), `OutboxService`, `IdentityAuditService`, `KeycloakSyncService` (happy path + circuit-breaker fallback davranışı — `Location` header eksikse/Keycloak çağrısı başarısızsa `KeycloakSyncException` fırlatıldığının doğrulanması dahil), servis katmanı (`UserServiceImpl`, `RoleServiceImpl`, `PermissionServiceImpl`) ve controller'lar (MockMvc standalone setup). `UserController`'ın `SYSTEM_ADMIN` yetki kuralı ayrıca, standalone setup'ın method-security AOP'sini devreye almaması nedeniyle, tam Spring context'i ve gerçek bir `Jwt` authority'siyle çalışan ayrı bir `@SpringBootTest`/`@AutoConfigureMockMvc` testiyle (`UserRoleAuthorizationTest`) doğrulanıyor — `SYSTEM_ADMIN` olmayan bir authority ile 403, `SYSTEM_ADMIN` ile 200 bekleniyor. Testcontainers kullanılmadı; `@SpringBootTest` context-load testi H2 in-memory veritabanı ile çalışıyor (Flyway devre dışı, `ddl-auto: create-drop`).

`KeycloakSyncService`'in fallback metodları (`syncUserCreationFallback`, `syncRoleAssignmentFallback`), CustomerClient'ın public default fallback metodlarından farklı olarak private değil **package-private** yapıldı — aynı pakette yaşayan test sınıfının bunları doğrudan çağırabilmesi için (Resilience4j'nin `@CircuitBreaker` AOP mekanizması zaten erişim seviyesinden bağımsız çalışıyor, bu değişiklik yalnızca testability için).

## 12. JaCoCo ve SonarQube

`jacoco-maven-plugin` (`prepare-agent` + `report`, order-service/payment-service ile aynı config) eklendi. `mvn test` çalıştırıldığında `target/site/jacoco/jacoco.xml` üretiliyor — kök `pom.xml`'in `sonar.coverage.jacoco.xmlReportPaths` ayarı bu dosyayı otomatik okuyor.

`mvn sonar:sonar` kök dizinden çalıştırılarak identity-service'in tek bir Sonar projesi (`telco-crm`) altında bir bileşen olarak göründüğü doğrulandı: %95 coverage, 0 bug, 16 code smell (bu son ikisi bu doküman kapsamında ayrıca ele alınmadı).

## 13. Bilinen Eksikler ve Gelecekte Yapılacaklar

- **Keycloak senkronizasyonunda retry yok**: `KeycloakSyncService` başarısız olduğunda sadece loglanıyor; `keycloakUserId` `null` kalan bir kullanıcı için otomatik bir "sonradan tekrar dene" mekanizması yok. Bugün bunu düzeltmenin tek yolu manuel müdahale (veya kullanıcıyı silip tekrar oluşturmak).
- **Şifre/credential provisioning yok**: `CreateUserRequest`'te şifre alanı yok; Keycloak'ta oluşturulan kullanıcının ilk şifresi/credential'ı ayrıca ele alınmadı.
- **`UserStateRules`'ta durum geçiş endpoint'i yok**: Kullanıcıyı `INACTIVE`/`SUSPENDED` yapan bir API endpoint'i henüz eklenmedi; yalnızca "role atamak için ACTIVE olmalı" kuralı var.
- **`code_smells: 16`**: Sonar analizinde görülen code smell'ler bu geliştirme turunda ele alınmadı.

---

## 14. Teknoloji Stack'i

| Katman | Teknoloji | Sürüm |
|---|---|---|
| Dil | Java | 21 |
| Framework | Spring Boot | 4.0.6 |
| Bağımlılık yönetimi | Spring Cloud BOM | 2025.1.1 (çözümlenen artifact sürümü: 5.0.1) |
| Web | Spring Web MVC (`spring-boot-starter-web`) | 4.0.6 |
| Validasyon | Spring Boot Starter Validation / Hibernate Validator | 4.0.6 |
| Veri erişimi (ORM) | Spring Data JPA / Hibernate ORM | 4.0.5 / 7.2.12.Final |
| Veritabanı | PostgreSQL (JDBC sürücüsü) | 42.7.10 |
| Şema versiyonlama | Flyway (`flyway-database-postgresql`) | 11.14.1 |
| Servis keşfi | Netflix Eureka Client (Spring Cloud) | 5.0.1 |
| Merkezi konfigürasyon | Spring Cloud Config Client | 5.0.1 |
| Servisler arası HTTP çağrısı | OpenFeign (`feign-core`) | 13.6 |
| Devre kesici / dayanıklılık | Resilience4j (`resilience4j-spring-boot3`) | 2.3.0 |
| AOP desteği (Resilience4j annotation'ları için) | Spring Boot Starter AspectJ | 4.0.6 |
| OAuth2 client (Keycloak admin token için) | Spring Boot Starter OAuth2 Client | 4.0.6 |
| Kimlik doğrulama | Spring Security OAuth2 Resource Server (JWT) | 4.0.6 |
| Kimlik sağlayıcı | Keycloak | (docker-compose imajı: `quay.io/keycloak/keycloak:26.1`) |
| Nesne dönüşümü (entity ↔ DTO) | MapStruct | 1.6.3 |
| Kod üretimi (boilerplate azaltma) | Lombok | 1.18.46 |
| JSON serileştirme | Jackson (`jackson-databind`) | 2.21.2 |
| Dağıtık izleme (tracing) | Micrometer Tracing (OpenTelemetry bridge) | 1.6.5 |
| Trace export | OpenTelemetry Zipkin Exporter | 1.55.0 |
| Trace toplama | Zipkin | (docker-compose imajı: `openzipkin/zipkin:latest`) |
| Metrikler | Micrometer Prometheus Registry | 1.16.5 |
| İzleme/health endpoint'leri | Spring Boot Actuator | 4.0.6 |
| Yapılandırılmış loglama | Logback + ECS formatı (Spring Boot structured logging) | 4.0.6 |
| Log toplama/görselleştirme | Loki + Promtail + Grafana | (docker-compose imajları: Loki 3.2.0, Promtail 3.2.0, Grafana 11.3.0) |
| CDC (Change Data Capture) | Debezium Connect (Postgres connector) | 3.0 |
| Test | Spring Boot Starter Test (JUnit 5, Mockito, AssertJ) + H2 | 4.0.6 (parent'tan miras) |
| Kod kalitesi analizi | SonarQube Scanner Maven Plugin | 4.0.0.4121 |
| Test kapsamı (coverage) aracı | JaCoCo | 0.8.12 |
| Konteynerleştirme / yerel altyapı | Docker Compose | — |

## 15. Paket Yapısı

`com.telcocrm.identityservice` kök paketi altında:

- **(kök paket)** — `IdentityServiceApplication`: giriş noktası; `@EnableFeignClients` ve `@EnableJpaAuditing` burada etkinleştiriliyor.
- **client** — `KeycloakAdminClient`: Keycloak Admin REST API'sine konuşan Feign arayüzü.
- **client.dto** — `KeycloakUserRepresentation`, `KeycloakRoleRepresentation`: Keycloak Admin API'sinin beklediği/döndürdüğü JSON şemalarının yerel temsilleri.
- **config** — `SecurityConfig` (JWT tabanlı kimlik doğrulama + `KeycloakRoleConverter` ile realm role'lerinin `GrantedAuthority`'e çevrilmesi), `OAuth2ClientConfig` (`OAuth2AuthorizedClientManager` bean'i), `KeycloakAdminClientConfig` (yalnızca `KeycloakAdminClient`'a özel, admin token alan `RequestInterceptor` — bilinçli olarak `@Configuration` değil).
- **controller** — `UserController`, `RoleController`, `PermissionController`: ince, sadece servise delege eden REST endpoint'leri.
- **dto.request** / **dto.response** — istek/cevap şekilleri.
- **entity** — `User`, `Role`, `Permission`, `UserRole`, `RolePermission`, `IdentityAuditLog`, `OutboxEvent`, `ProcessedEvent`.
- **entity.enums** — `UserStatus`.
- **event.publish** — `UserCreatedEvent`, `RoleAssignedEvent`: outbox üzerinden Kafka'ya yayınlanan olaylar.
- **exception** — `BaseException`, özel exception'lar, `GlobalExceptionHandler`.
- **mapper** — `UserMapper`, `RoleMapper`, `PermissionMapper`.
- **repository** — Spring Data JPA repository arayüzleri.
- **rules** — `UserStateRules`.
- **service** / **service.impl** — `UserService`/`RoleService`/`PermissionService` (+ Impl), `KeycloakSyncService` (Keycloak senkronizasyon yardımcı servisi), `OutboxService`, `IdentityAuditService`.

## 16. API Referansı

Tüm endpoint'ler `/api/v1/...` altında toplanmıştır ve (actuator/swagger hariç) geçerli bir JWT gerektirir. State değiştiren endpoint'ler ayrıca `SYSTEM_ADMIN` yetkisi ister (bkz. bölüm 6, "Rol/Kullanıcı/Permission Yönetiminin `SYSTEM_ADMIN` Yetkisiyle Sınırlandırılması"); salt-okunur endpoint'ler için `authenticated()` yeterlidir. Hata cevapları RFC 7807 `ProblemDetail` formatındadır.

### Kullanıcılar

| Method | Path | Yetki | Açıklama |
|---|---|---|---|
| POST | `/api/v1/users` | `SYSTEM_ADMIN` | Kullanıcı oluşturur (Keycloak senkronizasyonu best-effort, başarısızsa `keycloakUserId` null kalır) |
| GET | `/api/v1/users/{id}` | authenticated | Tekil kullanıcı getirir (rolleriyle birlikte) |
| GET | `/api/v1/users` | authenticated | Kullanıcıları sayfalı listeler |
| PUT | `/api/v1/users/{id}` | `SYSTEM_ADMIN` | Kısmi güncelleme (email/fullName/phoneNumber) |
| POST | `/api/v1/users/{id}/roles` | `SYSTEM_ADMIN` | Kullanıcıya rol atar (`AssignRoleRequest`) — yalnızca `ACTIVE` kullanıcılara |

### Roller

| Method | Path | Yetki | Açıklama |
|---|---|---|---|
| POST | `/api/v1/roles` | `SYSTEM_ADMIN` | Rol oluşturur |
| GET | `/api/v1/roles` | authenticated | Tüm rolleri listeler |
| POST | `/api/v1/roles/{name}/permissions` | `SYSTEM_ADMIN` | Role permission atar (`AssignPermissionRequest`) |

### Permission'lar

| Method | Path | Yetki | Açıklama |
|---|---|---|---|
| POST | `/api/v1/permissions` | `SYSTEM_ADMIN` | Permission oluşturur |
| GET | `/api/v1/permissions` | authenticated | Tüm permission'ları listeler |

**Olası durum kodları (genel):** 201/200 başarı, 400 validasyon hatası, 403 (`ACCESS_DENIED` — `SYSTEM_ADMIN` yetkisi olmadan bir yazma endpoint'ine erişildiğinde), 404 (`USER_NOT_FOUND`/`ROLE_NOT_FOUND`/`PERMISSION_NOT_FOUND`), 409 (`DUPLICATE_USERNAME`/`DUPLICATE_EMAIL`/`ROLE_ALREADY_ASSIGNED`/rol veya permission zaten var), 503 (`KEYCLOAK_SYNC_FAILED` — yalnızca senkron çağrılan iç akışlarda; kullanıcı-facing endpoint'ler Keycloak hatasında asla başarısız olmaz, bkz. bölüm 6).

## 17. Kafka Topics

identity-service, olayları veritabanına yazıp Debezium CDC ile Kafka'ya yayınlar (outbox pattern, bkz. bölüm 10); henüz bir Kafka event tüketicisi yok.

| Topic | Event | Ne zaman yayınlanır | Alanlar |
|---|---|---|---|
| `user-created-topic` | `UserCreatedEvent` | Kullanıcı başarıyla oluşturulduğunda | `eventId`, `occurredAt`, `userId`, `username`, `email`, `fullName`, `customerId` |
| `role-assigned-topic` | `RoleAssignedEvent` | Bir kullanıcıya rol atandığında | `eventId`, `occurredAt`, `userId`, `username`, `roleName` |

## 18. Ortam Kurulumu

1. **Altyapı konteynerlerini başlat**: repo kökünde `docker/docker-compose.yml` ile `docker compose up -d` — `identity-db` (Postgres, port 5401), Keycloak (port 8085, realm `keyclock/realm-export.json`'dan import edilir), Kafka, Debezium Connect, Zipkin, Loki/Promtail/Grafana dahil.
2. **discovery-server**'ı başlat (Eureka, port 8761).
3. **config-server**'ı başlat (port 8888, `native` profil) — identity-service'in başlaması için zorunlu.
4. **identity-service**'i başlat (`dev` profili, port 9001 — dikkat: bu port SonarQube ile çakışabilir, bkz. not).
5. **Debezium connector'ını kaydet**: `docker/register-connectors.sh` çalıştırılır (atlanırsa `outbox` event'leri Kafka'ya taşınmaz).

**Not — port çakışması**: Bu makinede SonarQube docker container'ı başlangıçta 9001'i kullanıyordu (identity-service'in dokümanda öngörülen portu); bu, `docker/docker-compose.sonarqube.yml`'de SonarQube'ün host portu 9500'e taşınarak çözüldü.

**Not — Keycloak realm reimport**: `identity-service` client'ı ve realm role'leri `keyclock/realm-export.json`'a eklendiğinde, Keycloak container'ının **sadece restart edilmesi yetmez** — `start-dev` modunda gömülü DB container'ın kendi dosya sisteminde yaşadığı için realm otomatik yeniden import edilmez. `docker compose rm -sf keycloak && docker compose up -d --force-recreate keycloak` gerekir.

## 19. Test ve Kod Kalitesi

**Testleri çalıştırma:**
```
cd identity-service
mvn test
```
`IdentityServiceApplicationTests` (`@SpringBootTest`) H2 in-memory veritabanı kullanır (`application-test.yml`, Flyway devre dışı, `ddl-auto: create-drop`) — bu nedenle gerçek bir Postgres/config-server bağlantısı gerekmez, testler izole çalışır.

Mevcut test kapsamı: entity davranışları, MapStruct mapper'ları, `UserStateRules`, event record'ları, servis katmanı (`UserServiceImpl`, `RoleServiceImpl`, `PermissionServiceImpl`, `KeycloakSyncService` fallback davranışı dahil), `OutboxService`, `IdentityAuditService`, exception sınıfları + `GlobalExceptionHandler`, controller'lar (`UserControllerTest`, `RoleControllerTest`, `PermissionControllerTest`) ve `SYSTEM_ADMIN` yetki kuralı (`UserRoleAuthorizationTest`). Toplam 114 test, 0 başarısızlık.

**JaCoCo coverage raporu:** `mvn test` sonrası `target/site/jacoco/index.html`'de görüntülenebilir (%93 instruction coverage).

**SonarQube analizi:**
```
mvn sonar:sonar
```
(kök dizinden çalıştırılır; kimlik doğrulama gerekiyorsa `-Dsonar.token=<token>` eklenir). Kök `pom.xml`'de `sonar.host.url=http://localhost:9500` olarak ayarlı; tüm modüller tek bir Sonar projesi (`telco-crm`) altında toplanıyor, identity-service bu proje içinde bir bileşen (`telco-crm:identity-service`) olarak görünür.
