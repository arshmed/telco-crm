# payment-service Geliştirme Süreci

Bu doküman, `payment-service`'in baştan bugüne kadar hangi adımlarla, hangi gerekçelerle geliştirildiğini anlatır. Kod içermez; her adımda ne yapıldığı ve **neden** o şekilde yapıldığı anlatılır. Format ve anlatım tarzı `order-service` için yazılan `ORDER_SERVICE_DEVELOPMENT.md` ile aynıdır.

---

## 1. Servisin Amacı ve Yeri

`payment-service`, telco-crm mikroservis mimarisinde ödeme yaşam döngüsünü yöneten servistir: bir sipariş ödeme bekler duruma geldiğinde (order-service'te `PENDING_PAYMENT`), bu ödemeyi bir PSP (Payment Service Provider) üzerinden tahsil eder, sonucu (başarılı/başarısız) diğer servislere duyurur, başarısız ödemeleri zamanlanmış bir görevle tekrar dener ve gerektiğinde iade işler. Sistemde database-per-service prensibi uygulanıyor; `payment-service` kendi Postgres veritabanına (`payment-db`, host portu 5408) sahip, sipariş veya müşteri bilgisini kendi tablolarında tutmuyor — ihtiyaç anında `order-service`'ten senkron olarak (Feign ile `OrderClient`) ya da Kafka event'leri ile alıyor.

`payment-service`'in dış dünyayla iki farklı temas noktası var:
- **order-service ile senkron**: `OrderClient` üzerinden bir siparişin gerçek tutarını/para birimini/müşterisini ve mevcut durumunu sorgular (client'a güvenmez, source of truth her zaman order-service'tir).
- **Kafka üzerinden asenkron**: order-service'in yayınladığı `OrderCreatedEvent`'i tüketip otomatik ödeme başlatabilir (bkz. bölüm 12), kendi ürettiği `PaymentCompletedEvent`/`PaymentFailedEvent`/`PaymentRefundedEvent`'leri yayınlar (order-service bunları tüketip saga'yı ilerletir), ve subscription-service'in (henüz yazılmamış) yayınlayacağı varsayılan `SubscriptionActivationFailedEvent`'i tüketip otomatik iade tetikler (bkz. bölüm 17).

## 2. Temel Altyapı Kurulumu

Servisin iskeleti order-service ile aynı kalıpta kuruldu: Spring Boot projesi, Maven bağımlılıkları (Spring Web, Spring Data JPA, Spring Security OAuth2 Resource Server + Client, Spring Cloud OpenFeign, Spring Cloud Stream/Kafka, Resilience4j, Flyway, Lombok, MapStruct) eklendi. Servis, diğer servislerle birlikte ortak `docker/docker-compose.yml` üzerinden ayağa kalkan altyapıya (Postgres, Kafka, Keycloak, Zipkin, Loki/Grafana, Debezium Connect) bağlanıyor; kendi Postgres instance'ı `payment-db` (host portu 5408, veritabanı adı `payment`).

Konfigürasyon dosyaları order-service'te olduğu gibi kod deposundan ayrı bir `configs/payment-service` klasöründe tutuluyor (`application.yml` profil seçimini yapıyor, `application-dev.yml` gerçek bağlantı bilgilerini içeriyor). Servis `dev` profiliyle çalışıyor ve `spring.config.import` ile config-server'a (port 8888) bağımlı — config-server ayakta değilse `payment-service` başlamıyor.

## 3. Entity, Enum ve Repository Katmanı

Ödeme domain modeli şu ana yapı taşlarıyla kuruldu:

- **Payment**: bir ödemenin kendisi — sipariş ID'si, müşteri ID'si, tutar, para birimi, yöntem (`PaymentMethod`), durum (`PaymentStatus`), PSP'den dönen referans (`externalRef`), varsa başarısızlık sebebi, ödenme zamanı, yeniden deneme sayacı (`retryCount`) ve bir sonraki deneme zamanı (`nextRetryAt`). `orderId` kolonu veritabanı seviyesinde **unique** — bu, "bir siparişin en fazla bir ödemesi olabilir" kuralını uygulama koduna güvenmeden şema seviyesinde de garanti altına alıyor (bkz. bölüm 14'teki mükerrer ödeme koruması ile ilişkisi).
- **PaymentAttempt**: bir ödeme için yapılan her PSP çağrısının kaydı — deneme numarası, PSP'den dönen ham cevap, deneme zamanı. `Payment` ile `@ManyToOne`/`@OneToMany` ilişkili (order-service'teki `Order`↔`OrderItem` ilişkisiyle aynı desende); bir ödeme birden fazla deneme geçirebildiği için (ilk deneme + retry'ler) bu geçmişin ayrı satırlar halinde saklanması gerekiyor.
- **OutboxEvent**: outbox pattern için, henüz Kafka'ya yayınlanmamış olayların geçici olarak tutulduğu tablo (order-service'teki `OutboxEvent` ile birebir aynı yapı).
- **ProcessedEvent**: inbox pattern için, daha önce işlenmiş gelen event ID'lerinin tutulduğu tablo.
- **PaymentAuditLog**: bir ödemenin geçmişteki her durum değişikliğini (`paymentId`, o anki `paymentStatus`, kısa açıklama, kimin/neyin tetiklediği, zaman damgası) kalıcı olarak saklayan, immutable (yalnızca ekleme yapılan) bir denetim kaydı — order-service'teki `OrderAuditLog` ile aynı desen (bkz. bölüm 18).

Enum'lar: `PaymentStatus` (`PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`), `PaymentMethod` (`CREDIT_CARD`, `BANK_TRANSFER`, `WALLET`).

`Payment` entity'sine `@Version` ile **optimistic locking** baştan eklendi — bir ödeme aynı anda hem kullanıcının manuel isteğiyle hem arka planda çalışan retry scheduler'ıyla güncellenmeye çalışılabileceği için (bkz. bölüm 14 ve 15), eşzamanlı iki güncellemeden birinin sessizce kaybolmasını (lost update) önlemek amacıyla bu koruma en başından tasarıma dahil edildi — order-service'te bu koruma sonradan bir "sertleştirme turu"nda eklenmişti, payment-service'te ise ilk günden itibaren var.

`createdAt`/`updatedAt` alanları Spring Data JPA Auditing (`@CreatedDate`/`@LastModifiedDate`, `@EntityListeners(AuditingEntityListener.class)`) ile otomatik dolduruluyor.

Repository katmanı standart Spring Data JPA arayüzleriyle kuruldu; `PaymentRepository`'de iş mantığının ihtiyaç duyduğu üç özel sorgu var: `findByOrderId` (bir siparişin ödemesini bulmak — hem tekrarlanan ödeme kontrolünde hem otomatik/manuel akış çakışması guard'ında kullanılıyor, bkz. bölüm 14), `findByPaymentRequestId` (idempotency replay için, bkz. bölüm 16) ve `findByStatusAndNextRetryAtBefore` (retry scheduler'ın işlenmeyi bekleyen ödemeleri bulması için, bkz. bölüm 15).

## 4. DTO, Mapper ve Servis Katmanı

İstemciye entity'lerin doğrudan sızmaması için ayrı request/response DTO'ları tanımlandı: `CreatePaymentRequest` (ödeme oluşturma — kart bilgileri dahil, bkz. bölüm 7), `RefundRequest` (iade sebebi), `PaymentResponse` (ödeme detayı, `PaymentAttemptResponse` listesi dahil), `PaymentAttemptResponse`. Entity ↔ DTO dönüşümü order-service'teki gibi **MapStruct** ile otomatik üretiliyor: `PaymentMapper` (`Payment` → `PaymentResponse`, `PaymentAttemptMapper`'ı iç içe kullanıyor) ve `PaymentAttemptMapper` (`PaymentAttempt` → `PaymentAttemptResponse`).

İş mantığı `PaymentService` arayüzü ve `PaymentServiceImpl` implementasyonu üzerinden yürütülüyor: tüm ödemeleri sayfalı listeleme, tekil ödeme getirme, iade, ve manuel ödeme oluşturma. Asenkron (event-tetiklemeli) ödeme akışı ise ayrı bir arayüzde: `PaymentEventProcessingService`/`PaymentEventProcessingServiceImpl` — bu ayrım order-service'teki `OrderService` (senkron/API) ile `OrderEventProcessingService` (asenkron/Kafka) ayrımıyla birebir aynı gerekçeye dayanıyor: HTTP isteğine cevap veren kodla arka planda Kafka event'i işleyen kodun sorumlulukları ve hata yönetimi farklı, karıştırılmaması gerekiyor.

