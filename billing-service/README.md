# Billing Service

Telco CRM'de faturalamayı yöneten mikroservis. Abonelik aktive olduğunda o abone için bir **fatura döngüsü** (bill cycle) oluşturur, dönemi gelen döngüler için fatura (invoice) üretir ve ödeme tamamlandığında faturayı `PAID` işaretler. Kullanım aşım (overage) bilgisini de dinler.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `billing-service` |
| Port | `9007` |
| Veritabanı | PostgreSQL — `billing` (`localhost:5407`) |
| Şema yönetimi | Flyway, `ddl-auto: validate` |
| Kimlik doğrulama | Keycloak JWT — realm `telcocrm-gygy5` (`localhost:8085`) |
| Servisler arası çağrı | OpenFeign → `product-catalog-service` (tarife fiyatı) |
| Servis keşfi | Eureka (`8761`) |
| Mesajlaşma | Spring Cloud Stream (tüketici) + Transactional Outbox → Debezium CDC (üretici) |
| Framework | Spring Boot, Spring Cloud |
| Konfigürasyon | Spring Cloud Config Server (`8888`) |
| Nesne eşleme | MapStruct |
| İzlenebilirlik | Zipkin (`9411`, tracing) + Micrometer/OpenTelemetry |
| Loglama | Loki + Promtail + Grafana (ECS formatlı loglar, Correlation-Id) |

> Tüketilen olaylar `ProcessedEvent` tablosuyla idempotent işlenir (aynı `eventId` iki kez işlenmez).

---

## 2. Endpoint'ler

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/api/v1/billing/runs` | BILLING_OPERATOR | Fatura döngüsünü tetikler (`?asOf=` opsiyonel) → dönemi gelen döngüler için fatura üretir |
| GET | `/api/v1/invoices` | auth | Faturaları sayfalı listeler (`?customerId=` ile filtrelenebilir) |
| GET | `/api/v1/invoices/{id}` | auth | Tekil fatura detayını getirir |
| GET | `/api/v1/invoices/stats` | auth | Fatura istatistikleri (toplam/tutar özetleri) |
| GET | `/api/v1/billing/cycles` | auth | Bir müşterinin fatura döngülerini listeler (`?customerId=` zorunlu) |

**Örnek — Fatura koşusu tetikleme**
```
POST /api/v1/billing/runs?asOf=2026-07-31
```
`asOf` verilmezse bugün baz alınır; yanıt `{ "generated": <adet>, "asOf": ..., "status": "completed" }`.

---

## 3. Kafka Event'leri

Üretilen olaylar Transactional Outbox'a iş verisiyle aynı transaction'da yazılıp Debezium CDC ile Kafka'ya taşınır.

**Dinledikleri:**
- `SubscriptionActivatedEvent` — abone için fatura döngüsü (bill cycle) oluşturulur
- `PaymentCompletedEvent` — `invoiceId` doluysa ilgili fatura `PAID` işaretlenir
- `UsageAggregatedEvent` — dönem sonu aşım (overage) bilgisi işlenir

**Yayınladıkları:**
- `InvoiceGeneratedEvent` — fatura koşusunda yeni fatura üretildiğinde (`invoice-generated-topic`)
- `InvoicePaidEvent` — fatura ödendiğinde

---

## 4. Servis Nasıl Çalıştırılır?

1. Repo kökünde `docker compose -f docker/docker-compose.yml up -d` (Postgres, Kafka, Debezium Connect, Keycloak `8085`, Zipkin, Loki/Grafana).
2. `discovery-server` başlatılır (Eureka, `8761`).
3. `config-server` başlatılır (`8888`) — servis ayarlarını buradan çeker, önce ayağa kalkmalı.
4. `docker/register-connectors.sh` ile Debezium connector'ları kaydedilir (atlanırsa outbox event'leri Kafka'ya taşınmaz).
5. `billing-service` host üzerinde `dev` profiliyle başlatılır (port `9007`).
