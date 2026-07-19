# Subscription Service

Telco CRM'de abonelik yaşam döngüsünü yöneten mikroservis. Sipariş oluşunca abonelik (subscription) yaratır, ödeme tamamlanınca aktive eder; askıya alma, yeniden etkinleştirme, sonlandırma, tarife değişimi ve ek paket (addon) yönetimini yürütür. order-service saga'sının abonelik ayağını tamamlar.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `subscription-service` |
| Port | `9005` |
| Veritabanı | PostgreSQL — `subscription` (`localhost:5405`) |
| Şema yönetimi | Flyway, `ddl-auto: validate` |
| Kimlik doğrulama | Keycloak JWT — realm `telcocrm-gygy5` (`localhost:8085`) |
| Servisler arası çağrı | OpenFeign → `customer-service`, `product-catalog-service` |
| Servis keşfi | Eureka (`8761`) |
| Mesajlaşma | Spring Cloud Stream (tüketici) + Transactional Outbox → Debezium CDC (üretici) |
| Framework | Spring Boot, Spring Cloud |
| Konfigürasyon | Spring Cloud Config Server (`8888`) |
| Nesne eşleme | MapStruct |
| İzlenebilirlik | Zipkin (`9411`, tracing) + Micrometer/OpenTelemetry |
| Loglama | Loki + Promtail + Grafana (ECS formatlı loglar, Correlation-Id) |

> Tüketilen olaylar `ProcessedEvent` tablosuyla idempotent işlenir. Durum geçişleri (`PENDING`/`ACTIVE`/`SUSPENDED`/`TERMINATED`) servis içinde tek noktada yönetilir.

---

## 2. Endpoint'ler — `/api/v1/subscriptions`

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/` | auth | Abonelik oluşturur |
| GET | `/` | auth | Abonelikleri sayfalı listeler |
| GET | `/{id}` | auth | Tekil abonelik getirir |
| GET | `/customer/{customerId}` | auth | Müşterinin aboneliklerini listeler |
| GET | `/status/{status}` | auth | Duruma göre listeler |
| GET | `/stats` | auth | Abonelik istatistikleri |
| GET | `/stats/monthly-activations` | auth | Aylık aktivasyon dağılımı (`?months=6`) |
| GET | `/stats/by-tariff` | auth | Tarife bazlı dağılım |
| POST | `/{id}/activate` | auth | Aboneliği aktive eder |
| POST | `/{id}/suspend` | auth | Askıya alır |
| POST | `/{id}/reactivate` | auth | Yeniden etkinleştirir |
| POST | `/{id}/terminate` | auth | Sonlandırır |
| PATCH | `/{id}/tariff` | auth | Tarife değiştirir |
| POST | `/{id}/addons` | auth | Ek paket ekler |
| GET | `/{id}/addons` | auth | Ek paketleri listeler |

**Örnek — Abonelik oluşturma**
```json
POST /api/v1/subscriptions
{
  "customerId": "8f3a1c2e-...",
  "tariffCode": "GENC-20GB",
  "msisdn": "5551234567"
}
```

---

## 3. Kafka Event'leri

Gelen sipariş/ödeme olayları abonelik durumunu ilerletir; sonuç olayları Transactional Outbox → Debezium CDC ile yayınlanır.

**Dinledikleri:**
- `OrderCreatedEvent` — sipariş için `PENDING` abonelik oluşturulur
- `PaymentCompletedEvent` — ilgili abonelik aktive edilmeye çalışılır
- `OrderCancelledEvent` — sipariş iptalinde abonelik geri alınır

**Yayınladıkları:**
- `SubscriptionActivatedEvent` — abonelik aktive olduğunda (`subscription-activated-topic`)
- `SubscriptionActivationFailedEvent` — aktivasyon başarısız olduğunda (`subscription-activation-failed-topic`)

---

## 4. Servis Nasıl Çalıştırılır?

1. Repo kökünde `docker compose -f docker/docker-compose.yml up -d` (Postgres, Kafka, Debezium Connect, Keycloak `8085`, Zipkin, Loki/Grafana).
2. `discovery-server` başlatılır (Eureka, `8761`).
3. `config-server` başlatılır (`8888`) — servis ayarlarını buradan çeker, önce ayağa kalkmalı.
4. `docker/register-connectors.sh` ile Debezium connector'ları kaydedilir (atlanırsa outbox event'leri Kafka'ya taşınmaz).
5. `subscription-service` host üzerinde `dev` profiliyle başlatılır (port `9005`).
