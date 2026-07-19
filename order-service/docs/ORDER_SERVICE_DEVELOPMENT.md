# order-service Geliştirme Süreci

Bu doküman, `order-service`'in baştan bugüne kadar hangi adımlarla, hangi gerekçelerle geliştirildiğini anlatır. Kod içermez; her adımda ne yapıldığı ve **neden** o şekilde yapıldığı anlatılır.

---

## 1. Servisin Amacı ve Yeri

`order-service`, telco-crm mikroservis mimarisinde sipariş yaşam döngüsünü yöneten servistir: bir müşteri sipariş oluşturduğunda müşteri ve ürün doğrulamasını yapar, siparişi kaydeder, ödeme ve abonelik aktivasyonu gibi başka servislerde gerçekleşen adımları bir **saga** (dağıtık işlem) üzerinden takip eder ve sonuçta siparişi tamamlar ya da iptal eder. Sistemde database-per-service prensibi uygulanıyor; yani `order-service` kendi Postgres veritabanına sahip, müşteri veya ürün bilgisini kendi tablolarında tutmuyor, ihtiyaç anında ilgili servislerden (customer-service, product-catalog-service) senkron olarak veya Kafka event'leri ile alıyor.

## 2. Temel Altyapı Kurulumu

İlk adımda servisin iskeleti oluşturuldu: Spring Boot projesi, Maven bağımlılıkları (Spring Web, Spring Data JPA, Spring Security OAuth2 Resource Server, Spring Cloud OpenFeign, Spring Cloud Stream/Kafka, Resilience4j, Flyway, Lombok, MapStruct) eklendi. Servis, diğer servislerle birlikte ortak bir `docker-compose.yml` üzerinden ayağa kalkan altyapıya (Postgres, Kafka, Keycloak, Zipkin, Loki/Grafana, Debezium Connect) bağlanacak şekilde tasarlandı. Her servisin kendi veritabanı olduğu için `order-service` için ayrı bir Postgres instance'ı (`order-db`) tanımlandı.

Konfigürasyon dosyaları, kod deposundan ayrı bir `configs/order-service` klasöründe tutuluyor (`application.yml` ana profil seçimini yapıyor, `application-dev.yml` gerçek bağlantı bilgilerini — veritabanı, Keycloak, Kafka broker adresleri — içeriyor). Bu ayrım, hassas/ortam bazlı bilgilerin uygulama koduyla karışmamasını sağlıyor.

## 3. Entity, Enum ve Repository Katmanı

Sipariş domain modeli şu ana yapı taşlarıyla kuruldu:

- **Order**: bir siparişin kendisi — müşteri ID'si, durumu, toplam tutarı, para birimi, ödeme/abonelik referansları, iptal sebebi, oluşturulma/güncellenme zamanları. Silme işlemi kalıcı değil; `deleted` alanıyla soft-delete uygulanıyor, çünkü sipariş kayıtlarının denetim (audit) amaçlı saklanması gerekiyor.
- **OrderItem**: bir siparişin içindeki her bir ürün kalemi — ürün kodu, adı, tipi (tarife/addon/VAS), adet, birim fiyat, kalem toplamı.
- **SagaState**: bir siparişin dağıtık işlem sürecindeki anlık durumu — hangi adımda olduğu, kaç kez yeniden denendiği, varsa hata mesajı. Her sipariş ile bire-bir ilişkili.
- **OutboxEvent**: outbox pattern için, henüz dışarıya (Kafka'ya) yayınlanmamış olayların geçici olarak tutulduğu tablo.
- **ProcessedEvent**: inbox pattern için, daha önce işlenmiş gelen event ID'lerinin tutulduğu tablo (aynı event'in iki kez işlenmesini engellemek için).

Enum'lar: `OrderStatus` (DRAFT, PENDING_PAYMENT, PAID, FULFILLED, CANCELLED), `SagaStep` (ORDER_CREATED, AWAITING_PAYMENT, AWAITING_SUBSCRIPTION, COMPLETED, COMPENSATING, FAILED), `OrderItemType` (TARIFF, ADDON, VAS).

Repository katmanı Spring Data JPA ile kuruldu; `createdAt`/`updatedAt` alanları elle set edilmek yerine Spring Data JPA Auditing (`@CreatedDate`/`@LastModifiedDate`) ile otomatikleştirildi — bu, her kayıt/güncelleme noktasında zaman damgasını unutma riskini ortadan kaldırdı.

## 4. DTO, Mapper ve Servis Katmanı

İstemciye (API tüketicisine) entity'lerin doğrudan sızmaması için ayrı request/response DTO'ları tanımlandı (`CreateOrderRequest`, `OrderItemRequest`, `CancelOrderRequest`, `OrderResponse`, `OrderItemResponse`). Entity ↔ DTO dönüşümü elle yazılan kod yerine **MapStruct** ile otomatik üretiliyor; bu hem tekrarlayan dönüşüm kodunu ortadan kaldırıyor hem de alan eklendiğinde/çıkarıldığında derleme zamanında hataları görünür kılıyor.

İş mantığı `OrderService` arayüzü ve `OrderServiceImpl` implementasyonu üzerinden yürütülüyor: sipariş oluşturma, tekil sipariş getirme, sipariş iptali. Servis katmanı; repository, dış servis client'ları (Feign) ve iş kuralları (rules katmanı, bkz. bölüm 9) arasında orkestrasyon yapıyor.

## 5. Controller Katmanı

`OrderController`, `/api/v1/orders` altında REST endpoint'lerini sunuyor: sipariş oluşturma (POST), tekil sipariş getirme (GET /{id}), sipariş iptali (POST /{id}/cancel). Controller katmanı kasıtlı olarak ince tutuldu — hiçbir iş kuralı burada yazılmadı, sadece HTTP isteğini servis katmanına yönlendirip uygun HTTP status kodunu (örn. 201 Created + Location header) dönüyor.

## 6. Hata Yönetimi (Exception Handling)

Baştan itibaren merkezi bir hata yönetimi stratejisi benimsendi: tüm iş kuralı hataları `BaseException` adlı ortak bir soyut sınıftan türetiliyor (her alt sınıf kendi HTTP status kodunu ve hata kodunu taşıyor — örn. `OrderNotFoundException` → 404, `OrderNotCancellableException` → 422, `InvalidOrderStateException` → 409). `GlobalExceptionHandler` (`@RestControllerAdvice`) bu hataları yakalayıp RFC 7807 (`ProblemDetail`) formatında, tutarlı bir gövdeyle döndürüyor. Bu yaklaşımın amacı: API tüketicilerinin hata durumlarını class'a bakmadan, tutarlı bir JSON şemasından (title, detail, errorCode, timestamp) okuyabilmesi.

## 7. Dış Servislerle İletişim: Feign Client'lar ve Circuit Breaker

`order-service`, müşteri bilgisini `customer-service`'ten, ürün/tarife bilgisini `product-catalog-service`'ten senkron HTTP çağrılarıyla (Spring Cloud OpenFeign) alıyor. Servisler birbirini Eureka üzerinden (service discovery, isimle) buluyor, sabit host/port bağımlılığı yok.

Bu çağrıların dış bir servise bağımlı olması nedeniyle, o servis yavaşladığında veya çöktüğünde `order-service`'in de kilitlenmesini önlemek için **Resilience4j Circuit Breaker** eklendi. Her Feign metodunun bir fallback'i var; devre açıldığında veya çağrı başarısız olduğunda anlamlı bir hata (servis kullanılamıyor durumunda 503, kaynak bulunamadıysa 404) döndürülüyor — bu ayrım daha sonra (bkz. bölüm 20) netleştirildi.

