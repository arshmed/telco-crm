# Customer Service

Telco CRM'de müşteri (bireysel/kurumsal) kayıtlarını, adreslerini, belgelerini ve KYC (kimlik doğrulama) sürecini yöneten mikroservis. Platformdaki birçok servisin (order, ticket, subscription…) müşteri doğrulaması için Feign ile sorguladığı kaynaktır.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `customer-service` |
| Port | `9002` |
| Veritabanı | PostgreSQL — `customer` (`localhost:5402`) |
| Cache | Redis (`localhost:6379`) |
| Şema yönetimi | Flyway, `ddl-auto: validate` |
| Kimlik doğrulama | Keycloak JWT — realm `telcocrm-gygy5` (`localhost:8085`) |
| Servis keşfi | Eureka (`8761`) |
| Mesajlaşma | Transactional Outbox → Debezium CDC (yalnızca üretici) |
| Framework | Spring Boot, Spring Cloud |
| Konfigürasyon | Spring Cloud Config Server (`8888`) |
| Nesne eşleme | MapStruct |
| İzlenebilirlik | Zipkin (`9411`, tracing) + Micrometer/OpenTelemetry |
| Loglama | Loki + Promtail + Grafana (ECS formatlı loglar, Correlation-Id) |

> Müşteri kendi kaydına erişimi `CustomerAccessGuard` (own-resource) ile kısıtlanır. Silme işlemi soft-delete'tir. KYC onay/ret yalnızca `CALL_CENTER_AGENT` veya `FIELD_DEALER` rolüne açıktır.

---

## 2. Endpoint'ler

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/api/v1/customers` | auth | Müşteri oluşturur → `CustomerRegisteredEvent` |
| GET | `/api/v1/customers` | auth | Müşterileri sayfalı listeler |
| GET | `/api/v1/customers/{id}` | auth (own) | Tekil müşteri getirir |
| GET | `/api/v1/customers/byNo/{customerNo}` | auth | Müşteri numarasıyla getirir |
| PUT | `/api/v1/customers/{id}` | auth | Günceller → `CustomerUpdatedEvent` |
| DELETE | `/api/v1/customers/{id}` | auth | Soft-delete |
| POST | `/api/v1/customers/{id}/documents` | auth | Müşteriye belge ekler |
| POST | `/api/v1/customers/{id}/kyc/approve` | CALL_CENTER_AGENT, FIELD_DEALER | KYC onaylar → `CustomerKYCApprovedEvent` |
| POST | `/api/v1/customers/{id}/kyc/reject` | CALL_CENTER_AGENT, FIELD_DEALER | KYC reddeder → `CustomerKYCRejectedEvent` |
| GET | `/api/v1/document-types` | auth | Aktif belge tiplerini listeler |

**Örnek — Müşteri oluşturma**
```json
POST /api/v1/customers
{
  "type": "INDIVIDUAL",
  "firstName": "Ayşe",
  "lastName": "Yılmaz",
  "identityNumber": "12345678901",
  "dateOfBirth": "1995-04-12",
  "email": "ayse.yilmaz@example.com",
  "phone": "5551234567",
  "addresses": [
    { "type": "HOME", "city": "İstanbul", "line1": "..." }
  ]
}
```

---

## 3. Kafka Event'leri

Değişiklikler Transactional Outbox tablosuna iş verisiyle aynı transaction'da yazılıp Debezium CDC ile Kafka'ya taşınır. Bu olayları özellikle notification-service tüketir.

**Yayınladıkları:**
- `CustomerRegisteredEvent` — müşteri oluşturulduğunda (`customer-registered-topic`)
- `CustomerUpdatedEvent` — müşteri güncellendiğinde (`customer-updated-topic`)
- `CustomerKYCApprovedEvent` — KYC onaylandığında (`customer-kyc-approved-topic`)
- `CustomerKYCRejectedEvent` — KYC reddedildiğinde (`customer-kyc-rejected-topic`)

**Dinledikleri:** Yok.

---

## 4. Servis Nasıl Çalıştırılır?

1. Repo kökünde `docker compose -f docker/docker-compose.yml up -d` (Postgres, Kafka, Debezium Connect, Keycloak `8085`, Zipkin, Loki/Grafana; Redis dahil).
2. `discovery-server` başlatılır (Eureka, `8761`).
3. `config-server` başlatılır (`8888`) — servis ayarlarını buradan çeker, önce ayağa kalkmalı.
4. `docker/register-connectors.sh` ile Debezium connector'ları kaydedilir (atlanırsa outbox event'leri Kafka'ya taşınmaz).
5. `customer-service` host üzerinde `dev` profiliyle başlatılır (port `9002`).