Hem senkron hem asenkron akışta ortak olan "PSP'yi çağır, sonucu işle" adımı `PaymentProcessingHelper` adlı ayrı bir bileşene çıkarıldı (bkz. bölüm 12) — kod tekrarını önlemek için.

## 5. Controller Katmanı

`PaymentController`, `/api/v1/payments` altında REST endpoint'lerini sunuyor: ödeme oluşturma (POST), tüm ödemeleri sayfalı listeleme (GET), tekil ödeme getirme (GET /{id}), iade (POST /{id}/refund). Controller katmanı order-service'teki gibi kasıtlı olarak ince tutuldu — hiçbir iş kuralı burada yazılmadı, sadece HTTP isteğini servis katmanına yönlendirip uygun HTTP status kodunu (201 Created + Location header, 200 OK) dönüyor.

## 6. Hata Yönetimi

order-service ile birebir aynı strateji: tüm iş kuralı hataları `BaseException` adlı ortak bir soyut sınıftan türetiliyor, her alt sınıf kendi HTTP status kodunu ve hata kodunu taşıyor. Tanımlanan özel exception'lar: `PaymentNotFoundException` (404), `PaymentRefundException` (422 — sadece `COMPLETED` ödemeler iade edilebilir), `DuplicateRequestException` (409 — aynı `paymentRequestId` ile eşzamanlı yarış kaybedildiğinde), `PaymentAlreadyProcessedException` (409 — bir siparişin zaten `COMPLETED` bir ödemesi varken tekrar ödeme denendiğinde), `OrderNotPayableException` (409 — sipariş `PENDING_PAYMENT` durumunda değilse), `InvalidPaymentStateException`, `OrderNotFoundException`/`DownstreamAccessException`/`ServiceUnavailableException` (order-service'e Feign çağrısı başarısız olduğunda, `OrderClient`'ın fallback metodundan fırlatılıyor), `OutboxPersistenceException`.

`GlobalExceptionHandler` (`@RestControllerAdvice`) bu hataları yakalayıp RFC 7807 (`ProblemDetail`) formatında döndürüyor. order-service'e göre iki ekstra handler'ı var: `ObjectOptimisticLockingFailureException` (409, "Payment was updated by another operation, please retry" — bölüm 3'teki `@Version` korumasının HTTP karşılığı) ve `FeignException` bazlı ayrım (404 "kaynak bulunamadı" ile 503 "servis kullanılamıyor" arasında, order-service'teki bölüm 17'de anlatılan aynı netleştirmeyle).

## 7. Mock PSP Entegrasyonu ve FR-25

Gerçek bir ödeme sağlayıcısıyla (Stripe, iyzico vb.) entegrasyon bu aşamada kapsam dışı bırakıldı — hem gerçek bir PSP hesabı/sandbox'ı gerektirmesi hem de testlerin gerçek para hareketi veya harici bir servise bağımlı olmasını istememek nedeniyle, `MockPspClient` arayüzü ve `MockPspClientImpl` uygulaması yazıldı. Bunun amacı gerçekçi ama tamamen kontrol edilebilir bir ödeme davranışı simüle etmek.

FR-25 kapsamında `MockPspClientImpl`, ödeme yöntemine göre farklı bir "PSP profili" uyguluyor — gerçek hayatta kredi kartı, banka havalesi ve cüzdan ödemelerinin başarı oranı ve işlem süresi gerçekten farklı olduğu için bu ayrım bilinçli: `CREDIT_CARD` %85 başarı oranı ve 100-300ms gecikme, `BANK_TRANSFER` %95 başarı oranı ve 500-1000ms gecikme (banka sistemlerinin daha yavaş ama daha güvenilir olduğu varsayımı), `WALLET` %98 başarı oranı ve 50-150ms gecikme (dijital cüzdanların en hızlı/güvenilir yöntem olduğu varsayımı). Başarısızlık sebep havuzu da yöntem bazlı ayrıldı (ör. kredi kartı için "Insufficient funds"/"Card declined"/"Card expired", banka havalesi için "Bank account not found"/"Transfer limit exceeded").

Demo ve test senaryolarını kolaylaştırmak için Stripe'ın test-kartı konvansiyonuna benzer, deterministik davranan iki kart numarası suffix'i eklendi: `...0002` her zaman "Card declined", `...9995` her zaman "Insufficient funds" döndürüyor — bu sayede "kart reddedildi" akışı canlı ortamda rastgeleliğe bağlı kalmadan test edilebiliyor.

`CardValidator` (util paketi) ise mock PSP'den bağımsız, saf bir doğrulama sınıfı: Luhn algoritmasıyla kart numarası formatının geçerliliğini (13-19 haneli, checksum doğru) ve son kullanma tarihinin geçmişte olmadığını kontrol ediyor. Bu doğrulama PSP'ye hiç gitmeden, `PaymentServiceImpl.createPayment` içinde en başta yapılıyor — geçersiz bir kart bilgisiyle gereksiz yere "PSP çağrısı" simülasyonu (ve onun getirdiği yapay gecikme) yapılmasının önüne geçiyor. Aynı Luhn/expiry mantığı frontend'de de (`telco-crm-fe/src/utils/cardValidation.ts`) birebir tekrarlandı — kullanıcıya anında geri bildirim vermek için.

## 8. Kimlik Doğrulama

`payment-service` de order-service gibi Keycloak üzerinden OAuth2/JWT tabanlı kimlik doğrulama kullanıyor, ama bu koruma servis geliştirmenin en başında değil, entity/exception/event/outbox altyapısı oturduktan sonra ayrı bir adımda eklendi (`SecurityConfig` sınıfı). Bunun nedeni, o ana kadar servisin kimlik doğrulaması olmadan (herkese açık) çalışıyor olmasıydı — yani `/api/v1/payments` uçları, güvenlik katmanı eklenene kadar teorik olarak kimliği doğrulanmamış herhangi bir istemciden ödeme oluşturma/iade isteği kabul edebiliyordu. Bu bir güvenlik açığı olarak tespit edilip, order-service'teki `SecurityConfig` ile aynı desende (actuator/swagger hariç tüm endpoint'ler JWT ister, `oauth2ResourceServer().jwt()`, CSRF devre dışı, stateless session) kapatıldı. Aynı adımda `PaymentAuditService`'in "kim yaptı" bilgisini de doğru okuyabilmesi için audit mekanizması da (bkz. bölüm 18) birlikte eklendi.

`payment-service`'in `order-service`'e yaptığı Feign çağrıları için de order-service'teki `FeignJwtInterceptor` deseni birebir taşındı: gelen isteğin `Authorization` header'ı varsa (kullanıcı tetiklemeli manuel akış) aynen forward ediliyor; yoksa (Kafka event'inden tetiklenen otomatik akış, ortada bir HTTP isteği/JWT olmadığı için) `client_credentials` grant'iyle `payment-service` kendi adına bir token alıp kullanıyor. Bu ayrım, `OrderClient.getOrderById`'nin hem manuel hem otomatik akıştan çağrılabilmesini (bkz. bölüm 12 ve 13) mümkün kılıyor.

## 9. Outbox Pattern ve Kafka Event Yayınlama

Aynı gerekçeyle (veritabanına yazma ile Kafka'ya yayınlama işlemini atomik yapmak) order-service'teki outbox pattern birebir taşındı: `OutboxService.saveEvent(...)`, olayı JSON'a serileştirip aynı transaction içinde `outbox` tablosuna yazıyor; gerçek Kafka yayınlama işini Debezium CDC'ye bırakıyor (bkz. bölüm 11). Serileştirme hatası ile veritabanı yazma hatası, order-service'te sonradan yapılan ayrımla aynı şekilde baştan itibaren ayrı ele alındı (`OutboxPersistenceException`, sebebe göre farklı log mesajı).

Yayınlanan olaylar: `PaymentCompletedEvent` (`eventId`, `occurredAt`, `orderId`, `paymentId`) — ödeme başarıyla tamamlandığında; `PaymentFailedEvent` (`eventId`, `occurredAt`, `orderId`, `reason`) — retry mekanizması tüm denemeleri tükettiğinde (bkz. bölüm 15); `PaymentRefundedEvent` (`eventId`, `occurredAt`, `orderId`, `paymentId`, `refundedAmount`) — hem manuel iade hem saga kompanzasyon akışında (bkz. bölüm 17).

## 10. Inbox Pattern (Idempotency)

`payment-service`, order-service'ten `OrderCreatedEvent`'i ve (varsayımsal olarak) subscription-service'ten `SubscriptionActivationFailedEvent`'i Kafka üzerinden tüketiyor. Kafka'da "en az bir kez teslim" garantisi olduğu için aynı event iki kez gelebilir; bunu tolere etmek için order-service'teki `ProcessedEvent` mekanizması birebir taşındı — her event işlenmeden önce daha önce işlenip işlenmediği (`eventId` ile) kontrol ediliyor, işlendiyse tekrar işlenmiyor. Bu kontrol hem `processOrderCreated` hem `processSubscriptionActivationFailed` metotlarının en başında yapılıyor.

## 11. Debezium CDC Entegrasyonu

Outbox tablosundaki kayıtları Kafka'ya taşımak için order-service'teki gibi Debezium CDC kullanılıyor — bir poller yerine Postgres'in WAL'ini (write-ahead log) okuyan bir connector. `docker/connectors/payment-outbox-connector.json` dosyası `payment-db`'nin `outbox` tablosunu izleyecek şekilde tanımlandı (`table.include.list: public.outbox`, `EventRouter` transform'u ile `outbox.topic` kolonundaki değeri gerçek Kafka topic adı olarak kullanıyor) ve `docker/register-connectors.sh` script'ine (diğer servislerin connector'larıyla aynı listeye) eklendi.

