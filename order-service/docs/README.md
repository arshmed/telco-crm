# order-service

## Ne İşe Yarar?

Telco-crm'de sipariş yaşam döngüsünü yöneten servis. Bir müşteri sipariş oluşturduğunda müşteri/ürün doğrulamasını yapar, siparişi kaydeder ve ödeme/abonelik aktivasyonu gibi başka servislerde gerçekleşen adımları bir saga üzerinden takip edip sonuçta siparişi tamamlar ya da iptal eder.

## Nasıl Çalışır?

- Sipariş oluşturulurken customer-service ve product-catalog-service'e senkron (Feign) çağrı yapılır — müşteri/ürün aktif mi kontrol edilir, gerçek fiyat ve isim çekilir.
- Sipariş `PENDING_PAYMENT` durumunda kaydedilir; `OrderCreatedEvent` outbox tablosuna yazılıp Debezium ile Kafka'ya taşınır.
- payment-service bu event'i otomatik tüketip ödeme başlatabilir; alternatif olarak frontend kullanıcıdan kart bilgisi alıp doğrudan payment-service'e manuel ödeme isteği de gönderebilir.
- payment-service'in yayınladığı `PaymentCompletedEvent`/`PaymentFailedEvent` tüketilip sipariş `PAID`/`CANCELLED`'a taşınır.
- Abonelik aktivasyonu (subscription-service, henüz yazılmadı) başarısız olursa saga `COMPENSATING`'e geçer, ödeme iade edildiğinde (`PaymentRefundedEvent`) sipariş `FAILED`'a taşınır.
- Sipariş cevabında saga durumu (`currentStep`, `retryCount`, `errorMessage`) her zaman görüntülenebilir durumdadır.

## Ana Özellikler

- Idempotency-Key desteği — aynı sipariş oluşturma isteğinin iki kez işlenmesini önler.
- Saga durum takibi — sürecin hangi adımda olduğu, kaç kez denendiği API'den görülebilir.
- Circuit breaker — customer-service/product-catalog-service'e ulaşılamadığında anlamlı bir 503 döner.
- Audit log — bir siparişin geçmişteki tüm durum/saga geçişleri kalıcı olarak saklanır.
- Optimistic locking — eşzamanlı güncellemelerde veri kaybını (lost update) önler.
- Sayfalı sipariş listeleme — müşteri bazlı filtreleme destekli.

## Teknoloji Stack'i

Spring Boot, Spring Cloud (Eureka, Config Server, OpenFeign, Stream Kafka), PostgreSQL, Flyway, Resilience4j, Debezium, Keycloak, Zipkin, Micrometer/OpenTelemetry, Loki + Promtail + Grafana, MapStruct

## API'ler

| Method | Path | Açıklama |
|---|---|---|
| POST | `/api/v1/orders` | Sipariş oluşturur (`Idempotency-Key` header'ı opsiyonel destekler) |
| GET | `/api/v1/orders` | Siparişleri sayfalı listeler (`customerId` ile filtrelenebilir) |
| GET | `/api/v1/orders/{id}` | Tekil sipariş detayını (saga durumu dahil) getirir |
| POST | `/api/v1/orders/{id}/cancel` | Yalnızca `PENDING_PAYMENT` durumundaki bir siparişi iptal eder |

## Kafka Event'leri

**Yayınladıkları:**
- `OrderCreatedEvent` — sipariş başarıyla oluşturulduğunda
- `OrderCancelledEvent` — kullanıcı iptali, ödeme başarısızlığı veya saga kompanzasyonu sonucunda
- `OrderConfirmedEvent` — abonelik aktivasyonu başarılı olup sipariş `FULFILLED` olduğunda

**Dinledikleri:**
- `PaymentCompletedEvent` — sipariş `PAID`'e geçer
- `PaymentFailedEvent` — sipariş `CANCELLED`'a geçer
- `PaymentRefundedEvent` — saga kompanzasyonu `FAILED`'a taşınır
- `SubscriptionActivatedEvent` — sipariş `FULFILLED`'a geçer
- `SubscriptionActivationFailedEvent` — saga `COMPENSATING`'e geçer

## Nasıl Çalıştırılır?

1. Repo kökünde `docker/docker-compose.yml` ile `docker compose up -d` çalıştırılır (Postgres, Kafka, Debezium Connect, Keycloak, Zipkin, Loki/Grafana dahil).
2. discovery-server başlatılır (Eureka, port 8761).
3. config-server başlatılır (port 8888) — order-service'in başlaması için zorunlu.
4. `docker/register-connectors.sh` ile Debezium connector'ları kaydedilir (atlanırsa outbox event'leri Kafka'ya hiç taşınmaz).
5. order-service host üzerinde `dev` profiliyle başlatılır (port 9004).

## Bilinen Sınırlamalar

- Yetkilendirme (yatay erişim kontrolü) yok — kimliği doğrulanmış herhangi bir kullanıcı başka bir müşterinin siparişini görüntüleyebilir/iptal edebilir.
- `FULFILLED` durumu ve `order-confirmed-topic` fiilen hiç tetiklenmiyor — subscription-service henüz yazılmadığı için.
- `PaymentCompletedEvent`/`PaymentFailedEvent`/`SubscriptionActivatedEvent`/`SubscriptionActivationFailedEvent` şemaları, karşı servisler tam doğrulanamadığı için kısmen varsayımsal.
- customer-service'in soft-delete'i filtrelememesi (silinmiş müşteri hâlâ sorgulanabiliyor) — order-service'in kontrolü dışında, bilgi amaçlı not.

## Detaylı Dokümantasyon

Geliştirme sürecinin detaylı anlatımı ve tam API referansı için: [ORDER_SERVICE_DEVELOPMENT.md](./ORDER_SERVICE_DEVELOPMENT.md)
