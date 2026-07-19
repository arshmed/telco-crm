# identity-service

## Ne İşe Yarar?

Telco-crm'de User/Role/Permission yönetimini üstlenen servis. Keycloak'ı **ikame etmez** — login, JWT üretimi ve refresh token rotation Keycloak'ta kalır; identity-service yalnızca kullanıcı/rol/yetki CRUD'unu ve bunun Keycloak realm'ine tek yönlü senkronizasyonunu yapar.

## Nasıl Çalışır?

- Bir kullanıcı identity-service'te oluşturulduğunda, önce kendi veritabanına kaydedilir; ardından Keycloak Admin API üzerinden Keycloak'ta da bir hesap açılması **denenir**.
- Keycloak senkronizasyonu **best-effort**'tur: başarısız olursa (Keycloak erişilemez, yanlış yapılandırılmış client vb.) sadece loglanır, kullanıcı DB'de kalıcı olarak var olmaya devam eder. `keycloakUserId` alanı bu yüzden nullable'dır ve senkronizasyon başarılı olduğunda doldurulur.
- Rol atamasında da aynı best-effort prensip geçerlidir: kullanıcı henüz Keycloak'a senkronize olmadıysa (`keycloakUserId: null`), realm role senkronizasyonu hiç denenmeden atlanır.
- Permission (ince taneli yetki), yalnızca identity-service'in kendi endpoint'lerini korumak için kullanılır — JWT claim'i olarak taşınmaz; diğer servisler değişmeden Keycloak realm role'üne göre yetkilendirme yapmaya devam eder.
- Değişiklikler (`UserCreatedEvent`, `RoleAssignedEvent`) outbox tablosuna yazılıp Debezium CDC ile Kafka'ya taşınır.

## Ana Özellikler

- User/Role/Permission CRUD + rol/permission atama.
- Keycloak Admin API senkronizasyonu — Resilience4j circuit breaker ile korunan, best-effort, User oluşturmayı asla bloklamayan.
- Audit log — her User/Role/Permission değişikliği kalıcı olarak saklanır.
- Sayfalı kullanıcı listeleme.
- Optimistic locking (User entity'sinde eşzamanlı güncelleme koruması).

## Teknoloji Stack'i

Spring Boot, Spring Cloud (Eureka, Config Server, OpenFeign), PostgreSQL, Flyway, Resilience4j, Debezium, Keycloak (Admin REST API), Zipkin, Micrometer/OpenTelemetry, Loki + Promtail + Grafana, MapStruct

## API'ler

| Method | Path | Açıklama |
|---|---|---|
| POST | `/api/v1/users` | Kullanıcı oluşturur |
| GET | `/api/v1/users` | Kullanıcıları sayfalı listeler |
| GET | `/api/v1/users/{id}` | Tekil kullanıcı getirir (rolleriyle) |
| PUT | `/api/v1/users/{id}` | Kısmi günceller (email/fullName/phoneNumber) |
| POST | `/api/v1/users/{id}/roles` | Kullanıcıya rol atar |
| POST | `/api/v1/roles` | Rol oluşturur |
| GET | `/api/v1/roles` | Rolleri listeler |
| POST | `/api/v1/roles/{name}/permissions` | Role permission atar |
| POST | `/api/v1/permissions` | Permission oluşturur |
| GET | `/api/v1/permissions` | Permission'ları listeler |

## Kafka Event'leri

**Yayınladıkları:**
- `UserCreatedEvent` — kullanıcı başarıyla oluşturulduğunda
- `RoleAssignedEvent` — bir kullanıcıya rol atandığında

Henüz bir Kafka event tüketicisi yok.

## Nasıl Çalıştırılır?

1. Repo kökünde `docker/docker-compose.yml` ile `docker compose up -d` çalıştırılır (Postgres, Kafka, Debezium Connect, Keycloak, Zipkin, Loki/Grafana dahil).
2. discovery-server başlatılır (Eureka, port 8761).
3. config-server başlatılır (port 8888) — identity-service'in başlaması için zorunlu.
4. `docker/register-connectors.sh` ile Debezium connector'ları kaydedilir.
5. identity-service host üzerinde `dev` profiliyle başlatılır (port 9001).

## Bilinen Sınırlamalar

- Keycloak senkronizasyonunda otomatik retry yok — başarısız senkronizasyon manuel müdahale gerektirir.
- `CreateUserRequest`'te şifre/credential alanı yok; Keycloak'taki ilk credential provisioning ayrıca ele alınmadı.
- Kullanıcı durumu (`INACTIVE`/`SUSPENDED`) değiştiren bir endpoint henüz yok.

## Detaylı Dokümantasyon

Geliştirme sürecinin detaylı anlatımı, mimari kararların gerekçeleri ve tam API referansı için: [IDENTITY_SERVICE_DEVELOPMENT.md](./IDENTITY_SERVICE_DEVELOPMENT.md)
