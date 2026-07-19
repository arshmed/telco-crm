# Ticket Service

Müşteri destek taleplerini (ticket) yöneten mikroservis. Talep açma, ekibe atama, yorum ekleme ve çözümleme akışlarını yürütür; ayrıca **SLA (hizmet seviyesi taahhüdü) ihlallerini** bir zamanlanmış tarayıcıyla otomatik tespit eder ve ilgili olayları yayınlayarak bildirim servisini besler.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `ticket-service` |
| Port | `9010` |
| Veritabanı | PostgreSQL — `ticket` (`localhost:5410`) |
| Şema yönetimi | Flyway, `ddl-auto: validate` |
| Kimlik doğrulama | Keycloak JWT — realm `telcocrm-gygy5` (`localhost:8085`) |
| Servisler arası çağrı | OpenFeign → `customer-service` (Resilience4j Circuit Breaker) |
| Servis keşfi | Eureka (`8761`) |
| Mesajlaşma | Transactional Outbox → Debezium CDC (yalnızca üretici) |
| Zamanlanmış iş | SLA ihlali tarayıcısı (60 sn) |
| Framework | Spring Boot, Spring Cloud |
| Konfigürasyon | Spring Cloud Config Server (`8888`) |
| Dayanıklılık | Resilience4j (Circuit Breaker) |
| Nesne eşleme | MapStruct |
| İzlenebilirlik | Zipkin (`9411`, tracing) + Micrometer/OpenTelemetry |
| Loglama | Loki + Promtail + Grafana (ECS formatlı loglar, Correlation-Id) |

> Öncelik→SLA süreleri (URGENT 4s, HIGH 8s, MEDIUM 24s, LOW 72s) şimdilik placeholder'dır; iş birimi teyidiyle config'e taşınacak. SLA tarayıcısı `@Version` optimistic locking sayesinde çok instance'lı ortamda aynı ticket'ı iki kez yayınlamaz.

---

## 2. Endpoint'ler — `/api/v1/tickets`

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/` | auth | Talep açar → müşteri doğrulanır → `ticket-opened` |
| GET | `/` | auth | Talepleri listeler (`?status=` filtresi, sayfalı, tarih azalan) |
| GET | `/{ticketId}` | auth | Talep detayını getirir |
| POST | `/{ticketId}/comments` | auth | Yorum ekler (yazar JWT'den alınır, sahtecilik engellenir) |
| POST | `/{ticketId}/assign` | admin | Talebi ekibe atar |
| POST | `/{ticketId}/resolve` | admin | Talebi çözümler → `ticket-resolved` |

**Örnek — Talep açma**
```json
POST /api/v1/tickets
{
  "customerId": "8f3a1c2e-...",
  "category": "COMPLAINT",
  "priority": "HIGH",
  "description": "Fatura tutarı hatalı yansıdı."
}
```

---

## 3. Kafka Event'leri

Olaylar iş verisiyle aynı transaction'da outbox'a yazılır, Debezium CDC ile Kafka'ya taşınır. Bildirim servisi bu olaylarla müşteriye e-posta gönderir. Talep açılırken `customer-service`'e Feign ile gidilir; müşteri `ACTIVE` değilse talep reddedilir. Çözülmüş (`RESOLVED`) talep artık değiştirilemez.

**Yayınladıkları:**
- `ticket-opened` — talep açıldığında (müşteri e-postası dahil)
- `ticket-resolved` — talep çözümlendiğinde
- `sla-breached` — SLA tarayıcısı `slaDueAt`'i geçmiş, henüz çözülmemiş talebi bulduğunda

**Dinledikleri:** Yok.

---

## 4. Servis Nasıl Çalıştırılır?

1. Repo kökünde `docker compose -f docker/docker-compose.yml up -d` (Postgres, Kafka, Debezium Connect, Keycloak `8085`, Zipkin, Loki/Grafana).
2. `discovery-server` başlatılır (Eureka, `8761`).
3. `config-server` başlatılır (`8888`) — servis ayarlarını buradan çeker, önce ayağa kalkmalı.
4. `docker/register-connectors.sh` ile Debezium connector'ları kaydedilir (atlanırsa outbox event'leri Kafka'ya taşınmaz).
5. `ticket-service` host üzerinde `dev` profiliyle başlatılır (port `9010`).
