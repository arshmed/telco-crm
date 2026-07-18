# Ticket Service

Müşteri destek taleplerini (ticket) yöneten mikroservis. Talep açma, ekibe atama, yorum ekleme ve çözümleme akışlarını yürütür; ayrıca **SLA (hizmet seviyesi taahhüdü) ihlallerini** otomatik tespit eder ve ilgili olayları bildirim servisine iletir.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `ticket-service` |
| Port | `9010` |
| Veritabanı | PostgreSQL — `ticket` (`localhost:5410`) |
| Şema yönetimi | Flyway, `ddl-auto: validate` |
| Kimlik doğrulama | Keycloak JWT (OAuth2 Resource Server) |
| Servisler arası çağrı | OpenFeign → `customer-service` (Resilience4j Circuit Breaker) |
| Mesajlaşma | Transactional Outbox → CDC (üretici) |
| Zamanlanmış iş | SLA ihlali tarayıcısı (60 sn) |
| İzlenebilirlik | Zipkin + ECS loglar + Correlation-Id |

---

## 2. Veri Modeli

### Ticket
| Alan | Açıklama |
|---|---|
| `customerId`, `customerName` | Müşteri kimliği ve snapshot ad |
| `category` | `COMPLAINT`, `REQUEST`, `FAULT` |
| `priority` | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `status` | `ASSIGNED` → `RESOLVED` |
| `description` | Talep metni (≤ 2000 karakter) |
| `assignedTeam` | Atanan ekip |
| `slaDueAt` | SLA son tarihi (önceliğe göre hesaplanır) |
| `slaBreached` | SLA aşıldı mı |
| `resolution`, `resolvedAt` | Çözüm metni ve zamanı |
| `version` | Optimistic locking (`@Version`) |
| `comments` | `TicketComment` ile One-to-Many |

`createdAt` / `updatedAt` JPA Auditing ile otomatik doldurulur.

### TicketComment
Ticket'a bağlı yorumlar; `authorId` (JWT'den gelen kullanıcı) ve `body` (≤ 2000 karakter) tutar.

### OutboxEvent
Domain olaylarının transaction içinde yazıldığı tablo; CDC ile Kafka'ya taşınır.

---

## 3. API Uç Noktaları — `/api/v1/tickets`

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/` | auth | Talep açar → müşteri doğrulanır → `ticket-opened` |
| GET | `/` | auth | Talepleri listeler (`?status=` filtresi, sayfalı, tarih azalan) |
| GET | `/{ticketId}` | auth | Talep detayını getirir |
| POST | `/{ticketId}/comments` | auth | Yorum ekler (yazar JWT'den alınır) |
| POST | `/{ticketId}/assign` | admin | Talebi ekibe atar |
| POST | `/{ticketId}/resolve` | admin | Talebi çözümler → `ticket-resolved` |

**Örnek — Talep açma**
```json
POST /api/v1/tickets
{
  "customerId": "8f3a...",
  "category": "COMPLAINT",
  "priority": "HIGH",
  "description": "Fatura tutarı hatalı yansıdı."
}
```

---

## 4. Çalışma Mantığı

### Talep oluşturma
1. `customer-service`'e Feign ile gidilir; müşteri **`ACTIVE` değilse** talep reddedilir.
2. `TicketSlaRules` önceliğe göre ekip ve SLA son tarihini belirler:

   | Öncelik | SLA süresi |
   |---|---|
   | URGENT | 4 saat |
   | HIGH | 8 saat |
   | MEDIUM | 24 saat |
   | LOW | 72 saat |

   > SLA süreleri şu an placeholder'dır (`ponytail:` notu); iş birimi teyidiyle config'e taşınacaktır.
3. Ticket `ASSIGNED` durumunda kaydedilir; `ticket-opened` olayı outbox'a yazılır (müşteri e-postası dahil, bildirim için).

### Durum kuralları (`TicketStateRules`)
Tüm durum geçişleri tek noktada toplanmıştır. **Çözülmüş (`RESOLVED`) bir talep artık değiştirilemez** — atama veya yeniden çözümleme denenirse `TicketNotModifiableException` (409) döner. Çözümleme sırasında `resolvedAt` enjekte edilen `Clock` ile set edilir (test edilebilirlik için).

### SLA ihlali tespiti (otomatik)
`SlaBreachScheduler` her 60 saniyede bir çalışır:
1. `slaDueAt` geçmiş, henüz `RESOLVED` olmamış ve `slaBreached = false` olan ticket'ları batch halinde (100'lük) bulur.
2. Her biri için `publishBreach` **ayrı transaction** (`REQUIRES_NEW`) içinde çalışır: ticket `slaBreached = true` işaretlenir ve `sla-breached` olayı yayınlanır.
3. `@Version` optimistic locking sayesinde **çok instance'lı** ortamda aynı ticket iki kez yayınlanmaz; çakışan instance `ObjectOptimisticLockingFailureException` alıp sessizce atlar.

### Dayanıklılık (resilience)
`customer-service` çağrıları Circuit Breaker ile korunur:
- `404` → `CustomerNotFoundException` (talep açılamaz).
- Diğer hatalar / servis kapalı → `ServiceUnavailableException` (503).
- Hata oranı %50'yi aşınca devre 30 sn açık kalır, sonra half-open ile toparlanma denenir.

### Transactional Outbox
`ticket-opened`, `ticket-resolved` ve `sla-breached` olayları iş verisiyle aynı transaction'da outbox'a yazılır; CDC ile Kafka topic'lerine taşınır (`ticket-opened-topic`, `ticket-resolved-topic`, `sla-breached-topic`). Bildirim servisi bu olaylarla müşteriye e-posta gönderir.

---

## 5. Güvenlik

- Stateless OAuth2 Resource Server; Keycloak JWT doğrulaması.
- Okuma ve yorum ekleme kimliği doğrulanmış kullanıcılara açık; **atama ve çözümleme yalnızca `admin`**.
- Yorum yazarı istekten değil, `Authentication`'dan (JWT subject) alınır — sahtecilik engellenir.
- Feign çağrılarına gelen isteğin JWT'si `FeignJwtInterceptor` ile taşınır.

---

## 6. Hata Yönetimi

| İstisna | HTTP |
|---|---|
| `TicketNotFoundException` | 404 |
| `CustomerNotFoundException` | 404 |
| `TicketNotModifiableException` | 409 |
| `ServiceUnavailableException` | 503 |
| Validasyon / müşteri pasif | 400 |
