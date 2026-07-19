# Order Service

Telco CRM'de sipariş yaşam döngüsünü yöneten mikroservis. Müşteri sipariş oluşturduğunda müşteri/ürün doğrulamasını yapar, siparişi kaydeder ve ödeme/abonelik aktivasyonu gibi başka servislerde gerçekleşen adımları bir **saga** üzerinden takip edip sonuçta siparişi tamamlar ya da iptal eder. Sipariş cevabında saga durumu (`currentStep`, `retryCount`, `errorMessage`) her zaman görülebilir.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `order-service` |
| Port | `9004` |
| Veritabanı | PostgreSQL — `orders` (`localhost:5404`) |
| Şema yönetimi | Flyway, `ddl-auto: validate` |
| Kimlik doğrulama | Keycloak JWT — realm `telcocrm-gygy5` (`localhost:8085`) |
| Servisler arası çağrı | OpenFeign → `customer-service`, `product-catalog-service` (Resilience4j Circuit Breaker) |
| Servis keşfi | Eureka (`8761`) |
| Mesajlaşma | Transactional Outbox → Debezium CDC (üretici) + Spring Cloud Stream (tüketici) |
| Framework | Spring Boot, Spring Cloud |
| Konfigürasyon | Spring Cloud Config Server (`8888`) |
| Dayanıklılık | Resilience4j (Circuit Breaker) |
| Nesne eşleme | MapStruct |
| İzlenebilirlik | Zipkin (`9411`, tracing) + Micrometer/OpenTelemetry |
| Loglama | Loki + Promtail + Grafana (ECS formatlı loglar, Correlation-Id) |

**Ana yetenekler:** Idempotency-Key ile mükerrer sipariş koruması · saga durum takibi · Circuit Breaker (downstream kapalıysa 503) · audit log · optimistic locking · sayfalı/müşteri bazlı listeleme.

> Yatay erişim kontrolü (authorization) henüz yok — kimliği doğrulanmış herhangi bir kullanıcı başka müşterinin siparişini görüntüleyebilir/iptal edebilir.

---

## 2. Endpoint'ler

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/api/v1/orders` | auth | Sipariş oluşturur (`Idempotency-Key` header'ı opsiyonel) → `OrderCreatedEvent` |
| GET | `/api/v1/orders` | auth | Siparişleri sayfalı listeler (`?customerId=` ile filtrelenebilir) |
| GET | `/api/v1/orders/{id}` | auth | Tekil sipariş detayını (saga durumu dahil) getirir |
| POST | `/api/v1/orders/{id}/cancel` | auth | Yalnızca `PENDING_PAYMENT` durumundaki siparişi iptal eder |

**Örnek — Sipariş oluşturma**
```json
POST /api/v1/orders
Idempotency-Key: 7c1f...  (opsiyonel)
{
  "customerId": "8f3a1c2e-...",
  "items": [
    { "productCode": "GENC-20GB", "productType": "TARIFF", "quantity": 1 },
    { "productCode": "NETFLIX-VAS", "productType": "ADDON", "quantity": 1 }
  ]
}
```

---

## 3. Kafka Event'leri

Sipariş oluşturulurken `customer-service` ve `product-catalog-service`'e senkron (Feign) çağrı yapılır; sipariş `PENDING_PAYMENT` kaydedilir ve `OrderCreatedEvent` outbox'a yazılıp Debezium ile Kafka'ya taşınır. Gelen ödeme/abonelik event'leri saga'yı ilerletir.

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

---

## 4. Servis Nasıl Çalıştırılır?

1. Repo kökünde `docker compose -f docker/docker-compose.yml up -d` (Postgres, Kafka, Debezium Connect, Keycloak `8085`, Zipkin, Loki/Grafana).
2. `discovery-server` başlatılır (Eureka, `8761`).
3. `config-server` başlatılır (`8888`) — servis ayarlarını buradan çeker, önce ayağa kalkmalı.
4. `docker/register-connectors.sh` ile Debezium connector'ları kaydedilir (atlanırsa outbox event'leri Kafka'ya taşınmaz).
5. `order-service` host üzerinde `dev` profiliyle başlatılır (port `9004`).
