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
| Kimlik doğrulama | Keycloak JWT (OAuth2 Resource Server) |
| Servis keşfi | Eureka (`8761`) |
| Mesajlaşma | **Transactional Outbox → Debezium CDC** (yalnızca üretici) |
| İzlenebilirlik | Zipkin tracing + ECS yapılandırılmış loglar + Correlation-Id |

Servis merkezi konfigürasyonunu **config-server**'dan (`8888`) çeker; kendi `application.yml`'i yalnızca servis adını ve config import satırını içerir.

---

## 2. Veri Modeli

### Tariff (Tarife)
Sürümlenmiş (versioned) bir varlıktır. Bir tarife koduna (`code`) karşılık birden çok sürüm bulunabilir; yalnızca biri `current = true` olur.

| Alan | Açıklama |
|---|---|
| `code`, `version` | Birlikte benzersiz. Fiyat/içerik değişince yeni sürüm üretilir |
| `current` | Bu kodun yürürlükteki sürümü mü |
| `type` | `POSTPAID`, `PREPAID`, `HYBRID` |
| `segment` | `INDIVIDUAL`, `CORPORATE`, `YOUTH`, `ALL` |
| `status` | `DRAFT` → `ACTIVE` → `RETIRED` |
| `monthlyFee`, `currency` | Aylık ücret (varsayılan `TRY`) |
| `minutesIncluded`, `smsIncluded`, `dataMbIncluded` | Paket içeriği |
| `effectiveFrom`, `effectiveTo` | Geçerlilik aralığı; yeni sürümde eskisinin `effectiveTo`'su kapatılır |
| `deleted` | Soft-delete |
| `addons` | `tariff_addon` ara tablosuyla Many-to-Many |

### Addon (Ek Paket)
| Alan | Açıklama |
|---|---|
| `code` | Benzersiz |
| `type` | `DATA`, `SMS`, `MINUTES`, `VAS` (katma değerli servis) |
| `price`, `currency`, `validityDays` | Fiyat ve geçerlilik süresi |
| `deleted` | Soft-delete |

### OutboxEvent
Domain olaylarının aynı transaction içinde yazıldığı tablo. Debezium CDC bu tabloyu okuyup Kafka'ya taşır.

---

## 3. API Uç Noktaları

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

## 4. Çalışma Mantığı

### Sürümleme (versioning)
Katalogda **hiçbir tarife yerinde güncellenmez**. `update` ve `changePrice` çağrıları:
1. Mevcut güncel sürümü `current = false` yapar, `effectiveTo`'yu bugüne kapatır.
2. `version + 1` ile yeni bir kayıt üretir ve `current = true` işaretler.

Böylece geçmiş fiyat/içerik tam olarak korunur — faturalama ve denetim için kritik. Sipariş/abonelik servisleri belirli bir `version`'ı referans alarak sözleşmeyi dondurabilir.

### Yaşam döngüsü
`DRAFT` durumunda oluşturulan tarife müşteriye kapalıdır. `publish` yalnızca `DRAFT`'tan `ACTIVE`'e geçişe izin verir; aksi halde `InvalidTariffStatusException` (409) döner.

### Cache
`getByCode` ve `getAddons` sonuçları Redis'te tutulur (`tariffs`, `tariff-addons`). Her yazma işlemi (`update`, `changePrice`, `publish`, `delete`) ilgili `code` anahtarını `@CacheEvict` ile temizler — bayat veri riski yoktur.

### Transactional Outbox
Domain değişikliği ve olay yayını **tek transaction** içinde gerçekleşir: iş verisi kaydedilirken `OutboxEvent` de aynı anda yazılır. Ayrı bir Kafka publish çağrısı yoktur; **Debezium CDC** outbox tablosunu izleyip mesajı Kafka'ya taşır. Bu, "veriyi kaydettim ama olayı yayınlayamadım" tutarsızlığını kökten engeller (dual-write problemi çözülür).

Yayınlanan olaylar: `TariffCreated`, `TariffUpdated`, `TariffPriceChanged`, `TariffPublished`.

---

## 5. Güvenlik

- Stateless OAuth2 Resource Server; her istek Keycloak JWT ile doğrulanır.
- `KeycloakRoleConverter` realm rollerini Spring authority'lerine çevirir; yetkilendirme prefix'siz `hasAuthority('admin')` ile yapılır.
- `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**` herkese açık; diğer tüm uç noktalar kimlik doğrulaması ister.
- CSRF kapalı (stateless API), session `STATELESS`.

---

## 6. Hata Yönetimi

`GlobalExceptionHandler` tüm domain hatalarını tutarlı JSON yanıtlara çevirir:

| İstisna | HTTP |
|---|---|
| `TariffNotFoundException`, `AddonNotFoundException` | 404 |
| `DuplicateCodeException` | 409 |
| `InvalidTariffStatusException` | 409 |
| Validasyon hataları | 400 |
| `OutboxPersistenceException` | 500 |