## 8. Kimlik Doğrulama: Keycloak / JWT

Servis, Keycloak üzerinden OAuth2/JWT tabanlı kimlik doğrulama kullanıyor (OAuth2 Resource Server). Gelen her istekte `Authorization: Bearer <token>` header'ındaki JWT, Keycloak'ın yayınladığı public key ile doğrulanıyor. Actuator, Swagger/OpenAPI endpoint'leri dışında tüm endpoint'ler kimlik doğrulama gerektiriyor.

## 9. İş Kurallarının Ayrıştırılması (Rules Katmanı)

Sipariş durumu geçişleri (iptal, ödeme tamamlandı, ödeme başarısız, abonelik aktive oldu gibi) ve fiyatlandırma mantığı, servis sınıflarından ayrılıp `OrderStateRules` ve `OrderPricingRules` adlı bağımsız sınıflara taşındı. Bunun amacı: durum makinesi mantığının (hangi durumdan hangi duruma geçilebilir, geçemezse hangi hata fırlatılır) servis katmanının orkestrasyon sorumluluğundan ayrışması, böylece hem test edilmesi hem de okunması kolaylaşıyor.

## 10. Outbox Pattern ve Kafka Event Yayınlama

Bir sipariş oluşturulduğunda veya durumu değiştiğinde, bu bilgiyi diğer servislere (örn. payment-service, subscription-service) Kafka üzerinden duyurmak gerekiyor. Ancak "veritabanına yaz" ile "Kafka'ya yayınla" işlemlerini aynı anda, atomik şekilde yapmak mümkün değil (biri başarılı olup diğeri başarısız olabilir, tutarsızlık doğar). Bu yüzden **outbox pattern** uygulandı: event, ana veritabanı işlemiyle aynı transaction içinde bir `outbox` tablosuna yazılıyor; Kafka'ya gerçek yayınlama işi ayrı bir mekanizmaya bırakılıyor (bkz. bölüm 12).

`OrderCreatedEvent`, `OrderCancelledEvent`, `OrderConfirmedEvent` bu şekilde yayınlanan olaylar.

## 11. Inbox Pattern (Idempotency) — Gelen Event'lerin İşlenmesi

`order-service`, `payment-service`'ten ödeme sonucu, `subscription-service`'ten abonelik aktivasyon sonucu event'lerini Kafka üzerinden tüketiyor (`PaymentCompletedEvent`, `PaymentFailedEvent`, `SubscriptionActivatedEvent`). Kafka'da "en az bir kez teslim" garantisi olduğu için aynı event iki kez gelebilir. Bunu tolere etmek için **inbox pattern** uygulandı: her event işlenmeden önce `ProcessedEvent` tablosunda daha önce işlenip işlenmediği kontrol ediliyor; işlendiyse tekrar işlenmiyor (idempotency).

## 12. Outbox Poller'dan Debezium CDC'ye Geçiş

Outbox tablosundaki kayıtları Kafka'ya taşımak için başlangıçta bir "poller" (veritabanını periyodik olarak tarayıp yeni kayıtları Kafka'ya gönderen bir arka plan görevi) düşünüldü, ama bu yaklaşım yerine **Debezium CDC (Change Data Capture)** tercih edildi. Debezium, Postgres'in write-ahead log'unu (WAL) okuyarak `outbox` tablosuna yapılan INSERT'leri gerçek zamanlı yakalıyor ve doğrudan Kafka'ya yayınlıyor. Bu yaklaşımın poller'a göre avantajı: uygulama içinde ek bir zamanlanmış görev/thread yönetmeye gerek kalmıyor, gecikme daha düşük, ve veritabanı üzerinde ekstra "hangi satırlar gönderildi" bookkeeping'i gerekmiyor.

## 13. JWT Relay Interceptor ve Müşteri Doğrulamasının Aktifleştirilmesi

`order-service`, `customer-service`'e Feign ile istek atarken, o isteğin de kimlik doğrulamadan geçmesi gerekiyor (customer-service de JWT bekliyor). Bunun için `FeignJwtInterceptor` eklendi: gelen orijinal HTTP isteğindeki `Authorization` header'ı, giden Feign isteğine aynen kopyalanıyor (JWT relay/forward). Bu sayede kullanıcının kimliği servisler arası çağrılarda kaybolmadı.

Bu altyapı kurulduktan sonra, sipariş oluşturma akışına gerçek müşteri doğrulaması eklendi: müşteri `customer-service`'ten sorgulanıyor ve durumu aktif değilse sipariş reddediliyor.

## 14. Dağıtık İzleme (Distributed Tracing)

Mikroservisler arası bir isteğin (örn. sipariş oluşturma → müşteri sorgulama → ürün sorgulama) uçtan uca takip edilebilmesi için OpenTelemetry entegrasyonu ve Zipkin'e trace gönderimi eklendi. Bu, bir isteğin hangi serviste ne kadar sürdüğünü, nerede hata verdiğini görsel olarak incelemeyi sağlıyor.

## 15. Merkezi Loglama

Loglar, servis bazında dağınık kalmaması için Loki + Promtail + Grafana üçlüsüyle merkezi bir sisteme toplanacak şekilde yapılandırıldı. Log formatı ECS (Elastic Common Schema) standardına uygun, yapılandırılmış (structured) JSON olarak ayarlandı — bu, log arama/filtreleme işlemlerini metin taramaktan çok daha güvenilir hale getiriyor.

## 16. Veritabanı Şeması Yönetimi: Flyway

Veritabanı şeması elle veya Hibernate'in otomatik şema güncellemesine (`ddl-auto: update`) bırakılmadı; bunun yerine **Flyway** ile versiyonlanmış migration dosyaları kullanıldı (`V1__init_order_tables.sql` ile başlangıç şeması). Bu, veritabanı şemasının değişim geçmişinin kod gibi versiyonlanmasını ve farklı ortamlarda (dev/test/prod) aynı şekilde, tekrarlanabilir şekilde uygulanmasını sağlıyor.

## 17. Sertleştirme Turu: Eşzamanlılık, Debezium Kaydı ve Hata Yönetimi İyileştirmeleri

Servis olgunlaştıkça yapılan bir gözden geçirmede şu noktalar tespit edilip düzeltildi:

