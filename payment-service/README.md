# Payment Service

Telco CRM'de ödeme tahsilatını, başarısız ödemelerin yeniden denenmesini ve iadeleri yöneten mikroservis. order-service'in yayınladığı `OrderCreatedEvent`'i dinleyip ödemeyi otomatik başlatabilir; frontend'den kullanıcının girdiği kart bilgisiyle manuel olarak da tetiklenebilir. İki akış aynı sipariş için çakışabildiğinden, bir siparişin zaten ödemesi varsa ikinci kayıt engellenir.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `payment-service` |
| Port | `9008` |
| Veritabanı | PostgreSQL — `payment` (`localhost:5408`) |
| Şema yönetimi | Flyway, `ddl-auto: validate` |
| Kimlik doğrulama | Keycloak JWT — realm `telcocrm-gygy5` (`localhost:8085`) |
| Servisler arası çağrı | OpenFeign → `order-service` (gerçek tutar/müşteri kaynağı) |
| Servis keşfi | Eureka (`8761`) |
| Mesajlaşma | Transactional Outbox → Debezium CDC (üretici) + Spring Cloud Stream (tüketici) |
| Framework | Spring Boot, Spring Cloud |
| Konfigürasyon | Spring Cloud Config Server (`8888`) |
| Dayanıklılık | Resilience4j (Circuit Breaker) |
| Nesne eşleme | MapStruct |
| İzlenebilirlik | Zipkin (`9411`, tracing) + Micrometer/OpenTelemetry |
| Loglama | Loki + Promtail + Grafana (ECS formatlı loglar, Correlation-Id) |

**Ana yetenekler:** `paymentRequestId` ile idempotency · retry (24/72/168 saat, 4. denemede kalıcı `FAILED`) · otomatik/manuel çakışma koruması · yöntem bazlı mock PSP (kredi kartı/havale/cüzdan farklı başarı oranı) · Luhn/son kullanma doğrulaması · audit log · saga kompanzasyonuyla otomatik iade.

> Gerçek PSP entegrasyonu yok — sonuçlar mock client tarafından simüle edilir. `amount/currency/customerId` client'tan alınmaz; `orderId` üzerinden order-service'ten sunucu tarafında çekilir. Yatay erişim kontrolü henüz yok.

---

## 2. Endpoint'ler

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/api/v1/payments` | auth | Kart bilgisiyle ödeme oluşturur (`paymentRequestId` idempotency anahtarı) |
| GET | `/api/v1/payments` | auth | Ödemeleri sayfalı listeler |
| GET | `/api/v1/payments/{id}` | auth | Tekil ödeme detayını (deneme geçmişi dahil) getirir |
| POST | `/api/v1/payments/{id}/refund` | auth | Tamamlanmış bir ödemeyi iade eder → `PaymentRefundedEvent` |

**Örnek — Ödeme oluşturma**
```json
POST /api/v1/payments
{
  "paymentRequestId": "req-8f3a...",
  "orderId": "1a2b3c4d-...",
  "method": "CREDIT_CARD",
  "cardHolder": "AYSE YILMAZ",
  "cardNumber": "4111 1111 1111 1111",
  "expiryDate": "08/28",
  "cvv": "123"
}
```

---

## 3. Kafka Event'leri

Sonuç event'leri Transactional Outbox'a iş verisiyle aynı transaction'da yazılır, Debezium CDC ile Kafka'ya taşınır; order-service bunları tüketip saga'yı ilerletir.

**Yayınladıkları:**
- `PaymentCompletedEvent` — bir ödeme (ilk deneme veya retry) başarıyla tamamlandığında
- `PaymentFailedEvent` — retry mekanizması tüm denemeleri tükettiğinde
- `PaymentRefundedEvent` — manuel iade veya saga kompanzasyonu sonucunda

**Dinledikleri:**
- `OrderCreatedEvent` — otomatik ödeme akışını başlatır
- `SubscriptionActivationFailedEvent` — otomatik iadeyi tetikler

---

## 4. Servis Nasıl Çalıştırılır?

1. Repo kökünde `docker compose -f docker/docker-compose.yml up -d` (Postgres, Kafka, Debezium Connect, Keycloak `8085`, Zipkin, Loki/Grafana).
2. `discovery-server` başlatılır (Eureka, `8761`).
3. `config-server` başlatılır (`8888`) — servis ayarlarını buradan çeker, önce ayağa kalkmalı.
4. `docker/register-connectors.sh` ile Debezium connector'ları kaydedilir (atlanırsa outbox event'leri Kafka'ya taşınmaz).
5. `payment-service` host üzerinde `dev` profiliyle başlatılır (port `9008`).
