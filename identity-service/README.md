# Identity Service

Telco CRM'de User/Role/Permission yönetimini üstlenen mikroservis. Keycloak'ı **ikame etmez** — login, JWT üretimi ve refresh token rotation Keycloak'ta kalır; identity-service yalnızca kullanıcı/rol/yetki CRUD'unu ve bunun Keycloak realm'ine **tek yönlü, best-effort** senkronizasyonunu yapar. Senkronizasyon başarısız olursa (Keycloak erişilemez) kullanıcı DB'de kalır, yalnızca loglanır; `keycloakUserId` senkron başarılı olunca dolar.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `identity-service` |
| Port | `9001` |
| Veritabanı | PostgreSQL — `identity` (`localhost:5401`) |
| Şema yönetimi | Flyway, `ddl-auto: validate` |
| Kimlik doğrulama | Keycloak JWT — realm `telcocrm-gygy5` (`localhost:8085`) + Keycloak Admin REST API (senkronizasyon) |
| Servis keşfi | Eureka (`8761`) |
| Mesajlaşma | Transactional Outbox → Debezium CDC (yalnızca üretici) |
| Framework | Spring Boot, Spring Cloud |
| Konfigürasyon | Spring Cloud Config Server (`8888`) |
| Dayanıklılık | Resilience4j (Keycloak senkron circuit breaker) |
| Nesne eşleme | MapStruct |
| İzlenebilirlik | Zipkin (`9411`, tracing) + Micrometer/OpenTelemetry |
| Loglama | Loki + Promtail + Grafana (ECS formatlı loglar, Correlation-Id) |

> Permission (ince taneli yetki) yalnızca identity-service'in kendi endpoint'lerini korumak için kullanılır; JWT claim'i olarak taşınmaz — diğer servisler Keycloak realm role'üne göre yetkilendirmeye devam eder.

---

## 2. Endpoint'ler

Tüm uç noktalar kimliği doğrulanmış kullanıcıya açıktır (`auth`).

| Metot | Yol | Yetki | Açıklama |
|---|---|---|---|
| POST | `/api/v1/users` | auth | Kullanıcı oluşturur → `UserCreatedEvent` |
| GET | `/api/v1/users` | auth | Kullanıcıları sayfalı listeler |
| GET | `/api/v1/users/{id}` | auth | Tekil kullanıcı getirir (rolleriyle) |
| PUT | `/api/v1/users/{id}` | auth | Kısmi günceller (email/fullName/phoneNumber) |
| POST | `/api/v1/users/{id}/roles` | auth | Kullanıcıya rol atar → `RoleAssignedEvent` |
| POST | `/api/v1/roles` | auth | Rol oluşturur |
| GET | `/api/v1/roles` | auth | Rolleri listeler |
| POST | `/api/v1/roles/{name}/permissions` | auth | Role permission atar |
| POST | `/api/v1/permissions` | auth | Permission oluşturur |
| GET | `/api/v1/permissions` | auth | Permission'ları listeler |

**Örnek — Kullanıcı oluşturma**
```json
POST /api/v1/users
{
  "username": "ayse.yilmaz",
  "email": "ayse.yilmaz@telcocrm.com",
  "fullName": "Ayşe Yılmaz",
  "phoneNumber": "5551234567",
  "customerId": "8f3a1c2e-..."
}
```

---

## 3. Kafka Event'leri

Değişiklikler Transactional Outbox tablosuna iş verisiyle aynı transaction'da yazılır, Debezium CDC ile Kafka'ya taşınır.

**Yayınladıkları:**
- `UserCreatedEvent` — kullanıcı başarıyla oluşturulduğunda
- `RoleAssignedEvent` — bir kullanıcıya rol atandığında

**Dinledikleri:** Yok.

---

## 4. Servis Nasıl Çalıştırılır?

1. Repo kökünde `docker compose -f docker/docker-compose.yml up -d` (Postgres, Kafka, Debezium Connect, Keycloak `8085`, Zipkin, Loki/Grafana).
2. `discovery-server` başlatılır (Eureka, `8761`).
3. `config-server` başlatılır (`8888`) — servis ayarlarını buradan çeker, önce ayağa kalkmalı.
4. `docker/register-connectors.sh` ile Debezium connector'ları kaydedilir (atlanırsa outbox event'leri Kafka'ya taşınmaz).
5. `identity-service` host üzerinde `dev` profiliyle başlatılır (port `9001`).