- **Eşzamanlı güncelleme riski**: Bir siparişin durumu, hem kullanıcının iptal işlemiyle hem de arka planda gelen bir saga event'iyle (örn. ödeme sonucu) aynı anda güncellenebiliyordu; bu durumda son yazan diğerinin değişikliğini fark etmeden üzerine yazabilirdi ("lost update"). Bunu önlemek için `Order` entity'sine **optimistic locking** (versiyon numarası) eklendi — aynı satır eşzamanlı güncellenmeye çalışılırsa, ikinci güncelleme reddedilip anlamlı bir hata dönüyor.
- **Debezium connector'larının kaydı**: Debezium Connect servisi ayakta olsa bile, hangi tabloyu izleyeceğini bilmesi için connector konfigürasyonunun ayrıca Kafka Connect'in REST API'sine kaydedilmesi gerekiyordu; bu adım daha önce elle yapılması gereken, kolay unutulan bir adımdı. Bunu otomatikleştiren bir script eklendi.
- **Eksik hata senaryoları**: Bazı iş kuralı ihlalleri (örn. müşteri aktif değil, saga kaydı eksik) genel bir Java exception'ı ile ifade ediliyordu ve bu, merkezi hata yönetimi tarafından yakalanmadığı için istemciye anlamsız bir "sunucu hatası" (500) olarak dönüyordu. Bu senaryolar için anlamlı bir HTTP durum kodu (409 Conflict) dönecek şekilde hata yönetimi genişletildi.
- **Tutarsız iptal mesajı formatı**: Kullanıcı kaynaklı iptal ile ödeme başarısızlığı kaynaklı iptalin, sipariş üzerindeki "iptal sebebi" alanını iki farklı biçimde doldurduğu görüldü; bu tutarsızlık giderildi.
- **Genel hata yakalama**: Outbox'a yazma sırasında oluşabilecek "veri serileştirilemedi" ile "veritabanına yazılamadı" hataları tek bir genel `catch` bloğunda ayrım yapılmadan ele alınıyordu; bu ikisi ayrıştırıldı, çünkü sebepleri ve olası çözümleri farklı.
- **Kullanılmayan/gereksiz kod temizliği**: Geliştirme sürecinde kalmış bir test controller'ı, gereksiz bir manuel veritabanı flush çağrısı ve hiç kullanılmayan bir zamanlanmış görev (`@EnableScheduling`) kaldırıldı — bu kod parçaları CDC'ye geçişle (bölüm 12) zaten anlamını yitirmişti.

## 18. Saga Durumunun Dışarıya Açılması (Gözlemlenebilirlik)

Bir sipariş, dış bir servisten event beklerken (örn. ödeme onayı) uzun süre "beklemede" kalabiliyor. Önceden, API üzerinden sadece siparişin genel durumu (`PENDING_PAYMENT` gibi) görülebiliyordu; siparişin saga sürecinde tam olarak hangi adımda olduğu, kaç kez yeniden denendiği veya varsa hata mesajı API'den görünmüyordu. Bu, bir sipariş takıldığında sorunun kaynağını anlamayı zorlaştırıyordu. Bu eksikliği gidermek için sipariş cevabına saga durumu bilgisi (mevcut adım, yeniden deneme sayısı, hata mesajı, son güncelleme zamanı) eklendi.

## 19. Saga Kompanzasyon Akışının Genişletilmesi

Saga durum makinesinde "kompanzasyon" (bir önceki tamamlanmış adımı geri almak) için ayrılmış bir durum vardı, ama bu durum hiçbir senaryoda fiilen kullanılmıyordu — çünkü onu tetikleyecek bir olay işlenmiyordu. Gerçek hayattaki karşılığı şu: ödeme başarıyla alındıktan sonra abonelik açma işlemi başarısız olursa, artık geri alınması gereken bir şey var (alınan ödeme). Bu senaryo için: abonelik aktivasyonu başarısız olduğunda gelen bir event işlenmeye başlandı; bu event geldiğinde sipariş "kompanzasyon sürüyor" durumuna alınıyor ve ilgili taraflara "sipariş iptal ediliyor" bilgisi yayınlanıyor. Siparişin nihai duruma (başarısız/iptal) geçişi ise, ödemenin gerçekten iade edildiğini bildiren bir event'e bağlandı — bu event henüz payment-service tarafında oluşturulmadığı için, bu son adım bilinçli olarak bir sonraki aşamaya bırakıldı.

## 20. customer-service ve product-catalog-service ile Gerçek Entegrasyon

Kardeş ekipler `customer-service` ve `product-catalog-service`'i tamamlayınca, `order-service`'in bu servislerle olan varsayımsal (mock) entegrasyonu gerçek servislerle karşılaştırılıp düzeltildi:

- Ürün fiyat bilgisinin alan adı, tarife ve ek paket (addon) kaynaklarında farklıydı; bu fark giderildi.
- Ürün kataloğunda tarifeler ve ek paketler (addon) ayrı kaynaklar olarak sunuluyor; sipariş kalemi tipine göre doğru kaynağa yönlendirme yapılacak şekilde entegrasyon güncellendi.
- Servisten "bulunamadı" cevabı geldiğinde, önceden bu durum genel bir "servis kullanılamıyor" hatasıyla karıştırılıyordu; artık "kaynak bulunamadı" ile "servis gerçekten ulaşılamıyor" durumları ayrıştırılıp doğru HTTP durum koduna bağlandı.
- Sipariş oluşturulurken artık gerçek ürün fiyatı ve adı kullanılıyor (önceden herkes için sabit bir örnek fiyat kullanılıyordu).
- Bir ürünün satışa kapalı (taslak veya emekliye ayrılmış) olma ihtimaline karşı, sipariş oluşturulmadan önce ürünün durumu da kontrol edilmeye başlandı — müşteri aktifliği kontrolüyle aynı mantık.
- Bir siparişteki farklı kalemlerin farklı para birimlerinde olabileceği ama toplamın tek bir para biriminde tutulduğu fark edildi; bu tutarsızlığı önlemek için kalemler arası para birimi uyuşmazlığı kontrol altına alındı.

## 21. VAS'ın Ek Paketin (Addon) Bir Alt Türü Olarak Ele Alınması

Sipariş kalemi tipleri arasında "VAS" (katma değerli servis) ayrı bir kategori gibi tanımlanmıştı, ama ürün kataloğunda VAS için ayrı bir kaynak yoktu. Netleştirme sonucunda VAS'ın, ek paket (addon) kaynağının bir alt türü olduğu (yani VAS ürünleri de addon olarak oluşturuluyor) anlaşıldı; entegrasyon buna göre güncellenerek VAS tipi sipariş kalemleri de doğru şekilde ek paket kaynağından sorgulanır hale getirildi.

## 22. Sayfalama (Pagination) Desteği

Sipariş listeleme ihtiyacı ortaya çıktığında (bir müşterinin tüm siparişlerini görüntüleme, ya da genel sipariş listesi), bunu sayfalama olmadan sunmak — özellikle sipariş sayısı arttıkça — performans sorunu yaratacaktı. Bu yüzden liste endpoint'i baştan sayfa numarası, sayfa boyutu ve sıralama parametrelerini dışarıdan (istek parametresi olarak) alacak şekilde tasarlandı. Aynı endpoint, bir müşteri filtresi verilirse o müşterinin siparişlerini, verilmezse tüm siparişleri (silinmiş olanlar hariç) listeliyor.

## 23. Kimlik Doğrulama Konfigürasyonunda Başlangıç Riskinin Giderilmesi

Servisin JWT doğrulama ayarlarında sadece "issuer" (Keycloak realm adresi) tanımlıydı; bu ayar tek başına kullanıldığında Spring Boot, uygulama **ayağa kalkarken** Keycloak'a bağlanıp ek bilgi (imza doğrulama anahtarlarının tam adresi) sorguluyor. Bu, uygulamanın başlaması için Keycloak'ın o an ayakta ve erişilebilir olmasını zorunlu kılıyor — geliştirme ortamında servisler elle, farklı sürelerde başlatıldığı için bu bir başlangıç hatası riski oluşturuyordu. Kardeş servislerde bu risk, imza doğrulama anahtarlarının adresinin ayrıca ve doğrudan belirtilmesiyle (böylece bu ek sorgulama adımına hiç gerek kalmadan) çözülmüştü; aynı çözüm `order-service`'e de uygulandı.

## 24. notification-service Entegrasyonu için Event'lerin Zenginleştirilmesi

