# Product Catalog Service

Telco CRM platformunda tarife (tariff) ve ek paket (addon) kataloğunu yöneten mikroservis. Ürün tanımlarının **tek doğruluk kaynağıdır**; diğer servisler (order, subscription, usage) ürün bilgisini buradan okur veya yayınlanan olaylardan dinler.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `product-catalog-service` |
| Port | `9003` |
| Veritabanı | PostgreSQL — `catalog` (`localhost:5403`) |
| Cache | Redis (`catalog:` prefix, TTL 10 dk) |
| Şema yönetimi | Flyway (`db/migration`), `ddl-auto: validate` |
| Kimlik doğrulama | Keycloak JWT — realm `telcocrm-gygy5` (`localhost:8085`) |
| Servis keşfi | Eureka (`8761`) |
| Mesajlaşma | **Transactional Outbox → Debezium CDC** (yalnızca üretici) |
| Framework | Spring Boot, Spring Cloud |
| Konfigürasyon | Spring Cloud Config Server (`8888`) |
| Nesne eşleme | MapStruct |
| İzlenebilirlik | Zipkin (`9411`, tracing) + Micrometer/OpenTelemetry |
| Loglama | Loki + Promtail + Grafana (ECS formatlı loglar, Correlation-Id) |

Servis merkezi konfigürasyonunu **config-server**'dan (`8888`) çeker; kendi `application.yml`'i yalnızca servis adını ve config import satırını içerir.

---

## 2. Endpoint'ler

Tüm yazma işlemleri `admin` yetkisi gerektirir; okuma uç noktaları kimliği doğrulanmış her kullanıcıya açıktır.

### Tarifeler — `/api/v1/tariffs`

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/` | admin | Yeni tarife oluşturur (`DRAFT`) → `TariffCreated` |
| GET | `/` | auth | Yürürlükteki tarifeleri listeler (`?status=` filtresi, sayfalı) |
| GET | `/{code}` | auth | Güncel sürümü getirir (cache'li) |
| GET | `/{code}/versions` | auth | Tüm sürümleri (versiyon azalan) |
| GET | `/{code}/versions/{version}` | auth | Belirli sürüm |
| PUT | `/{code}` | admin | İçeriği günceller → **yeni sürüm** → `TariffUpdated` |
| PATCH | `/{code}/price` | admin | Fiyat değiştirir → **yeni sürüm** → `TariffPriceChanged` |
| POST | `/{code}/publish` | admin | `DRAFT` → `ACTIVE` → `TariffPublished` |
| DELETE | `/{code}` | admin | Tüm sürümleri soft-delete |

### Ek Paketler — `/api/v1/addons`

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/` | admin | Ek paket oluşturur |
| GET | `/` | auth | Tümünü listeler; `?tariffCode=` verilirse o tarifenin paketleri |
| GET | `/{code}` | auth | Tekil ek paket |
| PUT | `/{code}` | admin | Günceller |
| DELETE | `/{code}` | admin | Soft-delete |

**Örnek — Tarife oluşturma**
```json
POST /api/v1/tariffs
{
  "code": "GENC-20GB",
  "name": "Genç 20GB",
  "type": "POSTPAID",
  "segment": "YOUTH",
  "monthlyFee": 199.90,
  "minutesIncluded": 1000,
  "smsIncluded": 500,
  "dataMbIncluded": 20480,
  "effectiveFrom": "2026-08-01",
  "addonCodes": ["NETFLIX-VAS"]
}
```

---

## 3. Kafka Event'leri

Servis yalnızca **üreticidir**: değişiklikler Transactional Outbox tablosuna iş verisiyle aynı transaction'da yazılır, Debezium CDC ile Kafka topic'lerine taşınır.

**Yayınladıkları:**
- `TariffCreated` — yeni tarife `DRAFT` olarak oluşturulduğunda
- `TariffUpdated` — tarife içeriği güncellenip yeni sürüm oluştuğunda
- `TariffPriceChanged` — fiyat değişip yeni sürüm oluştuğunda
- `TariffPublished` — tarife `DRAFT` → `ACTIVE`'e geçtiğinde

**Dinledikleri:** Yok — katalog, ürün tanımlarının kaynağıdır; başka servisten olay tüketmez.

---

## 4. Servis Nasıl Çalıştırılır?

1. Repo kökünde `docker compose -f docker/docker-compose.yml up -d` (Postgres, Kafka, Debezium Connect, Keycloak `8085`, Zipkin, Loki/Grafana).
2. `discovery-server` başlatılır (Eureka, `8761`).
3. `config-server` başlatılır (`8888`) — servis ayarlarını buradan çeker, önce ayağa kalkmalı.
4. `docker/register-connectors.sh` ile Debezium connector'ları kaydedilir (atlanırsa outbox event'leri Kafka'ya taşınmaz).
5. `product-catalog-service` host üzerinde `dev` profiliyle başlatılır (port `9003`).