Bu script'in geliştirme sürecinde iki ayrı, birbirinden bağımsız sorunu oldu ve ikisi de ayrı zamanlarda fark edilip düzeltildi: script başlangıçta bash-only syntax (`${BASH_SOURCE[0]}`, `set -o pipefail`) içeriyordu, ama `debezium-init` container'ının çalıştığı `curlimages/curl` imajında bash yok — script `/bin/sh` ile çalıştırılıyor ve sessizce syntax hatasıyla patlıyordu; bu, tüm connector'ların (payment dahil) hiçbir zaman otomatik kaydolmamasına yol açıyordu. Script POSIX-sh uyumlu şekilde yeniden yazılarak düzeltildi. Ayrı bir sorun olarak, `debezium-init` servis tanımında connector'ın konuşacağı Kafka Connect adresini belirten `CONNECT_URL` ortam değişkeni hiç tanımlanmamıştı; script varsayılan olarak `http://localhost:8083`'ü kullanıyordu, ama `debezium-init` ile `debezium-connect` ayrı container'lar olduğu için `localhost` container'ın kendi içini işaret ediyor, `debezium-connect`'e hiç ulaşamıyordu. `docker-compose.yml`'deki `debezium-init` servisine `CONNECT_URL: http://debezium-connect:8083` eklenerek düzeltildi. Bu iki düzeltmeden sonra sıfırdan bir `docker compose up` sonrasında tüm connector'ların (order, payment dahil) otomatik ve hatasız `RUNNING` durumuna geçtiği doğrulandı.

Ayrıca geliştirme sırasında bir kez, `payment-db`'nin `outbox` tablosu bir Flyway checksum uyuşmazlığını gidermek için elle drop edilip yeniden oluşturulduğunda, Postgres'in o tabloya bağlı publication'ı (Debezium'un `publication.autocreate.mode: filtered` ile otomatik oluşturduğu) da geçersiz kaldığı, connector'ın "RUNNING" görünmesine rağmen hiçbir yeni satırı Kafka'ya taşımadığı gözlemlendi — connector'ın REST API üzerinden yeniden başlatılması (`POST /connectors/payment-outbox-connector/restart`) publication'ın güncel tabloyla otomatik yeniden oluşmasını sağladı. Bu, connector "RUNNING" görünmesinin tek başına "veri akıyor" anlamına gelmediğini gösteren, ileride benzer bir migration/schema müdahalesi yapılırsa hatırlanması gereken bir durum.

## 12. Otomatik Ödeme Akışı: OrderCreatedEvent Tüketimi