`notification-service` devreye alınınca, sipariş oluşturma/onaylanma/iptal event'lerini (`OrderCreatedEvent`, `OrderConfirmedEvent`, `OrderCancelledEvent`) dinleyip müşteriye bildirim (e-posta vb.) göndermesi gerekti. Bunun için notification-service'in ayrıca `customer-service`'e geri dönüp müşteri bilgisini sorgulamasına gerek kalmasın diye, bu event'lere müşterinin e-posta adresi, adı ve soyadı da eklendi — event artık bildirim göndermek için yeterli bilgiyi kendi içinde taşıyor. Sipariş oluşturma sırasında bu bilgi zaten elde bulunan müşteri sorgusundan alınıyor; iptal ve abonelik aktivasyonu başarısızlığı senaryolarında ise event yayınlanmadan hemen önce müşteri tekrar sorgulanıp bilgi event'e ekleniyor.

## 25. Testler

Servisin farklı katmanları için birim testleri yazıldı: entity davranışları, mapper'ların doğru dönüşüm yaptığı, iş kuralı sınıflarının (rules) doğru durum geçişlerini uyguladığı, servis katmanının (`OrderServiceImpl`, `OrderEventProcessingServiceImpl`) doğru senaryoları yönettiği, exception sınıflarının doğru HTTP durum kodunu taşıdığı, Feign client'ların ve JWT interceptor'ının beklenen şekilde davrandığı, controller'ın doğru HTTP cevaplarını ürettiği ve Kafka consumer'ın event'leri doğru işlediği ayrı ayrı doğrulandı.

## 26. Bilinen Eksikler ve Gelecekte Yapılacaklar

Bu doküman yazıldığı an itibarıyla henüz tamamlanmamış veya bilinçli olarak ertelenmiş konular:

- **Yetkilendirme (yatay erişim kontrolü)**: Şu an kimliği doğrulanmış herhangi bir kullanıcı, başka bir müşteriye ait siparişi görüntüleyebilir/iptal edebilir; sipariş sahipliği kontrolü henüz eklenmedi. Bu, ekip içinde ayrıca karara bağlanacak.
- **Saga kompanzasyonunun tam kapanışı**: Abonelik aktivasyonu başarısız olduğunda sipariş "kompanzasyon sürüyor" durumuna geçiyor ama ödeme iade sürecini tamamlayan bir event henüz payment-service tarafında yayınlanmadığı için, sipariş bu ara durumda kalmaya devam edebilir.
- **payment-service ve subscription-service henüz iskelet halinde**: Bu servislerin gerçek olay şemaları henüz doğrulanamadı; `order-service`'in bu servislerden beklediği event formatları varsayımsaldır.
- **product-catalog-service'te tarife yayınlama akışının eksikliği**: Yeni oluşturulan tarifeler "taslak" durumunda kalıyor ve bunları "yayında" durumuna geçiren bir mekanizma henüz yok; bu, `order-service`'in kontrolüyle (satışa kapalı ürün reddi) kısmen telafi ediliyor ama kök neden karşı ekipte.
- **customer-service'in soft-delete'i filtrelememesi**: Silinmiş bir müşteri hâlâ sorgulanabiliyor; bu da karşı ekibe iletilmesi gereken bir konu.
- Sayfalama, sipariş listeleme ihtiyacı doğduğunda eklendi; ileride farklı filtreleme ihtiyaçları (durum, tarih aralığı gibi) doğabilir.

---

## 27. Teknoloji Stack'i

Aşağıdaki liste, `pom.xml`'deki bağımlılıklardan ve `mvn dependency:tree` ile alınan çözümlenmiş (resolved) sürümlerden oluşturulmuştur.

