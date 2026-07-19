# Discovery Server

Tüm Telco CRM mikroservisleri için **servis keşif sunucusu**. Netflix Eureka tabanlıdır; her servis açılışta buraya kaydolur, birbirini isimle (`lb://<servis-adı>`) çağırır. Sabit IP/port bilmeye gerek kalmaz, instance'lar dinamik ölçeklenebilir.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `discovery-server` |
| Port | `8761` |
| Temel bağımlılık | `spring-cloud-netflix-eureka-server` |
| Etkinleştirme | `@EnableEurekaServer` |
| Kendi kaydı | Kapalı (`register-with-eureka: false`, `fetch-registry: false`) |
| Konfigürasyon | Spring Cloud Config Server (`8888`) |
| Panel | `http://localhost:8761` (kayıtlı instance'lar) |

> Discovery-server tek başına (standalone) çalışır: kendini kendine kaydetmez ve registry çekmez. Çok düğümlü (HA) kurulumda birbirine peer olarak kaydolan birden fazla Eureka düğümü kullanılır.

---

## 2. Çalışma Mantığı

### Kayıt (registration)
Her mikroservis `eureka.client` ayarıyla açılışta `http://localhost:8761/eureka/` adresine kendi adı, host'u ve portuyla kaydolur. Eureka bu kaydı bir registry'de tutar ve düzenli heartbeat'lerle canlılığını izler; heartbeat kesilince instance registry'den düşer.

### Çözümleme (discovery)
Bir servis başka bir servisi çağırırken IP/port yerine mantıksal adını kullanır:
```
lb://customer-service
lb://ticket-service
```
`lb://` (load-balanced) öneki, çağrıyı yapan tarafın (gateway veya OpenFeign) Eureka'dan o servisin canlı instance listesini alıp aralarında yük dengelemesi yapmasını sağlar.

### Kimler kullanır?
- **gateway-server** — `/api/v1/*` route'larını `lb://` ile hedef servise yönlendirirken.
- **bff-server** — `/api/**` isteğini `lb://gateway-server`'a taşırken.
- **Servisler arası Feign çağrıları** — ör. `ticket-service` → `lb://customer-service`.

---

## 3. Mimarideki Rolü

Discovery-server, config-server ile birlikte platformun **açılış sırasındaki temel taşıdır**: gateway, bff ve tüm mikroservisler birbirini isimle bulabilmek için önce Eureka'ya kaydolmalıdır. Sağladığı faydalar:

- **Konumdan bağımsızlık**: Servisler sabit IP/port bilmeden birbirini adla çağırır; instance'lar taşınabilir, yeniden başlatılabilir.
- **Dinamik ölçekleme**: Aynı servisin birden çok instance'ı kaydolur, `lb://` yükü aralarında dağıtır.
- **Sağlık takibi**: Heartbeat kesilen instance registry'den düşürülür, çağrılar sadece canlı düğümlere gider.

---

## 4. Servis Nasıl Çalıştırılır?

1. `config-server` başlatılır (`8888`) — ayarlar buradan çekilir.
2. `discovery-server` başlatılır (`8761`) — diğer tüm servislerden **önce** ayakta olmalı.
3. `http://localhost:8761` panelinden kayıtlı instance'lar izlenir.
4. Gateway, bff ve mikroservisler açıldıkça registry'de görünür.
