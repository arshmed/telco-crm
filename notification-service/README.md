# Notification Service

Telco CRM'de bildirim gönderimini üstlenen mikroservis. Diğer servislerin yayınladığı domain olaylarını dinler, olayı bir **şablona** (template) eşleyip ilgili kanaldan (e-posta/SMS) müşteriye bildirim gönderir. API'den manuel bildirim de tetiklenebilir.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `notification-service` |
| Port | `9009` |
| Veritabanı | PostgreSQL — `notification` (`localhost:5409`) |
| Cache | Redis (`localhost:6379`) |
| E-posta | SMTP (`smtp.gmail.com:587`) |
| Şema yönetimi | Flyway, `ddl-auto: validate` |
| Kimlik doğrulama | Keycloak JWT — realm `telcocrm-gygy5` (`localhost:8085`) |
| Servis keşfi | Eureka (`8761`) |
| Mesajlaşma | Spring Cloud Stream (çok sayıda tüketici) + Transactional Outbox → Debezium CDC (üretici) |
| Framework | Spring Boot, Spring Cloud |
| Konfigürasyon | Spring Cloud Config Server (`8888`) |
| Nesne eşleme | MapStruct |
| İzlenebilirlik | Zipkin (`9411`, tracing) + Micrometer/OpenTelemetry |
| Loglama | Loki + Promtail + Grafana (ECS formatlı loglar, Correlation-Id) |

> Gönderim şablon bazlıdır (`templateCode`); bildirimler `NotificationChannel` (EMAIL/SMS) üzerinden dağıtılır. Kullanıcının kendi geçmişine erişimi `CustomerAccessGuard` ile kısıtlanır.

---

## 2. Endpoint'ler — `/api/v1/notifications`

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/` | auth | Manuel bildirim gönderir (bastırılırsa `204 No Content`) |
| GET | `/users/{userId}/history` | auth (own) | Kullanıcının bildirim geçmişini sayfalı getirir |
| GET | `/recent` | auth | Son bildirimleri listeler |

**Örnek — Manuel bildirim**
```json
POST /api/v1/notifications
{
  "userId": "8f3a1c2e-...",
  "templateCode": "WELCOME_EMAIL",
  "channel": "EMAIL",
  "payload": { "firstName": "Ayşe" }
}
```

---

## 3. Kafka Event'leri

Servis platformun bildirim toplayıcısıdır: birçok servisin olayını dinleyip her biri için ilgili şablonla müşteriye bildirim gönderir. Gönderim sonucu Transactional Outbox → Debezium CDC ile yayınlanır.

**Dinledikleri:**
- `CustomerRegisteredEvent`, `CustomerUpdatedEvent`, `CustomerKYCApprovedEvent`, `CustomerKYCRejectedEvent` (customer-service)
- `OrderCreatedEvent`, `OrderConfirmedEvent`, `OrderCancelledEvent` (order-service)
- `QuotaThresholdReachedEvent`, `QuotaExceededEvent` (usage-service)
- `TicketOpenedEvent`, `TicketResolvedEvent` (ticket-service)

**Yayınladıkları:**
- `NotificationDispatchedEvent` — bir bildirim gönderildiğinde (`notification-dispatched-topic`)

---

## 4. Servis Nasıl Çalıştırılır?

1. Repo kökünde `docker compose -f docker/docker-compose.yml up -d` (Postgres, Kafka, Debezium Connect, Keycloak `8085`, Zipkin, Loki/Grafana; Redis dahil).
2. `discovery-server` başlatılır (Eureka, `8761`).
3. `config-server` başlatılır (`8888`) — servis ayarlarını buradan çeker, önce ayağa kalkmalı.
4. `docker/register-connectors.sh` ile Debezium connector'ları kaydedilir (atlanırsa outbox event'leri Kafka'ya taşınmaz).
5. `notification-service` host üzerinde `dev` profiliyle başlatılır (port `9009`). SMTP kimlik bilgileri config'te ayarlı olmalı.