| Katman | Teknoloji | Sürüm |
|---|---|---|
| Dil | Java | 21 |
| Framework | Spring Boot | 4.0.6 |
| Bağımlılık yönetimi | Spring Cloud BOM | 2025.1.1 (çözümlenen artifact sürümü: 5.0.1) |
| Web | Spring Web MVC (`spring-boot-starter-webmvc`) | 4.0.6 |
| Validasyon | Spring Boot Starter Validation / Hibernate Validator | 9.0.1.Final |
| Veri erişimi (ORM) | Spring Data JPA / Hibernate ORM | 7.2.12.Final |
| Veritabanı | PostgreSQL (JDBC sürücüsü) | 42.7.10 |
| Şema versiyonlama | Flyway (`flyway-database-postgresql`) | 11.14.1 |
| Servis keşfi | Netflix Eureka Client (Spring Cloud) | 5.0.1 |
| Merkezi konfigürasyon | Spring Cloud Config Client | 5.0.1 |
| Servisler arası HTTP çağrısı | OpenFeign (`feign-core`) | 13.6 |
| Devre kesici / dayanıklılık | Resilience4j (`resilience4j-spring-boot3`) | 2.3.0 |
| AOP desteği (Resilience4j annotation'ları için) | Spring Boot Starter AspectJ | 4.0.6 |
| Mesajlaşma | Spring Cloud Stream + Kafka Binder | 5.0.1 |
| Kafka istemcisi | `spring-kafka` / `kafka-clients` | 4.0.5 / 4.1.2 |
| CDC (Change Data Capture) | Debezium Connect (Postgres connector) | 3.0 |
| Kimlik doğrulama | Spring Security OAuth2 Resource Server (JWT) | 4.0.6 |
| Kimlik sağlayıcı | Keycloak | (docker-compose imajı: `quay.io/keycloak/keycloak:26.1`) |
| Nesne dönüşümü (entity ↔ DTO) | MapStruct | 1.6.3 |
| Kod üretimi (boilerplate azaltma) | Lombok | 1.18.46 |
| JSON serileştirme | Jackson (`jackson-core`/`jackson-databind`) | 2.21.2 |
| Dağıtık izleme (tracing) | Micrometer Tracing (OpenTelemetry bridge) | 1.6.5 |
| Trace export | OpenTelemetry Zipkin Exporter | 1.55.0 |
| Trace toplama | Zipkin | (docker-compose imajı: `openzipkin/zipkin:latest`) |
| Metrikler | Micrometer Prometheus Registry | 1.16.5 |
| İzleme/health endpoint'leri | Spring Boot Actuator | 4.0.6 |
| Yapılandırılmış loglama | Logback + ECS formatı (Spring Boot structured logging) | 4.0.6 |
| Log toplama/görselleştirme | Loki + Promtail + Grafana | (docker-compose imajları: Loki 3.2.0, Promtail 3.2.0, Grafana 11.3.0) |
| Test | Spring Boot Starter Test (JUnit 5, Mockito vb.) | 4.0.6 (parent'tan miras) |
| Kod kalitesi analizi | SonarQube Scanner Maven Plugin | 4.0.0.4121 |
| Test kapsamı (coverage) aracı | JaCoCo | 0.8.12 (parent'ta versiyonu pinlenmiş, order-service'in kendi `pom.xml`'inde henüz etkinleştirilmemiş — bkz. bölüm 32) |
| Konteynerleştirme / yerel altyapı | Docker Compose | — |

## 28. Paket Yapısı

`com.telcocrm.orderservice` kök paketi altında, her paket tek bir sorumluluğa karşılık gelecek şekilde ayrıştırılmıştır:

- **(kök paket)** — `OrderServiceApplication`: Spring Boot uygulamasının giriş noktası; `@EnableFeignClients` ve `@EnableJpaAuditing` burada etkinleştiriliyor.
- **client** — `CustomerClient`, `ProductCatalogClient`: `customer-service` ve `product-catalog-service`'e senkron HTTP çağrısı yapan Feign arayüzleri; circuit breaker ve fallback mantığı da burada tanımlı.
- **client.dto** — `CustomerResponse`, `ProductResponse`: dış servislerden dönen cevapların, order-service'in ihtiyaç duyduğu alanlarla sınırlı yerel temsilleri.
- **config** — `SecurityConfig` (JWT tabanlı kimlik doğrulama kuralları), `FeignJwtInterceptor` (giden Feign isteklerine gelen JWT'yi ekleyen interceptor).
- **controller** — `OrderController`: dışarıya açılan REST endpoint'leri; iş mantığı içermez, sadece servis katmanına yönlendirir.
- **dto.request** — `CreateOrderRequest`, `OrderItemRequest`, `CancelOrderRequest`: gelen isteklerin şekli ve validasyon kuralları.
- **dto.response** — `OrderResponse`, `OrderItemResponse`, `SagaStateResponse`: dışarıya dönen cevapların şekli.
- **entity** — `Order`, `OrderItem`, `SagaState`, `OutboxEvent`, `ProcessedEvent`, `OrderAuditLog`, `IdempotencyKey`: JPA ile veritabanına eşlenen domain nesneleri.
- **entity.enums** — `OrderStatus`, `SagaStep`, `OrderItemType`: sabit durum/tip kümeleri.
- **event.publish** — `OrderCreatedEvent`, `OrderCancelledEvent`, `OrderConfirmedEvent`: outbox üzerinden Kafka'ya yayınlanan olayların şekli.
- **event.consume** — `PaymentCompletedEvent`, `PaymentFailedEvent`, `SubscriptionActivatedEvent`, `SubscriptionActivationFailedEvent`: Kafka'dan tüketilen olayların şekli.
- **exception** — `BaseException` (ortak soyut sınıf) ve ondan türeyen özel exception'lar, `GlobalExceptionHandler` (merkezi hata yakalama).
- **kafka.consumer** — `OrderEventConsumer`: Spring Cloud Stream fonksiyonel modeliyle Kafka'dan gelen event'leri ilgili servis metoduna yönlendiren `Consumer` bean'leri.
- **mapper** — `OrderMapper`, `OrderItemMapper`, `SagaStateMapper`: MapStruct ile üretilen entity ↔ DTO dönüşüm arayüzleri.
- **repository** — `OrderRepository`, `OrderItemRepository`, `OutboxRepository`, `ProcessedEventRepository`, `SagaStateRepository`, `OrderAuditLogRepository`, `IdempotencyKeyRepository`: Spring Data JPA repository arayüzleri.
- **rules** — `OrderStateRules` (sipariş/saga durum geçişi kuralları), `OrderPricingRules` (fiyatlandırma/sipariş kalemi oluşturma mantığı).
- **service** / **service.impl** — `OrderService`/`OrderServiceImpl` (sipariş orkestrasyonu), `OrderEventProcessingService`/`OrderEventProcessingServiceImpl` (gelen Kafka event'lerinin işlenmesi), `OutboxService` (outbox tablosuna yazma), `OrderAuditService` (denetim/geçmiş kaydı yazma).

## 29. API Referansı

Tüm endpoint'ler `/api/v1/orders` altında toplanmıştır ve (actuator/swagger hariç) geçerli bir JWT (`Authorization: Bearer <token>`) gerektirir. Hata cevapları RFC 7807 `ProblemDetail` formatındadır: `{ "type", "title", "status", "detail", "errorCode", "timestamp" }`.

### POST /api/v1/orders — Sipariş oluşturma

**Header:** `Idempotency-Key` (opsiyonel) — verilirse ve daha önce aynı key ile bir istek işlendiyse, yeni bir sipariş oluşturulmaz; o isteğin sonucunda oluşan sipariş aynen geri dönülür (bkz. bölüm 34).

**Request body** (`CreateOrderRequest`):
```json
{
  "customerId": "b3f1c2a0-....-....-............",
  "items": [
    {
      "productCode": "TARIFF-100",
      "productType": "TARIFF",
      "quantity": 1
    }
  ]
}
```
veya, müşterinin UUID'si değil de insan-dostu numarası biliniyorsa:
```json
{
  "customerNo": "C-000123",
  "items": [
    {
      "productCode": "TARIFF-100",
      "productType": "TARIFF",
      "quantity": 1
    }
  ]
}
```
- `customerId`: **opsiyonel**, UUID. Doğrudan biliniyorsa kullanılır (`customer-service`'ten `GET /api/v1/customers/{id}` ile doğrulanır).
- `customerNo`: **opsiyonel**, String, `C-XXXXXX` formatında (bkz. bölüm 35). `customerId` verilmediyse kullanılır; `customer-service`'ten `GET /api/v1/customers/byNo/{customerNo}` ile gerçek `customerId`'ye çözümlenir.
- `customerId` ve `customerNo`'dan **en az biri verilmelidir** — ikisi de eksikse `400 Bad Request` (`INVALID_REQUEST`) döner. İkisi birden verilirse `customerId` önceliklidir, `customerNo` dikkate alınmaz.
- `items`: zorunlu, en az 1 eleman.
  - `productCode`: zorunlu, boş olamaz.
  - `productType`: zorunlu, `TARIFF` | `ADDON` | `VAS`.
  - `quantity`: zorunlu, pozitif tam sayı.

**Response body** (201 Created, `Location: /api/v1/orders/{id}`): `OrderResponse` (bkz. aşağıdaki ortak şema).

**Olası durum kodları:**
| Kod | Durum |
|---|---|
| 201 | Sipariş oluşturuldu |
| 400 | Validasyon hatası (`@Valid` ihlali), bozuk JSON gövdesi, veya `customerId`/`customerNo` alanlarının ikisi de eksik |
| 404 | Müşteri veya sipariş kalemindeki ürün kodu bulunamadı |
| 409 | Müşteri ya da ürün aktif değil, sipariş kalemlerinde para birimi uyuşmuyor, veya aynı `Idempotency-Key` ile eşzamanlı bir istek yarışı kaybedildi |
| 503 | `customer-service` veya `product-catalog-service`'e ulaşılamıyor (circuit breaker devrede) |

### GET /api/v1/orders — Sipariş listeleme (sayfalı)

**Query parametreleri:**
| Parametre | Zorunlu mu | Varsayılan | Açıklama |
|---|---|---|---|
| `customerId` | Hayır | — | Verilirse yalnızca o müşterinin siparişleri listelenir; verilmezse tüm siparişler (silinmiş olanlar hariç) listelenir |
| `page` | Hayır | `0` | Sayfa numarası |
| `size` | Hayır | `20` | Sayfa başına kayıt sayısı |
| `sort` | Hayır | `createdAt,desc` | Sıralama alanı ve yönü |

**Response body** (200 OK): Spring Data `Page<OrderResponse>` — `content` (OrderResponse listesi), `totalElements`, `totalPages`, `number`, `size`, `first`, `last` gibi standart sayfalama alanlarını içerir.

### GET /api/v1/orders/{orderId} — Tekil sipariş getirme

**Path parametresi:** `orderId` (UUID).

**Response body** (200 OK): `OrderResponse`.

**Olası durum kodları:** 200, 404 (sipariş bulunamadı veya silinmiş).

### POST /api/v1/orders/{orderId}/cancel — Sipariş iptali

**Path parametresi:** `orderId` (UUID).

**Request body** (`CancelOrderRequest`):
```json
{
  "reason": "Müşteri fikrini değiştirdi"
}
```
- `reason`: zorunlu, boş olamaz.

**Response body** (200 OK): `OrderResponse`.

**Olası durum kodları:**
| Kod | Durum |
|---|---|
| 200 | Sipariş iptal edildi |
| 400 | Validasyon hatası |
| 404 | Sipariş bulunamadı |
| 422 | Sipariş şu anki durumda iptal edilemez (yalnızca `PENDING_PAYMENT` durumundaki siparişler kullanıcı tarafından iptal edilebilir) |
| 409 | Eşzamanlı güncelleme çakışması (optimistic lock) |

### Ortak response şeması: `OrderResponse`

```json
{
  "id": "uuid",
  "customerId": "uuid",
  "status": "DRAFT | PENDING_PAYMENT | PAID | FULFILLED | CANCELLED",
  "totalAmount": 199.90,
  "currency": "TRY",
  "paymentId": "uuid veya null",
  "subscriptionId": "uuid veya null",
  "cancellationReason": "string veya null",
  "items": [
    {
      "id": "uuid",
      "productCode": "string",
      "productName": "string",
      "productType": "TARIFF | ADDON | VAS",
      "quantity": 1,
      "unitPrice": 99.90,
      "lineTotal": 99.90
    }
  ],
  "sagaState": {
    "currentStep": "ORDER_CREATED | AWAITING_PAYMENT | AWAITING_SUBSCRIPTION | COMPLETED | COMPENSATING | FAILED",
    "retryCount": 0,
    "errorMessage": "string veya null",
    "lastUpdated": "2026-07-07T10:00:00"
  },
  "createdAt": "2026-07-07T10:00:00",
  "updatedAt": "2026-07-07T10:00:00"
}
```

## 30. Kafka Topics

`order-service`, olayları veritabanına yazıp Debezium CDC ile Kafka'ya yayınlar (outbox pattern — bkz. bölüm 10 ve 12); gelen olayları ise doğrudan Spring Cloud Stream Kafka binder'ı ile tüketir.

### Yayınlanan (publish) topic'ler

Outbox tablosundaki `topic` alanına yazılan değer, Debezium'un `EventRouter` transform'u tarafından gerçek Kafka topic adı olarak kullanılır.

| Topic | Event | Ne zaman yayınlanır | Alanlar |
|---|---|---|---|
| `order-created-topic` | `OrderCreatedEvent` | Sipariş başarıyla oluşturulduğunda | `eventId`, `occurredAt`, `orderId`, `customerId`, `totalAmount`, `currency`, `email`, `firstName`, `lastName` |
| `order-cancelled-topic` | `OrderCancelledEvent` | Üç senaryodan biri gerçekleştiğinde: (1) kullanıcı siparişi iptal ettiğinde, (2) `PaymentFailedEvent` işlendiğinde, (3) `SubscriptionActivationFailedEvent` işlendiğinde (kompanzasyon) | `eventId`, `occurredAt`, `orderId`, `customerId`, `cancellationReason`, `email`, `firstName`, `lastName` |
| `order-confirmed-topic` | `OrderConfirmedEvent` | `SubscriptionActivatedEvent` başarıyla işlendiğinde (sipariş `FULFILLED` olduğunda) | `eventId`, `occurredAt`, `orderId`, `customerId`, `subscriptionId`, `email`, `firstName`, `lastName` |

`email`, `firstName`, `lastName` alanları, `notification-service`'in ayrıca `customer-service`'e sorgu atmasına gerek kalmadan bildirim gönderebilmesi için event'e gömülür (bkz. bölüm 24).

### Tüketilen (consume) topic'ler

`configs/order-service/application-dev.yml` içindeki `spring.cloud.stream.bindings` altında tanımlıdır; her biri `OrderEventConsumer`'daki aynı isimli bir `Consumer<T>` bean'ine bağlanır.

| Topic | Binding adı | Event | Alanlar | İşleyen metod |
|---|---|---|---|---|
| `payment-completed-topic` | `paymentCompletedEvent-in-0` | `PaymentCompletedEvent` | `eventId`, `occurredAt`, `orderId`, `paymentId` | `processPaymentCompleted` — sipariş `PAID`'e geçer, saga `AWAITING_SUBSCRIPTION`'a geçer |
| `payment-failed-topic` | `paymentFailedEvent-in-0` | `PaymentFailedEvent` | `eventId`, `occurredAt`, `orderId`, `reason` | `processPaymentFailed` — sipariş `CANCELLED`'a geçer, `order-cancelled-topic`'e event yayınlanır |
| `subscription-activated-topic` | `subscriptionActivatedEvent-in-0` | `SubscriptionActivatedEvent` | `eventId`, `occurredAt`, `orderId`, `subscriptionId` | `processSubscriptionActivated` — sipariş `FULFILLED`'a geçer, `order-confirmed-topic`'e event yayınlanır |
| `subscription-activation-failed-topic` | `subscriptionActivationFailedEvent-in-0` | `SubscriptionActivationFailedEvent` | `eventId`, `occurredAt`, `orderId`, `reason` | `processSubscriptionActivationFailed` — saga `COMPENSATING`'e geçer, `order-cancelled-topic`'e event yayınlanır |

Her tüketici, aynı event'in tekrar işlenmesini önlemek için önce `ProcessedEvent` tablosunda `eventId`'nin daha önce işlenip işlenmediğini kontrol eder (inbox/idempotency pattern, bkz. bölüm 11).

> Not: `PaymentCompletedEvent`/`PaymentFailedEvent`/`SubscriptionActivatedEvent`/`SubscriptionActivationFailedEvent` şemaları, `payment-service` ve `subscription-service` henüz tam olarak geliştirilmediği için şu an varsayımsaldır (bkz. bölüm 26).

## 31. Ortam Kurulumu

Servisin yerelde ayağa kaldırılması için önerilen sıra:

1. **Altyapı konteynerlerini başlat**: repo kökünde `docker/docker-compose.yml` ile `docker compose up -d` çalıştırılır. Bu, şunları ayağa kaldırır: `order-db` (Postgres, port 5404), `kafka` (port 9092), `debezium-connect` (port 8083), `keycloak` (port 8085 — realm, `keyclock/realm-export.json` dosyasından otomatik import edilir), `zipkin` (port 9411), `loki`/`promtail`/`grafana` (port 3100/—/3000), ayrıca diğer servislerin kendi veritabanları.
2. **discovery-server'ı başlat** (Eureka, port 8761) — `order-service` kendini burada kaydeder ve `customer-service`/`product-catalog-service`'i burada bulur.
3. **config-server'ı başlat** (port 8888, `native` profil ile `configs/` klasörünü servis eder) — `order-service`'in `application.yml`'i `spring.config.import: "configserver:http://localhost:8888"` ile bu servere **zorunlu** bir bağımlılık taşır; config-server ayakta değilse `order-service` başlamaz.
4. **order-service'i başlat** (`dev` profili aktif; `configs/order-service/application-dev.yml`'den veritabanı, Keycloak, Kafka ve resilience4j ayarlarını devralır).
5. **Debezium connector'larını kaydet**: `debezium-connect` konteyneri ayaktayken `docker/register-connectors.sh` çalıştırılır. Bu adım atlanırsa `outbox` tablosuna yazılan event'ler hiçbir zaman Kafka'ya taşınmaz (bkz. bölüm 12 ve 17).

Dikkat edilmesi gerekenler:
- `order-service`, docker-compose içinde bir konteyner olarak tanımlı **değildir**; geliştirme ortamında host üzerinde (IDE/`mvn spring-boot:run`) çalıştırılır.
- Keycloak'ın `order-service`'ten önce tam olarak ayakta olması artık zorunlu değildir (bkz. bölüm 23 — `jwk-set-uri` sayesinde ilk JWT doğrulaması gelene kadar bağlantı ertelenir), ama yine de önerilir.
- `register-connectors.sh`, aynı connector zaten kayıtlıysa tekrar kaydetmeye çalışmaz (idempotent).

## 32. Test ve Kod Kalitesi

**Testleri çalıştırma:**
```
cd order-service
mvn test
```
Not: `OrderServiceApplicationTests` gibi `@SpringBootTest` ile işaretli testler, `dev` profilindeki gerçek Postgres bağlantısını kullanır; bu yüzden testleri çalıştırmadan önce en azından `order-db`'nin (ve bağlıysa config-server'ın) ayakta olması gerekir.

Mevcut test kapsamı şu katmanları içerir: entity davranışları, MapStruct mapper'ları, iş kuralları (`rules`), servis katmanı (`OrderServiceImpl`, `OrderEventProcessingServiceImpl`), `OutboxService`, exception sınıfları ve `GlobalExceptionHandler`, Feign client'lar (`CustomerClientTest`, `ProductCatalogClientTest`), `FeignJwtInterceptor`, `OrderController` ve `OrderEventConsumer`.

**SonarQube analizi:**
Kod kalitesi analizi, kök `pom.xml`'de tanımlı `sonar-maven-plugin` (4.0.0.4121) ile tüm modülleri **tek bir Sonar projesi** (`sonar.projectKey=telco-crm`) altında topluyor. Sunucu adresi `sonar.host.url=http://localhost:9001` olarak ayarlı. Analiz repo kökünden şu şekilde çalıştırılır:
```
mvn sonar:sonar
```
(Sonar sunucusu kimlik doğrulaması istiyorsa `-Dsonar.login=<token>` parametresi eklenir.)

`sonar.exclusions` ile `entity`, `dto`, `config`, `enums` paketleri ve `*Application.java` sınıfları analiz dışı bırakılıyor.

**Önemli eksik**: Kök `pom.xml`, kapsam (coverage) verisinin `**/target/site/jacoco/jacoco.xml` yolundan okunmasını bekliyor (`sonar.coverage.jacoco.xmlReportPaths`) ve JaCoCo plugin'inin sürümünü (`0.8.12`) `pluginManagement` içinde pinliyor. Ancak bu plugin, **`order-service`'in kendi `pom.xml`'inde etkinleştirilmemiş** (bazı diğer modüllerde — örn. `customer-service`, `notification-service` — etkin). Bu nedenle `mvn test` çalıştırıldığında `order-service` için bir JaCoCo raporu üretilmiyor ve Sonar analizi bu modül için kod kapsamı verisi görmüyor. Coverage verisinin Sonar'a yansıması için `jacoco-maven-plugin`'in (`prepare-agent` ve `report` execution'larıyla) `order-service/pom.xml`'e de eklenmesi gerekiyor.

## 33. Audit Log Mekanizması

Önceden servis, `@CreatedDate`/`@LastModifiedDate` ile sadece bir kaydın en son ne zaman oluşturulup güncellendiğini tutuyordu; siparişin geçmişte hangi durumlardan geçtiğine, saga sürecinin hangi adımlarından geçtiğine veya neden değiştiğine dair kalıcı bir kayıt yoktu — `SagaState` de bu bilgiyi tutuyordu ama üzerine yazılan (mutable), tek satırlık, anlık bir durum olarak.

Bu eksikliği gidermek için, mevcut mimariyle aynı desende, **immutable (yalnızca ekleme yapılan) bir denetim kaydı** mekanizması eklendi:

- **`OrderAuditLog` entity'si**: her satır bir siparişin (`orderId`) belirli bir anda hangi sipariş durumunda (`orderStatus`) ve hangi saga adımında (`sagaStep`) olduğunu, bunun kısa bir açıklamasıyla (`detail`), bunu kimin/neyin tetiklediğiyle (`performedBy`) ve zaman damgasıyla (`createdAt`, Spring Data JPA Auditing ile otomatik dolduruluyor) birlikte kalıcı olarak saklıyor. Diğer append-only kayıtlarda (`OutboxEvent`) olduğu gibi, `Order` ile JPA ilişkisi (`@ManyToOne`) kurulmadı — sadece `orderId` düz bir alan olarak tutuluyor; böylece denetim kaydı, sipariş nesnesinin yaşam döngüsünden (cascade, lazy loading vb.) tamamen bağımsız kalıyor.
- **`OrderAuditLogRepository`**: siparişin tüm geçmişini kronolojik sırayla getiren bir sorgu metodu içeriyor.
- **`OrderAuditService`**: `OutboxService` ile birebir aynı yapıda (`@Service`, tek repository bağımlılığı) yazıldı; tek sorumluluğu, verilen sipariş nesnesinin o anki durumunu bir denetim satırı olarak kaydetmek.
- **`performedBy` — "kim yaptı" bilgisi**: `OrderAuditService`, her kayıt öncesinde `SecurityContextHolder`'daki mevcut `Authentication`'ı kontrol ediyor. İstek bir HTTP çağrısı üzerinden (JWT ile kimliği doğrulanmış bir kullanıcı tarafından) geldiyse `performedBy` alanına JWT'nin `sub` claim'i (`Authentication.getName()`) yazılıyor — bu, `createOrder` ve `cancelOrder` senaryoları için geçerli. İstek bir Kafka event'inin işlenmesinden (ödeme/abonelik sonucu) geldiyse ortada bir HTTP isteği/JWT olmadığı için `SecurityContextHolder` boş kalıyor; bu durumda `performedBy` sabit olarak `"SYSTEM"` değerini alıyor. Bu ayrım, çağıran servis sınıflarına (`OrderServiceImpl`, `OrderEventProcessingServiceImpl`) hiçbir ek parametre veya kod eklemeden, tamamen `OrderAuditService`'in içinde, ortam bağlamına (context) bakılarak otomatik yapılıyor.
- **Entegrasyon noktaları**: `OrderStateRules`'a dokunulmadı (bu sınıf hâlâ saf karar mantığı olarak kalıyor, veritabanına hiç erişmiyor). Bunun yerine, servis katmanında (`OrderServiceImpl`, `OrderEventProcessingServiceImpl`) her durum değişikliğinden hemen sonra — tam olarak `outboxService.saveEvent(...)` çağrısının yapıldığı noktalarla aynı yerlerde — `orderAuditService.log(...)` çağrısı eklendi: sipariş oluşturulduğunda, kullanıcı tarafından iptal edildiğinde, ödeme tamamlandığında, ödeme başarısız olduğunda, abonelik aktive olduğunda ve abonelik aktivasyonu başarısız olup saga kompanzasyona geçtiğinde.
- **Veritabanı şeması**: `V3__add_order_audit_logs.sql` migration'ı ile `order_audit_logs` tablosu, `performed_by` kolonu ve `order_id` üzerinde bir index eklendi.

Bu sayede artık bir sipariş için "hangi tarihte hangi durumdaydı, saga hangi adımdaydı, neden değişti, bunu kim/ne tetikledi" sorusu, `SagaState`'in o anki tek satırlık haline değil, `order_audit_logs` tablosundaki tam geçmişe bakılarak cevaplanabiliyor. Şu an bu geçmişi dışarıya açan bir API endpoint'i eklenmedi; mekanizma yalnızca veriyi kalıcı olarak biriktiriyor.

### Doğrulama: Testler

Mekanizma eklendikten sonra tüm test paketi çalıştırılarak doğrulandı, bu sırada iki gerçek sorun ortaya çıktı ve düzeltildi:

- **Mevcut testlerde regresyon**: `OrderServiceImpl` ve `OrderEventProcessingServiceImpl`'e yeni bir bağımlılık (`OrderAuditService`) eklenince, bu sınıfları mock'layan `OrderServiceImplTest` ve `OrderEventProcessingServiceImplTest`, yeni alan için mock tanımlamadıkları için `NullPointerException` ile başarısız olmaya başladı. Her iki test sınıfına da `@Mock private OrderAuditService orderAuditService;` eklenerek düzeltildi.
- **`OrderAuditService` için yeni birim testleri** (`OrderAuditServiceTest`) yazıldı: `performedBy` alanının kimliği doğrulanmış bir kullanıcı varken JWT `sub` claim'inden doğru dolduğu, hem hiç `Authentication` yokken (Kafka event senaryosu) hem de anonim bir `Authentication` varken `"SYSTEM"`'e düştüğü ayrı ayrı doğrulandı.
- **İlgisiz ama bu sırada fark edilen bir kırık test**: `ProductCatalogClientTest.shouldThrowForUnsupportedVasType`, VAS'ın addon'a yönlendirilmesinden önceki eski davranışı (VAS için exception bekleme) test ediyordu ve o değişiklik yapıldığında testler çalıştırılmadığı için fark edilmemişti. `shouldDispatchToAddonLookupForVasType` olarak, VAS'ın artık doğru şekilde addon lookup'ına yönlendirildiğini doğrulayacak şekilde güncellendi.

Bu düzeltmelerden sonra tüm paket çalıştırıldı: 105 test, 0 başarısızlık, yalnızca gerçek bir config-server/Postgres bağlantısı isteyen `OrderServiceApplicationTests.contextLoads` altyapı ayakta olmadığı için hata veriyor (bkz. bölüm 32).

## 34. Idempotency-Key Desteği

Sipariş oluşturma endpoint'i (`POST /api/v1/orders`) tek gerçek "yan etkili yazma" işlemiydi: bir client isteği gönderdikten sonra network timeout'u nedeniyle yanıtı alamayıp isteği tekrar gönderirse, aynı sipariş **iki kez** oluşturulabiliyordu (sipariş iptali bu riski taşımıyor, çünkü ikinci çağrı zaten "durum uygun değil" hatasıyla doğal olarak engelleniyor). Bunu önlemek için, Kafka event'leri için zaten kullanılan **inbox pattern**'in (`ProcessedEvent`) aynısı HTTP yazma isteklerine taşındı:

- **`IdempotencyKey` entity'si**: `key` (client'ın `Idempotency-Key` header'ıyla gönderdiği değer, veritabanı seviyesinde **unique**), `orderId` (bu isteğin sonucunda oluşan sipariş), `createdAt`.
- **`OrderController.createOrder`**: `@RequestHeader(value = "Idempotency-Key", required = false)` ile opsiyonel bir header kabul ediyor — header gönderilmezse davranış tamamen eskisi gibi kalıyor, mevcut client'lar etkilenmiyor.
- **`OrderServiceImpl.createOrder`** akışı:
  1. Key verilmişse, `IdempotencyKeyRepository.findByKey(...)` ile daha önce görülüp görülmediği kontrol edilir. Görülmüşse, müşteri/ürün doğrulaması gibi hiçbir iş mantığı **hiç çalıştırılmadan** doğrudan o isteğin sonucunda oluşan sipariş getirilip aynı response geri dönülür (gerçek bir "replay").
  2. Görülmemişse, sipariş normal şekilde oluşturulur; `orderRepository.save(order)`'dan hemen sonra, **aynı transaction içinde**, `IdempotencyKey(key, orderId)` kaydedilir.
- **Eşzamanlılık**: `key` kolonundaki unique constraint sayesinde, iki eşzamanlı istek aynı anda "daha önce görülmedi" sonucunu alıp ikisi de sipariş oluşturmaya çalışırsa, ikinci isteğin `IdempotencyKey` kaydı unique constraint ihlaliyle başarısız olur (`DataIntegrityViolationException`). Bu, yeni bir `DuplicateRequestException` (409 Conflict, `BaseException` alt sınıfı — mevcut `GlobalExceptionHandler` altyapısına ek bir kod gerekmeden otomatik yakalanıyor) olarak dönüştürülüp fırlatılır; `@Transactional` sayesinde o isteğin oluşturduğu sipariş de dahil tüm işlem geri alınır (rollback). Client aynı key ile tekrar denediğinde, artık "daha önce görülmedi" değil "görüldü" yoluna girip kazanan isteğin sonucunu alır.
- **Yeni migration**: `V4__add_idempotency_keys.sql` — `idempotency_keys` tablosu ve `key` üzerinde unique constraint.
- **Testler**: `OrderServiceImplTest`'e üç yeni test eklendi — key daha önce görüldüğünde replay yapıldığının, key ilk kez görüldüğünde doğru şekilde kaydedildiğinin, ve eşzamanlılık çakışmasında `DuplicateRequestException` fırlatıldığının doğrulanması. `OrderControllerTest`'teki `createOrder` mock'ları da yeni imzaya (`createOrder(request, idempotencyKey)`) göre güncellendi.

Bu mekanizma bilinçli olarak yalnızca `createOrder`'a eklendi; `cancelOrder` zaten doğal idempotency'ye sahip olduğu için kapsam dışı bırakıldı.

## 35. `customerNo` ile Sipariş Oluşturma

Frontend'in sipariş sihirbazı (`OrderWizard`), operasyonel akışta müşteri temsilcisinin elinde genelde müşterinin dahili UUID'si değil, ekranda gösterilen insan-dostu müşteri numarası (`customer-service`'in ürettiği `C-XXXXXX` formatındaki `customerNo`, bkz. bölüm 20) bulunduğu için, sipariş oluşturma isteğine UUID yerine bu numarayı gönderecek şekilde tasarlandı. `order-service`'in bunu karşılayabilmesi için `CreateOrderRequest.customerId` alanı **zorunlu olmaktan çıkarıldı** ve yanına opsiyonel bir `customerNo` alanı eklendi:

- `customerId` verilmişse doğrudan kullanılır (`customer-service`'ten `GET /api/v1/customers/{id}` ile doğrulanır) — mevcut/programatik client'ların davranışı değişmedi.
- `customerId` verilmemiş ama `customerNo` verilmişse, `customer-service`'ten `GET /api/v1/customers/byNo/{customerNo}` ile gerçek `customerId`'ye çözümlenir.
- İkisi de verilmemişse `IllegalArgumentException` fırlatılır, `GlobalExceptionHandler` bunu `400 Bad Request` (`INVALID_REQUEST`) olarak döner.

Bu değişiklik yapılırken bölüm 29'daki API referansı da güncellendi (o zamana kadar dokümanda hâlâ "customerId zorunlu" yazıyordu, kod ilerlemiş ama doküman güncellenmemişti).
