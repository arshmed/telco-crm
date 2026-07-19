# BFF Server

Tarayıcıdaki tek sayfa uygulaması (SPA) ile arka uç arasındaki **Backend-for-Frontend** katmanı. Frontend'in doğrudan token yönetmesini engeller: Keycloak ile OAuth2 `authorization_code` akışını yürütür, kullanıcı oturumunu (session + cookie) tutar ve gelen `/api/**` isteklerine oturumdaki access token'ı ekleyerek gateway'e taşır. Böylece token tarayıcıda hiç görünmez.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `bff-server` |
| Port | `9011` |
| Temel bağımlılık | `spring-cloud-gateway-mvc` + `oauth2-client` |
| Kimlik doğrulama | OAuth2 **Client** (`authorization_code`) — Keycloak, client `bff-client` |
| Oturum | Sunucu tarafı session + cookie, timeout `8h` (8 saat) |
| CSRF | Cookie tabanlı (SPA uyumlu, `XOR` token handler) |
| CORS | `localhost:5173` (Vite), `localhost:3000` — credentials açık |
| Servis keşfi | Eureka (`8761`) |
| Konfigürasyon | Spring Cloud Config Server (`8888`) |

> SPA ayrı origin'de (Vite `:5173`) çalışır. Bu yüzden `/api/**` isteklerinde oturum geçersizse Keycloak'a **redirect atılmaz**, düz `401` dönülür — frontend bu 401'i yakalayıp login'e yönlendirir. Redirect atılsaydı tarayıcı cross-origin CORS preflight zincirine girer ve istek patlardı.

---

## 2. İstek Akışı

```
Tarayıcı (SPA, :5173)
      │  (cookie oturumu, credentials: include)
      ▼
bff-server (:9011)            ← OAuth2 client, oturumu ve token'ı tutar
      │  /api/**  →  lb://gateway-server  (TokenRelay: access token eklenir)
      ▼
gateway-server (:8080)        ← JWT doğrular, yönlendirir
      ▼
mikroservisler
```

- **Login**: Kullanıcı Keycloak'a yönlendirilir, `authorization_code` akışı tamamlanınca bff oturumu açar ve `http://localhost:5173`'e döner.
- **API çağrısı**: SPA `/api/**` çağırır → bff `TokenRelay` filtresiyle oturumdaki access token'ı `Authorization` başlığına koyup `gateway-server`'a proxy'ler.
- **Logout**: OIDC initiated logout ile Keycloak oturumu da sonlandırılır.

---

## 3. Güvenlik Detayları

### CSRF
`CookieCsrfTokenRepository` (httpOnly kapalı) ile CSRF token'ı cookie'de tutulur; SPA bunu okuyup istek başlığına koyar. `SpaCsrfTokenRequestHandler` XOR maskeleme ile BREACH koruması sağlar, `CsrfCookieFilter` token'ın her yanıtta cookie'ye yazılmasını garanti eder.

### CORS
CORS, Spring Security filtre zincirine bağlanır ki `401`/redirect gibi Security'nin erken ürettiği yanıtlarda da doğru `Access-Control-*` başlıkları eklensin. İzinli origin'ler: `localhost:5173`, `localhost:3000`; `credentials: true` (cookie taşınabilsin).

### Neden BFF?
Access/refresh token'lar yalnızca bff'in sunucu tarafı oturumunda kalır — tarayıcıdaki JS'e (localStorage/XSS yüzeyi) hiç düşmez. Frontend sadece httpOnly session cookie'siyle konuşur.

---

## 4. Servis Nasıl Çalıştırılır?

1. Docker altyapısı ayağa kaldırılır (Keycloak `8085` — `bff-client` tanımlı olmalı).
2. `config-server` (`8888`) ve `discovery-server` (`8761`) başlatılır.
3. `gateway-server` (`8080`) başlatılır — bff isteği buraya taşır.
4. `bff-server` `dev` profiliyle başlatılır (port `9011`).
5. Frontend (`telco-crm-fe`, Vite `:5173`) bff'e `credentials: include` ile bağlanır.
