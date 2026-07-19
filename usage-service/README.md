# Usage Service

Abonelerin kullanım (voice/SMS/data) tüketimini takip eden, kota (quota) durumunu tutan ve eşik/aşım olaylarını yayınlayan mikroservis. Ağdan gelen çağrı kayıtlarını (CDR — Call Detail Record) **olay tabanlı** işler; bu yönüyle platformun tek **event-consumer ağırlıklı** servisidir, REST yüzeyi dardır.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `usage-service` |
| Port | `9006` |
| Veritabanı | PostgreSQL — `usage` (`localhost:5406`) |
| Şema yönetimi | Flyway, `ddl-auto: validate` |
| Kimlik doğrulama | Keycloak JWT — realm `telcocrm-gygy5` (`localhost:8085`) + `client_credentials` (giden çağrılar) |
| Servisler arası çağrı | OpenFeign → `product-catalog-service`, `customer-service` (Resilience4j Circuit Breaker) |
| Servis keşfi | Eureka (`8761`) |
| Mesajlaşma | Spring Cloud Stream (Kafka) — **tüketici + üretici** (üretim outbox → CDC) |
| Framework | Spring Boot, Spring Cloud |
| Konfigürasyon | Spring Cloud Config Server (`8888`) |
| Dayanıklılık | Resilience4j (Circuit Breaker) |
| Nesne eşleme | MapStruct |
| İzlenebilirlik | Zipkin (`9411`, tracing) + Micrometer/OpenTelemetry |
| Loglama | Loki + Promtail + Grafana (ECS formatlı loglar, Correlation-Id) |

> Giden Feign çağrıları için `client_credentials` akışıyla servis-servis token alınır — Kafka tetiklemeli (kullanıcı oturumu olmayan) çağrılarda kimlik böyle taşınır. CDR yarış koşulu, aktif kota `SELECT ... FOR UPDATE` ile kilitlenerek önlenir. `ProcessedEvent` tablosu ile idempotency garanti edilir.

---

## 2. Endpoint'ler

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| GET | `/api/v1/usage/subscriptions/{subscriptionId}/quota` | auth | Abonenin güncel kota durumu (hak/tüketim) |
| GET | `/api/v1/usage/subscriptions/{subscriptionId}/history` | auth | Kullanım geçmişi (`?from=&to=`, sayfalı, tarih azalan) |
| POST | `/api/v1/usage/aggregations/run` | admin | Kapanmış dönemleri özetler → `usage-aggregated` (`?asOf=` opsiyonel) |

**Örnek — Dönem toplama tetikleme**
```
POST /api/v1/usage/aggregations/run?asOf=2026-07-31
```
`asOf` verilmezse bugün baz alınır; `periodEnd` geçmiş ve henüz özetlenmemiş kotalar için `usage-aggregated` yayınlar.

---

## 3. Kafka Event'leri

Ana veri akışı Kafka üzerindendir. Giden olaylar Transactional Outbox'a iş verisiyle aynı transaction'da yazılıp Debezium CDC ile taşınır. Eşik/aşım "ilk kez geçme" kontrolüyle (`before < eşik && after >= eşik`) tekrar yayınlanmaz.

**Dinledikleri:**
- `subscription-activated-topic` — tarife hakları çekilip yeni dönem `Quota`'sı oluşturulur
- `cdr-recorded-topic` — `UsageRecord` yazılır, tipe göre (VOICE/SMS/DATA) sayaç artırılır

**Yayınladıkları:**
- `quota-threshold-reached-topic` — tüketim paket hakkının %80'ini ilk kez geçtiğinde
- `quota-exceeded-topic` — tüketim paket hakkını ilk kez aştığında
- `usage-aggregated-topic` — dönem toplama sonucu (faturalama için dönem sonu özeti)

> `CdrSimulator`, gerçek şebeke entegrasyonu yokken CDR akışını taklit eden test/demo aracıdır.

---

## 4. Servis Nasıl Çalıştırılır?

1. Repo kökünde `docker compose -f docker/docker-compose.yml up -d` (Postgres, Kafka, Debezium Connect, Keycloak `8085`, Zipkin, Loki/Grafana).
2. `discovery-server` başlatılır (Eureka, `8761`).
3. `config-server` başlatılır (`8888`) — servis ayarlarını buradan çeker, önce ayağa kalkmalı.
4. `docker/register-connectors.sh` ile Debezium connector'ları kaydedilir (atlanırsa outbox event'leri Kafka'ya taşınmaz).
5. `usage-service` host üzerinde `dev` profiliyle başlatılır (port `9006`).
