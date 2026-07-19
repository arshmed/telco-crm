# payment-service

## Ne İşe Yarar?

Telco-crm'de ödeme tahsilatını, başarısız ödemelerin yeniden denenmesini ve iadeleri yöneten servis. order-service'in yayınladığı sipariş event'lerini dinleyip ödemeyi otomatik başlatabilir; frontend'den kullanıcının girdiği kart bilgisiyle manuel olarak da tetiklenebilir.

## Nasıl Çalışır?

- order-service bir sipariş oluşturduğunda `OrderCreatedEvent` yayınlar; payment-service bunu tüketip otomatik (kart bilgisi olmadan) bir ödeme dener.
- Frontend, kullanıcıdan kart bilgisi alıp `POST /api/v1/payments` ile manuel ödeme de tetikleyebilir — iki akış aynı sipariş için eşzamanlı çalışabildiğinden, bir siparişin zaten ödemesi varsa otomatik akışın ikinci bir kayıt oluşturmasını engelleyen bir kontrol var.
- Mock bir PSP (gerçek ödeme sağlayıcısı entegrasyonu yok), ödeme yöntemine göre farklı başarı oranı ve gecikmeyle sonucu simüle eder.
- Başarısız ödemeler zamanlanmış bir görevle 24, 72 ve 168 saat sonra sırayla tekrar denenir; dördüncü denemede de başarısız olursa kalıcı olarak `FAILED` işaretlenir.
- Sonuç (`PaymentCompletedEvent`/`PaymentFailedEvent`) outbox tablosuna yazılıp Debezium ile Kafka'ya taşınır; order-service bunu tüketip saga'yı ilerletir.
- Abonelik aktivasyonu başarısız olursa (order-service'ten gelen kompanzasyon sinyaliyle) ödeme otomatik iade edilir ve `PaymentRefundedEvent` yayınlanır.

## Ana Özellikler

- Idempotency desteği — `paymentRequestId` ile aynı isteğin iki kez işlenmesini önler.
- Retry mekanizması — başarısız ödemeleri 24/72/168 saat sonra sırayla tekrar dener.
- Otomatik/manuel akış çakışma koruması — bir siparişe mükerrer ödeme kaydı oluşmasını engeller.
- Yöntem bazlı mock PSP davranışı — kredi kartı, banka havalesi ve cüzdan için farklı başarı oranı ve gecikme profili.
- Luhn/son kullanma tarihi doğrulaması — geçersiz kart bilgisiyle gereksiz PSP çağrısı yapılmaz.
- Audit log — bir ödemenin geçmişteki tüm durum geçişleri kalıcı olarak saklanır.
- Saga kompanzasyonu — abonelik aktivasyonu başarısız olursa ödeme otomatik iade edilir.

## Teknoloji Stack'i

Spring Boot, Spring Cloud (Eureka, Config Server, OpenFeign, Stream Kafka), PostgreSQL, Flyway, Resilience4j, Debezium, Keycloak, Zipkin, Micrometer/OpenTelemetry, Loki + Promtail + Grafana, MapStruct

## API'ler

| Method | Path | Açıklama |
|---|---|---|
| POST | `/api/v1/payments` | Kart bilgisiyle ödeme oluşturur (`paymentRequestId` idempotency anahtarıdır) |
| GET | `/api/v1/payments` | Ödemeleri sayfalı listeler |
| GET | `/api/v1/payments/{id}` | Tekil ödeme detayını (deneme geçmişi dahil) getirir |
| POST | `/api/v1/payments/{id}/refund` | Tamamlanmış bir ödemeyi iade eder |

## Kafka Event'leri

**Yayınladıkları:**
- `PaymentCompletedEvent` — bir ödeme (ilk deneme veya retry) başarıyla tamamlandığında
- `PaymentFailedEvent` — retry mekanizması tüm denemeleri tükettiğinde
- `PaymentRefundedEvent` — manuel iade veya saga kompanzasyonu sonucunda

**Dinledikleri:**
- `OrderCreatedEvent` — otomatik ödeme akışını başlatır
- `SubscriptionActivationFailedEvent` — otomatik iadeyi tetikler

## Nasıl Çalıştırılır?

1. Repo kökünde `docker/docker-compose.yml` ile `docker compose up -d` çalıştırılır (Postgres, Kafka, Debezium Connect, Keycloak, Zipkin, Loki/Grafana dahil).
2. discovery-server başlatılır (Eureka, port 8761).
3. config-server başlatılır (port 8888) — payment-service'in başlaması için zorunlu.
4. `docker/register-connectors.sh` ile Debezium connector'ları kaydedilir (atlanırsa outbox event'leri Kafka'ya hiç taşınmaz).
5. payment-service host üzerinde `dev` profiliyle başlatılır (port 9008).

## Bilinen Sınırlamalar

- Gerçek bir PSP entegrasyonu yok — tüm ödeme sonuçları mock bir client tarafından simüle ediliyor.
- `SubscriptionActivationFailedEvent`'in şeması varsayımsal — subscription-service henüz yazılmadı.
- `Payment` entity'sindeki `invoiceId` alanı şimdilik hep boş — henüz hiçbir akışta kullanılmıyor, ileride fatura/abonelik bazlı otomatik ödeme senaryosu (order-service dışında bir tetikleyici) eklenmek istendiğinde kullanılmak üzere şemaya hazırlık amaçlı eklendi.
- Yetkilendirme (yatay erişim kontrolü) yok — kimliği doğrulanmış herhangi bir kullanıcı başka bir müşterinin ödemesini görüntüleyebilir/iade edebilir.

## Detaylı Dokümantasyon

Geliştirme sürecinin detaylı anlatımı ve tam API referansı için: [PAYMENT_SERVICE_DEVELOPMENT.md](./PAYMENT_SERVICE_DEVELOPMENT.md)
