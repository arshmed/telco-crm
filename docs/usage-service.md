# Usage Service

Abonelerin kullanım (voice/SMS/data) tüketimini takip eden, kota (quota) durumunu tutan ve eşik/aşım olaylarını yayınlayan mikroservis. Ağdan gelen çağrı kayıtlarını (CDR — Call Detail Record) **olay tabanlı** işler; bu yönüyle platformun tek **event-consumer** ağırlıklı servisidir.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `usage-service` |
| Port | `9006` |
| Veritabanı | PostgreSQL — `usage` (`localhost:5406`) |
| Şema yönetimi | Flyway, `ddl-auto: validate` |
| Kimlik doğrulama | Keycloak JWT (Resource Server) + `client_credentials` (giden çağrılar için) |
| Mesajlaşma | Spring Cloud Stream (Kafka) — **tüketici + üretici** |
| Servisler arası çağrı | OpenFeign → `product-catalog-service`, `customer-service` |
| İzlenebilirlik | Zipkin + ECS loglar + Correlation-Id |

---

## 2. Veri Modeli

### Quota (Kota)
Bir abonenin bir fatura dönemine ait tüketim sayacı. `subscription_id + period_start` benzersizdir.

| Alan | Açıklama |
|---|---|
| `subscriptionId`, `customerId`, `msisdn` | Abone kimlikleri |
| `email`, `firstName`, `lastName` | Bildirim için müşteri snapshot'ı |
| `tariffCode` | Kotanın türetildiği tarife |
| `periodStart`, `periodEnd` | Fatura dönemi (1 ay) |
| `minutesIncluded` / `smsIncluded` / `dataMbIncluded` | Paket hakları |
| `minutesUsed` / `smsUsed` / `dataMbUsed` | Tüketilen |
| `aggregatedAt` | Dönem kapandıktan sonra özet yayınlandı mı |

### UsageRecord
Her CDR bir satır: `subscriptionId`, `type` (`VOICE`/`SMS`/`DATA`), `quantity`, `recordedAt`, `cdrRef`.

### ProcessedEvent
İşlenmiş Kafka olaylarının `eventId`'lerini tutar — **idempotency** (tekrar işlememe) garantisi.

### OutboxEvent
Yayınlanacak olayların transaction içinde yazıldığı tablo.

---

## 3. API Uç Noktaları

Servisin ana veri akışı Kafka üzerinden olduğu için REST yüzeyi dardır (sorgu + operasyon).

### Sorgu — `/api/v1/usage/subscriptions/{subscriptionId}`

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| GET | `/quota` | auth | Abonenin güncel kota durumu (hak/tüketim) |
| GET | `/history` | auth | Kullanım geçmişi (`?from=&to=`, sayfalı, tarih azalan) |

### Toplama — `/api/v1/usage/aggregations`

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/run` | admin | Kapanmış dönemleri özetler → `usage-aggregated` (`?asOf=` opsiyonel) |

---

## 4. Çalışma Mantığı

### Tükettiği olaylar (Kafka consumer)
Spring Cloud Stream fonksiyonel binding'lerle iki olay dinlenir:

**`subscription-activated-topic` → `processSubscriptionActivated`**
1. `eventId` daha önce işlendiyse atlanır (idempotency).
2. `product-catalog-service`'ten tarife çekilir; paket hakları (`minutesIncluded` vb.) okunur.
3. `customer-service`'ten müşteri bilgisi alınır (bildirim snapshot'ı).
4. İlgili dönem için sayaçları sıfırdan başlayan bir `Quota` oluşturulur.

**`cdr-recorded-topic` → `processCdrRecorded`**
1. Idempotency kontrolü.
2. O aboneye ait aktif kota **kilitlenerek** (`findActiveForUpdate`, `SELECT ... FOR UPDATE`) çekilir — eşzamanlı CDR'lerde sayaç yarış koşulu (race condition) yaşanmaz. Kota yoksa CDR atlanır ama işlenmiş sayılır.
3. Bir `UsageRecord` yazılır; tipe göre (`VOICE`/`SMS`/`DATA`) ilgili sayaç artırılır.
4. Eşik geçişleri kontrol edilir (bkz. aşağı).

### Eşik ve aşım olayları
Her CDR sonrası `before` ve `after` değerleri paket hakkıyla kıyaslanır:
- **%80 uyarısı**: tüketim %80'i ilk kez geçtiyse → `quota-threshold-reached-topic`.
- **Aşım**: tüketim paket hakkını ilk kez geçtiyse → `quota-exceeded-topic`.

"İlk kez geçme" kontrolü (`before < eşik && after >= eşik`) sayesinde aynı olay tekrar tekrar yayınlanmaz. Paket hakkı `0` ise (sınırsız/uygulanamaz) eşik hesaplanmaz.

### Dönem toplama (aggregation)
`POST /aggregations/run` çağrısı, `periodEnd` geçmiş ve henüz özetlenmemiş kotaları bulur; her biri için `usage-aggregated` olayını yayınlar ve `aggregatedAt` damgalar. Bu olay faturalama servisi için dönem sonu tüketim özetidir. `asOf` verilmezse bugün baz alınır.

### Giden olaylar (üretici)
Tüm yayınlar **Transactional Outbox** üzerinden: iş verisi ve olay aynı transaction'da yazılır, CDC ile Kafka'ya taşınır.
- `quota-threshold-reached-topic`
- `quota-exceeded-topic`
- `usage-aggregated-topic`

### CdrSimulator
Gerçek şebeke entegrasyonu henüz yokken CDR akışını taklit eden geliştirme aracı; test/demo amaçlı `cdr-recorded` olayları üretir.

---

## 5. Güvenlik

- **Gelen**: Stateless OAuth2 Resource Server; sorgular kimlik doğrulaması, `aggregations/run` `admin` ister.
- **Giden**: Feign çağrıları için `client_credentials` akışıyla servis-servis token alınır (`FeignClientCredentialsInterceptor` + `OAuth2ClientConfig`) — kullanıcı oturumu olmadan (Kafka tetiklemeli) çağrılarda kimlik taşınır.
- Circuit Breaker: `product-catalog` ve `customer` çağrıları korunur; `401/403/404` istisnaları devreyi açmaz (bunlar geçici hata değil iş hatasıdır).

---

## 6. Hata Yönetimi

| İstisna | HTTP |
|---|---|
| `QuotaNotFoundException` | 404 |
| `TariffNotFoundException` | 404 |
| `DownstreamAccessException` | 502 |
| `ServiceUnavailableException` | 503 |
| `OutboxPersistenceException` | 500 |