Servisin ilk tasarımında (bölüm 4'teki `PaymentEventProcessingService` ayrımıyla birlikte), sipariş oluşunca ödemenin **otomatik** başlaması öngörülmüştü: `PaymentEventConsumer`'da bir `Consumer<OrderCreatedEvent>` bean'i, order-service'in yayınladığı event'i dinliyor, `PaymentEventProcessingServiceImpl.processOrderCreated` bir `Payment` oluşturup `PaymentProcessingHelper` üzerinden mock PSP'yi çağırıyor, sonucu event olarak yayınlıyordu. Bu akış FR-27 (retry) ve saga kompanzasyon (bölüm 17) ile birlikte geliştirildi ve bir süre bu şekilde çalıştı.

Geliştirme sürecinin ilerleyen bir aşamasında (Muhammed'in "payment/saga realism" başlıklı checkpoint commit'i), bu otomatik akış **bilinçli olarak kaldırıldı** ve yerine kullanıcı tetiklemeli bir akış (`POST /api/v1/payments`, gerçek kart bilgisi girilerek — bkz. bölüm 13) getirildi. Gerekçe, commit mesajında açıkça belirtildiği gibi, ödemenin gerçekçi bir kullanıcı deneyimine (frontend'in bir ödeme formu göstermesi, kullanıcının kart bilgisi girmesi) bağlanması ve aynı checkpoint'te eklenen Luhn/expiry doğrulamasının (`CardValidator`) anlamlı olabilmesi için gerçek bir kart girişi noktasına ihtiyaç duyulmasıydı — otomatik, kartsız bir ödeme akışıyla kart doğrulamasının bir arada anlamı yoktu.

Ancak bu kaldırma, order-service tarafındaki varsayımla senkronize değildi: order-service hâlâ `OrderCreatedEvent`'i "birisi dinleyip ödemeyi otomatik başlatacak" varsayımıyla yayınlıyordu ve kendi geliştirme dokümanında (`ORDER_SERVICE_DEVELOPMENT.md`, Kafka Topics bölümü) bu event hâlâ "payment-service'in tükettiği" bir topic olarak listeleniyordu. Bu iki servisin bağımsız ilerleyen dallarının birleştirilmesi sırasında (reconciliation merge commit'i) bu tutarsızlık gözden kaçtı ve otomatik akış geri gelmeden birleşti — yani bir süre `payment-service` sadece manuel akışı destekliyor, order-service ise hâlâ eski varsayımla event yayınlıyor ama kimse dinlemiyor durumdaydı.

Bu tutarsızlık daha sonra, iki servisin uçtan uca (E2E) test edilmesi sırasında somut olarak tespit edildi: bir sipariş oluşturulduğunda hiçbir ödemenin otomatik tetiklenmediği, sadece manuel `POST /api/v1/payments` çağrısıyla ilerleyebildiği görüldü. Karar, otomatik akışı **geri getirmek** yönünde verildi — gerekçe, dokümana (hem order-service'in kendi Kafka Topics referansına hem sistemin genel saga tasarımına) uygunluk ve gerçek dünyada frontend dışında kalan entegrasyonların (örn. doğrudan API ile sipariş açan bir B2B entegrasyonu) da ödemenin otomatik ilerlemesine güvenebilmesiydi. `OrderCreatedEvent` record'u, `PaymentEventConsumer`'daki bean, `PaymentEventProcessingService` arayüzündeki metot ve `PaymentEventProcessingServiceImpl.processOrderCreated`'ın implementasyonu, kaldırılmadan önceki haliyle **birebir** geri eklendi — ama Luhn/kart doğrulaması bu akışa hiç dahil edilmedi, çünkü otomatik akışta gerçek bir kart bilgisi zaten yok (retry scheduler'ın da yaptığı gibi, `PaymentProcessingHelper.attemptInitialCharge`'a kart numarası olarak `null` geçiliyor — bkz. bölüm 15). Config tarafında da `orderCreatedEvent-in-0` binding'i ve `spring.cloud.function.definition`'a `orderCreatedEvent` girişi geri eklendi.

Sonuç olarak sistemde artık **iki** ödeme başlatma yolu bir arada var — bu durumun yarattığı yeni bir sorun ve çözümü bölüm 14'te anlatılıyor.

## 13. Manuel Ödeme Akışı ve Frontend Entegrasyonu

`POST /api/v1/payments`, frontend'in sipariş sihirbazında (`OrderWizard.tsx`) son adımda kullanıcıdan topladığı kart bilgilerini (`cardHolder`, `cardNumber`, `expiryDate`, `cvv`) `method` (ödeme yöntemi) ve `orderId` ile birlikte gönderdiği endpoint. `CreatePaymentRequest` DTO'sunda bilinçli olarak **tutar/para birimi/müşteri ID'si yok** — bunlar client'tan gelen değerlere güvenilmeden, her zaman `orderId` üzerinden `OrderClient.getOrderById` ile order-service'ten (source of truth) sunucu tarafında çekiliyor. Bu, kullanıcı tarafında JavaScript ile tutarın değiştirilip sunucuya "farklı bir tutar öde" denilmesi gibi bir saldırı yüzeyini baştan kapatıyor.

Frontend tarafında kart numarası ve son kullanma tarihi girişleri anlık olarak biçimlendiriliyor (4'lü gruplar halinde boşluklu kart numarası, `AA/YY` formatlı son kullanma tarihi) ve `cardValidation.ts` ile Luhn/expiry kontrolü client-side'da da yapılıyor — "Siparişi Tamamla" butonu, bu kontroller geçmeden ve bir KVKK onay kutusu işaretlenmeden aktif olmuyor. Bu, sunucuya geçersiz bir istek gitmeden önce kullanıcıya anında geri bildirim vermek için; sunucu tarafındaki `CardValidator` doğrulaması (bölüm 7) yine de son otorite ve tek başına client-side kontrole güvenilmiyor.

## 14. Otomatik ve Manuel Akış Arasındaki Çakışma ve Çözümü

Bölüm 12'de anlatılan otomatik akışın geri getirilmesi, frontend'in hâlâ manuel akışa göre yazılmış olmasıyla (bölüm 13) birleşince gerçek bir çakışma riski ortaya çıktı: bir sipariş oluşturulduğunda, order-service'in yayınladığı `OrderCreatedEvent` payment-service'e ulaşıp otomatik ödeme denemesini başlatabilirken, **aynı anda** frontend de kullanıcının girdiği kart bilgileriyle manuel `POST /api/v1/payments` çağrısı yapıyor. Bu iki yol, hangisinin önce yetiştiğine bağlı olarak iki farklı şekilde bozulabiliyordu:

- Otomatik akış önce tamamlanırsa, frontend'in manuel çağrısı `PaymentAlreadyProcessedException` (409) alır — kullanıcıya "kartınız reddedildi" gibi yanlış bir hata gösterilme riski doğar, oysa ödeme aslında (kartsız, otomatik olarak) zaten başarıyla tamamlanmıştır.
- Frontend'in manuel çağrısı önce yetişirse (Kafka/Debezium round-trip birkaç saniye sürebildiği için bu daha olası), sonra `OrderCreatedEvent` işlendiğinde `processOrderCreated`'ın o zamanki hali `findByOrderId` kontrolü yapmadan doğrudan yeni bir `Payment` satırı oluşturuyordu — aynı sipariş için **mükerrer bir ödeme kaydı** riski.

Çözüm olarak `PaymentEventProcessingServiceImpl.processOrderCreated`'ın en başına (inbox/`ProcessedEvent` kontrolünden hemen sonra, herhangi bir `Payment` oluşturulmadan önce) bir guard eklendi: `paymentRepository.findByOrderId(event.orderId())` doluysa (yani o sipariş için zaten bir ödeme kaydı — manuel akıştan gelmiş olabilir — varsa), otomatik akış hiçbir şey yapmadan (yeni satır oluşturmadan, PSP'yi çağırmadan) sessizce çıkıyor ve durumu log'luyor. Bu guard, bölüm 3'te bahsedilen `orderId` üzerindeki veritabanı UNIQUE kısıtıyla da örtüşüyor — guard olmasaydı bile ikinci `INSERT` şema seviyesinde reddedilirdi, ama guard bunu daha temiz bir şekilde (bir `DataIntegrityViolationException`/500 yerine sessiz bir "zaten var, atlanıyor" log satırıyla) ele alıyor.

Bu düzeltme üç ayrı senaryoyla doğrulandı:
1. **Frontend akışı simülasyonu**: sipariş oluşturulup hemen ardından (frontend'in yaptığı gibi) manuel `POST /api/v1/payments` çağrıldığında — hangi taraf yarışı kazanırsa kazansın (otomatik akış önce bitip manuel çağrı 409 alması ya da manuel çağrının önce bitip otomatik akışın guard'a takılması) veritabanında her zaman tam olarak **bir** `Payment` satırı oluştuğu doğrulandı.
2. **API bypass (frontend'i atlayarak)**: doğrudan `POST /api/v1/orders` çağrılıp hiçbir manuel ödeme yapılmadığında, otomatik akışın devreye girip bir `Payment` oluşturduğu (kartsız, `paymentRequestId` boş) ve siparişin `PAID`'e geçtiği doğrulandı — guard, manuel bir kayıt yokken otomatik akışı engellemiyor.
3. **Paralel yük**: birden fazla sipariş eşzamanlı olarak oluşturulup her biri için hemen manuel ödeme çağrısı gönderildiğinde, tüm siparişlerin her birinde tam olarak bir `Payment` satırı oluştuğu ve guard'ın log satırının (manuel çağrı kazandığı durumlarda) her sipariş için tam bir kez tetiklendiği doğrulandı.

## 15. FR-27: Ödeme Retry Mekanizması

İlk ödeme denemesi başarısız olduğunda (`PaymentProcessingHelper.attemptInitialCharge`, hem manuel hem otomatik akıştan çağrılan ortak nokta), ödeme `FAILED` durumuna geçiyor, `retryCount` 1'e ayarlanıyor ve `nextRetryAt` şu andan 24 saat sonrasına kuruluyor. `PaymentRetryScheduler`, `@Scheduled(fixedDelay = 60000)` ile her dakika çalışıp `nextRetryAt`'i geçmiş ve hâlâ `FAILED` olan ödemeleri (`findByStatusAndNextRetryAtBefore`) bulup tek tek yeniden dener.

Gecikme kademeleri artan şekilde tasarlandı — art arda başarısız olan bir kartı çok sık denemenin (hem PSP tarafında hem kullanıcı deneyimi açısından) anlamsız olduğu varsayımıyla: 2. deneme 72 saat sonra, 3. deneme 168 saat (1 hafta) sonra. `MAX_RETRY_COUNT` 3 olarak sabitlendi; 4. deneme de (toplamda ilk deneme + 3 retry = 4 deneme) başarısız olursa ödeme kalıcı olarak `FAILED` kalıyor (`nextRetryAt` `null`'a çekiliyor, bir daha hiç denenmiyor) ve `PaymentFailedEvent` yayınlanıyor — order-service bunu tüketip siparişi `CANCELLED`'a çeker (order-service'in `processPaymentFailed` metodu).

Zamanlanmış retry'lerin **kart bilgisi taşımadığı** bilinçli bir tasarım kararı: `PaymentRetryScheduler`, `mockPspClient.charge(...)`'ı her zaman `cardNumber` parametresine `null` geçerek çağırıyor. Gerekçe kod içindeki yorumda da açık: PCI kapsamını (kart verisiyle temas eden sistemlerin denetim yükünü) daraltmak için kart numarası hiçbir zaman veritabanında saklanmıyor, dolayısıyla arka planda otomatik çalışan bir zamanlanmış görevin elinde zaten kart bilgisi yok — yöntem bazlı mock PSP profiliyle (bölüm 7) kartsız deneniyor. Bunun pratik bir sonucu var: zamanlanmış retry'lerin gerçek başarı/başarısızlık sonucu tamamen mock PSP'nin rastgele başarı oranına bağlı — yani "4. denemede kalıcı başarısızlık" senaryosunu canlı ortamda deterministik olarak tetiklemenin bir yolu yok (test/geliştirme sırasında bu, veritabanında `retry_count`/`next_retry_at` alanlarının elle ileri sarılmasıyla aşılıyor).

## 16. FR-26: Idempotency (Backend ve Frontend)

**Backend**: `CreatePaymentRequest.paymentRequestId` alanı, client'ın (frontend veya doğrudan API entegrasyonu) her ödeme denemesine kendi ürettiği bir tekil kimlik atamasını sağlıyor. `PaymentServiceImpl.createPayment`, bu alanla `paymentRepository.findByPaymentRequestId` sorgusu yaparak daha önce görülüp görülmediğini kontrol ediyor; görülmüşse hiçbir yeni PSP çağrısı yapmadan mevcut kaydı aynen döndürüyor (replay). Görülmemişse yeni bir `Payment` satırı `saveAndFlush` ile kaydediliyor.

Burada `save` yerine bilinçli olarak `saveAndFlush` kullanılıyor — bu, order-service'in `IdempotencyKey` mekanizmasında da (bölüm 34) karşılaşılan aynı JPA/Hibernate bug'ının payment-service'teki karşılığı: `GenerationType.UUID` stratejili entity'lerde `save()` (persist), gerçek `INSERT`'i flush/commit anına kadar erteliyor; eşzamanlı iki istek aynı `paymentRequestId` ile gelirse, ikisi de "daha önce görülmedi" sonucunu alıp `save()` çağırabiliyor ve unique constraint ihlali (`DataIntegrityViolationException`) `saveAndFlush()` kullanılmadan hiç yakalanamıyordu (INSERT gerçekten transaction commit'inde olduğu için, `catch` bloğu çoktan geçilmiş oluyordu). `saveAndFlush()`'a geçilerek `INSERT`'in hemen, `catch` bloğunun etkili olabileceği anda gerçekleşmesi sağlandı; unique constraint ihlali artık düzgün yakalanıp `DuplicateRequestException` (409) olarak dönüştürülüyor. Aynı fix, keşfedildiğinde order-service'teki `OrderServiceImpl.createOrder`'a da uygulandı.

**Frontend**: order-service tarafına giden `POST /api/v1/orders` isteğine de aynı mantıkla bir `Idempotency-Key` HTTP header'ı eklendi (order-service'in kendi `Idempotency-Key` desteğini kullanan, `body`'deki `paymentRequestId`'den ayrı bir mekanizma) — `OrderWizard.tsx` component'i mount olduğunda `crypto.randomUUID()` ile bir kez üretilip state'te tutuluyor, ağ hatası sonrası kullanıcı "tekrar dene" yaptığında aynı key tekrar gönderiliyor.

Ödeme tarafında (`paymentRequestId`) ise aynı deseni **birebir** kopyalamak yanlış olurdu: `paymentRequestId` aynı kalırsa backend, önceki sonuç ne olursa olsun (başarılı ya da `FAILED`), hiçbir yeni PSP denemesi yapmadan eskisini aynen döndürüyor. Bir kartın reddedildiği bir denemede bu key sabit kalsaydı, kullanıcı kartını düzeltip tekrar denediğinde backend yine eski `FAILED` sonucunu döndürür, düzeltilmiş kart hiçbir zaman denenmezdi. Bunun için **hibrit bir strateji** benimsendi: `paymentIdempotencyKey` de mount'ta bir kez üretiliyor ve ağ hatası/timeout durumunda (network seviyesinde belirsizlik — istek sunucuda işlenmiş olabilir, aynı key ile replay güvenli) değiştirilmiyor; ama backend'den gerçek bir `FAILED` sonucu döndüğünde (kart kesin olarak reddedildi), key `handleCreateOrder` içinde bilinçli olarak yeniden üretiliyor — böylece "tekrar dene" gerçek bir yeni deneme oluyor, backend'in "önceki deneme `FAILED` oldu, aynı sipariş için yeni `paymentRequestId` ile tekrar deneniyor" dalı (bölüm 13/14'teki mevcut-ödeme-satırını-yeniden-kullanma mantığı) doğru şekilde tetikleniyor.

## 17. Saga Kompanzasyon Akışı

Sistemin saga tasarımında, ödeme tamamlandıktan sonra abonelik aktivasyonu (subscription-service) başarısız olursa, artık geri alınması gereken bir şey vardır: alınan ödeme. Bu senaryo subscription-service henüz hiç yazılmadığı için tamamen **varsayımsal** bir event şemasıyla (`SubscriptionActivationFailedEvent` — `eventId`, `occurredAt`, `orderId`, `reason`) geliştirildi; kod içinde bunu açıkça belirten bir TODO yorumu var. `PaymentEventProcessingServiceImpl.processSubscriptionActivationFailed`, bu event'i tükettiğinde: ilgili siparişin ödemesini `findByOrderId` ile bulur, ödeme `COMPLETED` değilse (zaten iade edilecek bir şey yoksa) işlemi atlar, `COMPLETED` ise ödemeyi `REFUNDED`'a çevirip `PaymentRefundedEvent` yayınlar ve audit log'a yazar.

Bu akış, order-service'in kendi geliştirme dokümanındaki (bölüm 19 ve 26) "bilinen eksik" olarak işaretlediği bir boşluğu kapatıyor: order-service, abonelik aktivasyonu başarısız olduğunda siparişi "kompanzasyon sürüyor" (`COMPENSATING`) durumuna alıyor ama nihai duruma (`FAILED`) geçişi "ödemenin gerçekten iade edildiğini bildiren bir event"e bağlamıştı — o doküman yazıldığı sırada bu event payment-service tarafında henüz yayınlanmıyordu. `PaymentRefundedEvent`'in payment-service'te üretilmesiyle, order-service'in `processPaymentRefunded` consumer'ı bu event'i tüketip siparişi `COMPENSATING`'den `FAILED`'a taşıyabiliyor — iki servisin bağımsız geliştirilen parçaları burada birbirini tamamlıyor.

## 18. Audit Log

`PaymentAuditLog` mekanizması, order-service'teki `OrderAuditLog` ile birebir aynı desende (aynı `@Service`/tek repository bağımlılığı yapısı, append-only, `Payment` ile JPA ilişkisi kurulmadan sadece `paymentId` düz alan olarak tutuluyor) `PaymentAuditService` üzerinden yazılıyor. Servis katmanındaki her durum değişikliğinden hemen sonra (`outboxService.saveEvent(...)` çağrılarıyla aynı noktalarda) `paymentAuditService.log(...)` çağrılıyor: ödeme oluşturulduğunda/tamamlandığında/başarısız olduğunda, retry denemelerinde, iade edildiğinde.

`performedBy` ("kim yaptı") çözümlemesi, `SecurityContextHolder`'daki mevcut `Authentication`'a bakarak yapılıyor — Kafka event'inden tetiklenen çağrılarda (otomatik akış, retry scheduler) ortada bir HTTP isteği/JWT olmadığı için bu alan `"SYSTEM"` oluyor, HTTP çağrısından gelen (manuel ödeme, iade) çağrılarda kimliği doğrulanmış kullanıcının bilgisi kullanılıyor. Burada order-service'teki ilk implementasyonda da (ve payment-service'in kendi ilk implementasyonunda da) yaşanan bir hata sonradan fark edilip düzeltildi: bu ortamdaki Keycloak realm'inin ürettiği JWT'lerde standart `sub` claim'i (`Authentication.getName()`'in okuduğu alan) **boş** — sadece `preferred_username` claim'i doluydu. İlk halde `resolvePerformedBy`, `Authentication.getName()`'i doğrudan kullanıyordu ve bu her zaman `null`/boş dönüyordu, dolayısıyla HTTP çağrılarından gelen işlemler de yanlışlıkla `"SYSTEM"` olarak kaydediliyordu. Düzeltme: `JwtAuthenticationToken` ise önce `preferred_username` claim'i okunuyor, o da boşsa `getName()`'e (JWT `sub` claim'ine) düşülüyor, o da yoksa `"SYSTEM"`.

Kod tabanında payment-service'e özgü, order-service'te karşılığı olmayan **ikinci, ayrı bir denetim mekanizması** daha var: `AuditLogEntry` entity'si, `AuditLogRepository`, ve bunu kullanan `PaymentAuditListener` (config paketinde) — `logCreate`/`logUpdate`/`logDelete` metotlarıyla genel amaçlı (herhangi bir entity tipi için) bir değişiklik geçmişi tutmak üzere tasarlanmış, `entity_type`/`entity_id`/`action`/`changed_by`/`old_values`/`new_values` (JSONB) kolonlarına sahip bir tablo (`audit_log`, `V2__create_audit_log_table.sql`). Ancak bu mekanizma şu an kod tabanında **hiçbir yerden çağrılmıyor** — `PaymentAuditListener`'ın tek referansı kendi dosyası ve kendi birim testi (`PaymentAuditListenerTest`); hiçbir servis sınıfı onu enjekte edip kullanmıyor. Bu, gerçek kullanılan mekanizmanın (`PaymentAuditLog`/`PaymentAuditService`) yanında yetim kalmış, migration'ı ve testi var ama üretim akışına hiç bağlanmamış bir bileşen — bkz. bölüm 21.

## 19. Distributed Tracing ve Gözlemlenebilirlik

order-service ile aynı gözlemlenebilirlik yığını kullanılıyor: Micrometer Tracing + OpenTelemetry bridge ile trace'ler üretiliyor, Zipkin exporter'ı üzerinden `http://localhost:9411`'e gönderiliyor (`management.tracing.sampling.probability: 1.0` — geliştirme ortamında tüm istekler örnekleniyor). Prometheus metrikleri `micrometer-registry-prometheus` ile `/actuator/prometheus`'ta açık. Loglar ECS (Elastic Common Schema) formatında, yapılandırılmış JSON olarak hem konsola hem `logs/payment-service.log` dosyasına yazılıyor.

Bu son nokta — dosyaya loglama — bir süre merkezi log toplama zincirinin (Loki/Promtail/Grafana) dışında kaldı: `docker-compose.yml`'deki `promtail` servisi sadece `order-service/logs` dizinini konteynere mount ediyordu, `payment-service/logs` hiç bağlı değildi; yani payment-service loglarının kendisi doğru formatta üretiliyor ama Loki'ye hiç ulaşmıyordu. `docker-compose.yml`'e `../payment-service/logs:/var/log/telco-crm/payment-service:ro` mount'u ve `promtail-config.yaml`'a order-service ile aynı desende (`job_name: payment-service`, aynı ECS `@timestamp` alanını okuyan pipeline stage) yeni bir `scrape_configs` girişi eklenerek payment-service logları da Loki üzerinden sorgulanabilir hale getirildi.

Ayrıca, `order-service`'in `customer-service`/`product-catalog-service`'e yaptığı Feign çağrılarının Zipkin'de hiç görünmediği (ne CLIENT span'i oluşuyor ne propagation çalışıyordu) fark edildiğinde kök neden araştırıldı: `spring-cloud-starter-openfeign` ve micrometer-tracing bağımlılıkları var ama `io.github.openfeign:feign-micrometer` classpath'te yoktu — Spring Cloud OpenFeign'in `MicrometerObservationCapability`'yi otomatik yapılandırması bu artifact'in varlığına şartlı olduğu için, eksikliğinde Feign istekleri hiçbir `Observation`/span'e sarılmıyordu. `feign-micrometer` order-service'in `pom.xml`'ine eklenerek düzeltildi ve canlı testte order-service→customer-service, order-service→product-catalog-service zincirinin artık tek bir `traceId` altında CLIENT/SERVER span'leriyle göründüğü doğrulandı. payment-service'in kendi `OrderClient`'ı da aynı Feign+Resilience4j deseniyle kurulu olduğu için teorik olarak aynı eksikliği taşıyordu, ama bu düzeltme henüz sadece order-service'e uygulandı — bkz. bölüm 21.

## 20. Testler

Servisin farklı katmanları için 21 test sınıfı yazıldı (order-service'teki desenle aynı: JUnit 5 + Mockito, Testcontainers kullanılmadı, saf birim testler): entity davranışları (`PaymentTest`), mapper'lar (`PaymentMapperTest`, `PaymentAttemptMapperTest`), mock PSP client'ın yöntem bazlı davranışı ve deterministik test kartları (`MockPspClientImplTest`), servis katmanı (`PaymentServiceImplTest`, `PaymentEventProcessingServiceImplTest`, `PaymentProcessingHelperTest`, `OutboxServiceTest`, `PaymentAuditServiceTest`), retry scheduler'ın gecikme kademeleri ve kalıcı başarısızlık dalı (`PaymentRetrySchedulerTest`), exception sınıfları ve `GlobalExceptionHandler`, Kafka consumer (`PaymentEventConsumerTest`), controller (`PaymentControllerTest`), ve genel amaçlı audit listener'ın (kullanılmıyor olsa da) doğru çalıştığı (`PaymentAuditListenerTest`).

`jacoco-maven-plugin`, order-service'te de sonradan fark edilip eklenen aynı eksiklik burada da yaşandı: kök `pom.xml` coverage verisinin `**/target/site/jacoco/jacoco.xml`'den okunmasını bekliyor ve JaCoCo sürümünü pinliyor, ama `payment-service`'in kendi `pom.xml`'inde plugin etkinleştirilmemişti — bu yüzden `mvn test` çalıştırıldığında bir coverage raporu üretilmiyor, SonarQube analizi bu modül için veri görmüyordu. `prepare-agent` ve `report` (test fazına bağlı) execution'larıyla plugin eklendi; `mvn test` sonrası `target/site/jacoco/jacoco.xml`'in oluştuğu ve kök dizinden `mvn sonar:sonar` çalıştırıldığında payment-service için coverage verisinin (yaklaşık %97 civarında) Sonar'a yansıdığı doğrulandı.

## 21. Bilinen Eksikler ve Gelecekte Yapılacaklar

Bu doküman yazıldığı an itibarıyla henüz tamamlanmamış veya bilinçli olarak ertelenmiş konular:

- **billing-service ile invoice bağlantısı eksik**: `billing-service`'in `BillingEventHandler`'ı `payment-completed-topic`'i dinliyor ve gelen event'ten bir `invoiceId` alanı okuyup ilgili faturayı "ödendi" işaretlemeyi (`invoiceService.markInvoicePaid`) bekliyor. Ama payment-service'in yayınladığı `PaymentCompletedEvent`'te (`eventId`, `occurredAt`, `orderId`, `paymentId`) **`invoiceId` alanı hiç yok** — `Payment` entity'si tamamen `orderId`-merkezli tasarlandı (bölüm 3'teki `orderId` UNIQUE kısıtı da bunu pekiştiriyor: bir sipariş = en fazla bir ödeme). Bu, billing-service'in fatura-ödeme eşleştirmesinin şu an **hiçbir zaman otomatik tetiklenmediği** anlamına geliyor — `invoiceIdObj` her zaman `null` geldiği için `markInvoicePaid` çağrısı hiç yapılmıyor. Bunu çözmek, `Payment` entity'sinin sipariş-ödemesi dışında fatura-ödemesini de temsil edip edemeyeceği (ya da ayrı bir kavram mı olması gerektiği) konusunda bir tasarım kararı gerektiriyor; bu doküman yazıldığı an bu karar verilmedi.
- **subscription-service henüz yazılmadı**: `SubscriptionActivationFailedEvent`'in şeması (bölüm 17) tamamen varsayımsal, gerçek subscription-service ile hiç doğrulanmadı. Ayrıca subscription-service'in aktivasyon *başarılı* olduğunda payment-service'e bildirmesi gereken bir event de yok/gerekmiyor (payment-service'in bunu bilmesine gerek yok, sadece başarısızlık durumunda ilgileniyor) — ama bu varsayımın da subscription-service yazıldığında yeniden doğrulanması gerekiyor.
- **feign-micrometer eksikliği payment-service'e henüz taşınmadı**: bölüm 19'da anlatılan Zipkin CLIENT span düzeltmesi sadece order-service'in `pom.xml`'ine uygulandı; `payment-service`'in kendi `OrderClient`'ı (Feign) da muhtemelen aynı eksikliği taşıyor, ama bu henüz doğrulanmadı/düzeltilmedi.
- **Genel amaçlı `PaymentAuditListener`/`AuditLogEntry` mekanizması kullanılmıyor**: bölüm 18'de anlatıldığı gibi, `audit_log` tablosu ve onu besleyen `PaymentAuditListener` migration'ı ve testiyle birlikte var ama hiçbir servis sınıfından çağrılmıyor — ya gerçek çağrı noktalarına bağlanmalı ya da kod tabanından temizlenmeli.
- **Yetkilendirme (yatay erişim kontrolü)**: order-service'teki aynı eksiklik burada da geçerli — kimliği doğrulanmış herhangi bir kullanıcı, başka bir müşteriye ait ödemeyi görüntüleyebilir/iade edebilir; ödeme sahipliği kontrolü henüz eklenmedi.
- **customer-service'in soft-delete'i filtrelememesi**: order-service'in kendi dokümanında da not edilen, kapsam dışı ama bilgi amaçlı bir gözlem — silinmiş bir müşteri hâlâ sorgulanabiliyor; payment-service bu bilgiye doğrudan bağımlı olmasa da (müşteri doğrulamasını order-service üzerinden dolaylı alıyor) not edilmeye değer.

---

## 22. Teknoloji Stack'i

Aşağıdaki liste `pom.xml`'deki bağımlılıklardan ve `mvn dependency:tree` ile alınan çözümlenmiş sürümlerden oluşturulmuştur. order-service ile aynı parent/BOM'u kullandığı için ortak bağımlılıkların sürümleri birebir aynı.

| Katman | Teknoloji | Sürüm |
|---|---|---|
| Dil | Java | 21 |
| Framework | Spring Boot | 4.0.6 |
| Bağımlılık yönetimi | Spring Cloud BOM | 2025.1.1 (çözümlenen artifact sürümü: 5.0.1) |
| Web | Spring Web (`spring-boot-starter-web`) | 4.0.6 |
| Validasyon | Spring Boot Starter Validation / Hibernate Validator | 9.0.1.Final |
| Veri erişimi (ORM) | Spring Data JPA / Hibernate ORM | 7.2.12.Final |
| Veritabanı | PostgreSQL (JDBC sürücüsü) | 42.7.10 |
| Şema versiyonlama | Flyway (`flyway-database-postgresql`) | 11.14.1 |
| Servis keşfi | Netflix Eureka Client (Spring Cloud) | 5.0.1 |
| Merkezi konfigürasyon | Spring Cloud Config Client | 5.0.1 |
| Servisler arası HTTP çağrısı | OpenFeign (`feign-core`) | 13.6 |
| Devre kesici / dayanıklılık | Resilience4j (`resilience4j-spring-boot3`) | 2.3.0 |
| Mesajlaşma | Spring Cloud Stream + Kafka Binder | 5.0.1 |
| Kafka istemcisi | `spring-kafka` / `kafka-clients` | 4.0.5 / 4.1.2 |
| CDC (Change Data Capture) | Debezium Connect (Postgres connector) | 3.0 |
| Kimlik doğrulama | Spring Security OAuth2 Resource Server + Client (JWT) | 4.0.6 |
| Kimlik sağlayıcı | Keycloak | (docker-compose imajı: `quay.io/keycloak/keycloak:26.1`) |
| Nesne dönüşümü (entity ↔ DTO) | MapStruct | 1.6.3 |
| Kod üretimi (boilerplate azaltma) | Lombok | 1.18.46 |
| JSON serileştirme | Jackson (`jackson-databind`) | 2.21.2 |
| Dağıtık izleme (tracing) | Micrometer Tracing (OpenTelemetry bridge) | 1.6.5 |
| Trace export | OpenTelemetry Zipkin Exporter | 1.55.0 |
| Trace toplama | Zipkin | (docker-compose imajı: `openzipkin/zipkin:latest`) |
| Metrikler | Micrometer Prometheus Registry | 1.16.5 |
| İzleme/health endpoint'leri | Spring Boot Actuator | 4.0.6 |
| API dokümantasyonu | springdoc-openapi (`springdoc-openapi-starter-webmvc-ui`) | 3.0.3 |
| Yapılandırılmış loglama | Logback + ECS formatı (Spring Boot structured logging) | 4.0.6 |
| Log toplama/görselleştirme | Loki + Promtail + Grafana | (docker-compose imajları: Loki 3.2.0, Promtail 3.2.0, Grafana 11.3.0) |
| Test | Spring Boot Starter Test (JUnit 5, Mockito vb.) | 4.0.6 (parent'tan miras) |
| Kod kalitesi analizi | SonarQube Scanner Maven Plugin | 4.0.0.4121 (kök `pom.xml`'de tanımlı) |
| Test kapsamı (coverage) aracı | JaCoCo | 0.8.12 |
| Konteynerleştirme / yerel altyapı | Docker Compose | — |

## 23. Paket Yapısı

`com.telcocrm.paymentservice` kök paketi altında, her paket tek bir sorumluluğa karşılık gelecek şekilde ayrıştırılmıştır:

- **(kök paket)** — `PaymentServiceApplication`: Spring Boot uygulamasının giriş noktası.
- **client** — `OrderClient`: order-service'e senkron HTTP çağrısı yapan Feign arayüzü, circuit breaker/fallback mantığı dahil; `MockPspClient`/`MockPspClientImpl`: mock ödeme sağlayıcısı (bkz. bölüm 7).
- **client.dto** — `OrderResponse`, `PspChargeResult`: dış servisten/mock PSP'den dönen cevapların yerel temsilleri.
- **config** — `SecurityConfig` (JWT tabanlı kimlik doğrulama kuralları), `FeignJwtInterceptor` (giden Feign isteklerine JWT ekleyen interceptor), `PaymentAuditListener`/`OpenApiConfig` (bkz. bölüm 18 ve 21).
- **controller** — `PaymentController`: dışarıya açılan REST endpoint'leri.
- **dto.request** — `CreatePaymentRequest`, `RefundRequest`: gelen isteklerin şekli ve validasyon kuralları.
- **dto.response** — `PaymentResponse`, `PaymentAttemptResponse`: dışarıya dönen cevapların şekli.
- **entity** — `Payment`, `PaymentAttempt`, `OutboxEvent`, `ProcessedEvent`, `PaymentAuditLog`, `AuditLogEntry`: JPA ile veritabanına eşlenen domain nesneleri.
- **entity.enums** — `PaymentStatus`, `PaymentMethod`: sabit durum/tip kümeleri.
- **event.publish** — `PaymentCompletedEvent`, `PaymentFailedEvent`, `PaymentRefundedEvent`: outbox üzerinden Kafka'ya yayınlanan olayların şekli.
- **event.consume** — `OrderCreatedEvent`, `SubscriptionActivationFailedEvent`: Kafka'dan tüketilen olayların şekli.
- **exception** — `BaseException` ve ondan türeyen özel exception'lar, `GlobalExceptionHandler`.
- **kafka.consumer** — `PaymentEventConsumer`: Spring Cloud Stream fonksiyonel modeliyle Kafka'dan gelen event'leri ilgili servis metoduna yönlendiren `Consumer` bean'leri.
- **mapper** — `PaymentMapper`, `PaymentAttemptMapper`: MapStruct ile üretilen entity ↔ DTO dönüşüm arayüzleri.
- **repository** — `PaymentRepository`, `PaymentAttemptRepository`, `OutboxRepository`, `ProcessedEventRepository`, `PaymentAuditLogRepository`, `AuditLogRepository`: Spring Data JPA repository arayüzleri.
- **scheduler** — `PaymentRetryScheduler`: FR-27 zamanlanmış retry görevi (bkz. bölüm 15).
- **service** / **service.impl** — `PaymentService`/`PaymentServiceImpl` (senkron ödeme orkestrasyonu), `PaymentEventProcessingService`/`PaymentEventProcessingServiceImpl` (gelen Kafka event'lerinin işlenmesi), `PaymentProcessingHelper` (senkron/asenkron akışlar arasında paylaşılan PSP-çağırma mantığı), `OutboxService`, `PaymentAuditService`.
- **util** — `CardValidator`: Luhn algoritması ve son kullanma tarihi doğrulaması (bölüm 7).

## 24. API Referansı

Tüm endpoint'ler `/api/v1/payments` altında toplanmıştır ve (actuator/swagger hariç) geçerli bir JWT (`Authorization: Bearer <token>`) gerektirir. Hata cevapları RFC 7807 `ProblemDetail` formatındadır: `{ "type", "title", "status", "detail", "errorCode", "timestamp" }`.

### POST /api/v1/payments — Ödeme oluşturma

**Request body** (`CreatePaymentRequest`):
```json
{
  "paymentRequestId": "istemcinin-urettigi-tekil-id",
  "orderId": "b3f1c2a0-....-....-............",
  "method": "CREDIT_CARD",
  "cardHolder": "Ali Veli",
  "cardNumber": "4242424242424242",
  "expiryDate": "12/28",
  "cvv": "123"
}
```
- `paymentRequestId`: zorunlu, boş olamaz — idempotency anahtarı (bkz. bölüm 16).
- `orderId`: zorunlu, UUID.
- `method`: zorunlu, `CREDIT_CARD` | `BANK_TRANSFER` | `WALLET`.
- `cardHolder`: zorunlu, boş olamaz.
- `cardNumber`: zorunlu, 13-19 haneli (boşluklu girilebilir), Luhn algoritmasıyla doğrulanır.
- `expiryDate`: zorunlu, `AA/YY` formatında, geçmiş bir tarih olamaz.
- `cvv`: zorunlu, 3-4 haneli.

**Response body** (201 Created, `Location: /api/v1/payments/{id}`): `PaymentResponse` (bkz. aşağıdaki ortak şema).

**Olası durum kodları:**
| Kod | Durum |
|---|---|
| 201 | Ödeme isteği alındı (PSP sonucu ne olursa olsun — `status` alanına bakılmalı, `COMPLETED` olmayan bir 201 de mümkündür) |
| 400 | Validasyon hatası, bozuk JSON, veya Luhn/expiry doğrulaması başarısız |
| 404 | `orderId` order-service'te bulunamadı |
| 409 | Sipariş `PENDING_PAYMENT` durumunda değil (`ORDER_NOT_PAYABLE`), sipariş için zaten `COMPLETED` bir ödeme var (`PAYMENT_ALREADY_PROCESSED`), aynı `paymentRequestId` ile eşzamanlı bir istek yarışı kaybedildi (`DUPLICATE_REQUEST`), veya eşzamanlı güncelleme çakışması (`CONCURRENT_UPDATE`, optimistic lock) |
| 503 | `order-service`'e ulaşılamıyor (circuit breaker devrede) |

### GET /api/v1/payments — Ödeme listeleme (sayfalı)

**Query parametreleri:** standart Spring Data `Pageable` (`page`, `size`, `sort`).

**Response body** (200 OK): Spring Data `Page<PaymentResponse>`.

### GET /api/v1/payments/{id} — Tekil ödeme getirme

**Response body** (200 OK): `PaymentResponse` (`attempts` listesi dahil — bir ödemenin tüm PSP deneme geçmişi).

**Olası durum kodları:** 200, 404 (ödeme bulunamadı).

### POST /api/v1/payments/{id}/refund — İade

**Request body** (`RefundRequest`):
```json
{
  "reason": "Müşteri talebi"
}
```
- `reason`: zorunlu, boş olamaz.

**Response body** (200 OK): `PaymentResponse`.

**Olası durum kodları:**
| Kod | Durum |
|---|---|
| 200 | İade edildi |
| 400 | Validasyon hatası |
| 404 | Ödeme bulunamadı |
| 422 | Ödeme `COMPLETED` durumunda değil (sadece tamamlanmış ödemeler iade edilebilir) |

### Ortak response şeması: `PaymentResponse`

```json
{
  "id": "uuid",
  "orderId": "uuid",
  "customerId": "uuid",
  "amount": 149.90,
  "currency": "TRY",
  "method": "CREDIT_CARD | BANK_TRANSFER | WALLET",
  "status": "PENDING | COMPLETED | FAILED | REFUNDED",
  "externalRef": "MOCK-REF-... veya null",
  "failureReason": "string veya null",
  "paidAt": "2026-07-16T10:00:00Z veya null",
  "attempts": [
    {
      "id": "uuid",
      "attemptNo": 1,
      "response": "MOCK_PSP_APPROVED veya bir başarısızlık sebebi",
      "attemptedAt": "2026-07-16T10:00:00Z"
    }
  ],
  "createdAt": "2026-07-16T10:00:00Z",
  "updatedAt": "2026-07-16T10:00:00Z"
}
```

## 25. Kafka Topics

`payment-service`, olayları veritabanına yazıp Debezium CDC ile Kafka'ya yayınlar (outbox pattern — bkz. bölüm 9 ve 11); gelen olayları ise doğrudan Spring Cloud Stream Kafka binder'ı ile tüketir.

### Yayınlanan (publish) topic'ler

| Topic | Event | Ne zaman yayınlanır | Alanlar |
|---|---|---|---|
| `payment-completed-topic` | `PaymentCompletedEvent` | Bir ödeme (ilk deneme veya retry) başarıyla tamamlandığında | `eventId`, `occurredAt`, `orderId`, `paymentId` |
| `payment-failed-topic` | `PaymentFailedEvent` | Retry mekanizması tüm denemeleri (4) tükettiğinde | `eventId`, `occurredAt`, `orderId`, `reason` |
| `payment-refunded-topic` | `PaymentRefundedEvent` | Manuel iade (`POST /{id}/refund`) veya saga kompanzasyon akışında otomatik iade | `eventId`, `occurredAt`, `orderId`, `paymentId`, `refundedAmount` |

`billing-service`'in `payment-completed-topic`'i dinleyip bir `invoiceId` alanı beklediği, ama bu alanın event şemasında olmadığı — bkz. bölüm 21'deki bilinen eksik.

### Tüketilen (consume) topic'ler

`configs/payment-service/application-dev.yml` içindeki `spring.cloud.stream.bindings` altında tanımlıdır; her biri `PaymentEventConsumer`'daki aynı isimli bir `Consumer<T>` bean'ine bağlanır.

| Topic | Binding adı | Event | Alanlar | İşleyen metod |
|---|---|---|---|---|
| `order-created-topic` | `orderCreatedEvent-in-0` | `OrderCreatedEvent` | `eventId`, `occurredAt`, `orderId`, `customerId`, `totalAmount`, `currency`, `email`, `firstName`, `lastName` (order-service'in yayınladığı gerçek event'te ayrıca `tariffCode` de var, ama payment-service bunu kullanmadığı için kendi kopyasına eklemedi — Jackson fazladan alanları görmezden geliyor) | `processOrderCreated` — otomatik ödeme akışını başlatır (bkz. bölüm 12) |
| `subscription-activation-failed-topic` | `subscriptionActivationFailedEvent-in-0` | `SubscriptionActivationFailedEvent` | `eventId`, `occurredAt`, `orderId`, `reason` | `processSubscriptionActivationFailed` — otomatik iade tetikler (bkz. bölüm 17) |

Her tüketici, aynı event'in tekrar işlenmesini önlemek için önce `ProcessedEvent` tablosunda `eventId`'nin daha önce işlenip işlenmediğini kontrol eder (bkz. bölüm 10).

> Not: `SubscriptionActivationFailedEvent`'in şeması, subscription-service henüz tam olarak geliştirilmediği için şu an varsayımsaldır (bkz. bölüm 21).

## 26. Ortam Kurulumu

Servisin yerelde ayağa kaldırılması için önerilen sıra (order-service ile aynı):

1. **Altyapı konteynerlerini başlat**: repo kökünde `docker/docker-compose.yml` ile `docker compose up -d` çalıştırılır. Bu, `payment-db` (Postgres, port 5408) dahil tüm ortak altyapıyı (Kafka, debezium-connect, Keycloak, Zipkin, Loki/Promtail/Grafana) ayağa kaldırır.
2. **discovery-server'ı başlat** (Eureka, port 8761).
3. **config-server'ı başlat** (port 8888) — `payment-service`'in başlaması için zorunlu bağımlılık.
4. **payment-service'i başlat** (`dev` profili aktif, port 9008).
5. **Debezium connector'larını kaydet**: `debezium-connect` konteyneri ayaktayken `docker/register-connectors.sh` çalıştırılır (ya da `docker compose run --rm debezium-init` ile manuel tetiklenir). Bu adım atlanırsa `outbox` tablosuna yazılan event'ler hiçbir zaman Kafka'ya taşınmaz.

Dikkat edilmesi gerekenler:
- `payment-service`, docker-compose içinde bir konteyner olarak tanımlı **değildir**; host üzerinde çalıştırılır.
- `payment-db`'nin şeması Flyway migration'larıyla (`V1`, `V2`, `V3`) yönetiliyor; migration dosyaları bir kez uygulandıktan sonra değiştirilirse (checksum mismatch) servis açılışta hata verir — bu durumda geliştirme ortamında tabloların (ve `flyway_schema_history`'nin) drop edilip yeniden oluşturulması gerekir, ki bu da Debezium publication'ının yeniden oluşması için ilgili connector'ın restart edilmesini gerektirir (bkz. bölüm 11).
- `register-connectors.sh` idempotent'tir — zaten kayıtlı bir connector'ı tekrar kaydetmeye çalışmaz.

## 27. Test ve Kod Kalitesi

**Testleri çalıştırma:**
```
cd payment-service
mvn test
```
Testler Testcontainers kullanmaz; gerçek bir Postgres/Kafka bağlantısı gerektirmez (saf birim testler, JUnit 5 + Mockito).

Mevcut test kapsamı (21 sınıf, bkz. bölüm 20) şu katmanları içerir: entity davranışları, MapStruct mapper'ları, mock PSP client'ın yöntem bazlı ve deterministik davranışları, servis katmanı (`PaymentServiceImpl`, `PaymentEventProcessingServiceImpl`, `PaymentProcessingHelper`), retry scheduler, `OutboxService`, `PaymentAuditService`, exception sınıfları ve `GlobalExceptionHandler`, Kafka consumer, controller.

**SonarQube analizi:**
Kod kalitesi analizi, kök `pom.xml`'de tanımlı `sonar-maven-plugin` ile tüm modülleri **tek bir Sonar projesi** (`sonar.projectKey=telco-crm`) altında topluyor. Analiz repo kökünden şu şekilde çalıştırılır:
```
mvn sonar:sonar -Dsonar.token=<token>
```
`payment-service`'in kendi `pom.xml`'ine `jacoco-maven-plugin` (`prepare-agent` ve test fazına bağlı `report` execution'larıyla) eklendikten sonra, `mvn test` çalıştırıldığında `target/site/jacoco/jacoco.xml` üretiliyor ve bu Sonar analizine coverage verisi olarak yansıyor — düzeltme öncesi bu veri hiç görünmüyordu (order-service'te de aynı eksiklik ayrıca giderildi).
