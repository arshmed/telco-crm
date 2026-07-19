# API Gateway

Platformdaki tüm mikroservislerin önündeki **tek giriş noktası**. Spring Cloud Gateway (reactive/WebFlux) tabanlıdır; `/api/v1/*` isteklerini Eureka üzerinden ilgili servise yönlendirir, Keycloak JWT'sini doğrular, Redis destekli hız sınırlama (rate limiting) uygular ve alt servislere kimlik/izlenebilirlik başlıkları taşır.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `gateway-server` |
| Port | `8080` |
| Temel bağımlılık | `spring-cloud-starter-gateway` (WebFlux) |
| Kimlik doğrulama | OAuth2 Resource Server — Keycloak JWT, realm `telcocrm-gygy5` (`localhost:8085`) |
| Hız sınırlama | Redis (`localhost:6379`), 100 istek/sn, kullanıcı bazlı |
| Servis keşfi | Eureka (`8761`), `lb://` yönlendirme |
| Konfigürasyon | Spring Cloud Config Server (`8888`) |
| İzlenebilirlik | `Correlation-Id` üretimi + ECS formatlı loglar (Loki/Grafana) |

> CORS **burada tanımlanmaz**, yalnızca tarayıcıya doğrudan bakan `bff-server`'da tanımlıdır. İki katmanda birden tanımlanırsa `Access-Control-Allow-Origin` başlığı iki kez eklenir ve tarayıcı isteği CORS ihlali sayar. Zincir: `browser → bff-server → gateway-server → servisler`.

---

## 2. Yönlendirme (Routing)

`/api/v1/*` yolları Eureka'daki mantıksal servis adına (`lb://`) yönlendirilir:

| Yol deseni | Hedef servis |
|---|---|
| `/api/v1/customers/**`, `/api/v1/document-types/**` | `customer-service` |
| `/api/v1/tariffs/**`, `/api/v1/addons/**` | `product-catalog-service` |
| `/api/v1/orders/**` | `order-service` |
| `/api/v1/subscriptions/**` | `subscription-service` |
| `/api/v1/usage/**` | `usage-service` |
| `/api/v1/invoices/**`, `/api/v1/billing/**` | `billing-service` |
| `/api/v1/payments/**` | `payment-service` |
| `/api/v1/notifications/**` | `notification-service` |
| `/api/v1/tickets/**` | `ticket-service` |

`/actuator/**` ve `OPTIONS` (CORS preflight) istekleri kimlik doğrulamasız geçer; geri kalan her istek geçerli JWT ister.

---

## 3. Güvenlik ve Global Filtreler

### JWT doğrulama
Gateway bir **resource server**'dır: gelen `Bearer` token'ı Keycloak'ın JWK set'i ile doğrular. Realm rolleri (`realm_access.roles`) authority'lere dönüştürülür.

### Global filtreler (çalışma sırasıyla)
1. **`CorrelationIdFilter`** (order `-2`) — istekte `Correlation-Id` yoksa yeni bir UUID üretip ekler. Tüm servislerde aynı istek zinciri boyunca log'lar bu id ile ilişkilendirilir.
2. **`JwtHeaderRelayFilter`** (order `-1`) — JWT'den `X-User-Id` (subject) ve `X-User-Roles` başlıklarını türetip alt servise iletir. İstemcinin gönderdiği sahte `X-User-*` başlıkları **önce silinir**, sonra gerçek değerler eklenir (spoofing engellenir).

### Hız sınırlama
Redis tabanlı `RequestRateLimiter` varsayılan filtre olarak tüm route'lara uygulanır: kullanıcı başına saniyede 100 istek (burst 100). Anahtar `userKeyResolver` ile JWT'deki kullanıcı adından çözülür; kimliksiz istekler `anonymous` kovasına düşer.

---

## 4. Servis Nasıl Çalıştırılır?

1. Docker altyapısı ayağa kaldırılır (Keycloak `8085`, Redis `6379`, Loki/Grafana).
2. `config-server` (`8888`) ve `discovery-server` (`8761`) başlatılır — gateway ayarını ve servis listesini bunlardan çeker.
3. `gateway-server` `dev` profiliyle başlatılır (port `8080`).
4. İstekler `http://localhost:8080/api/v1/...` üzerinden (veya önündeki `bff-server` aracılığıyla) yönlendirilir.
