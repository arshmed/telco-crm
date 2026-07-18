# Config Server

Tüm Telco CRM mikroservisleri için **merkezi konfigürasyon sunucusu**. Spring Cloud Config Server tabanlıdır; her servisin ortama özel ayarlarını (veritabanı, Keycloak, Kafka, Eureka, port vb.) tek bir yerden dağıtır.

---

## 1. Genel Bakış

| Özellik | Değer |
|---|---|
| Servis adı | `config-server` |
| Port | `8888` |
| Temel bağımlılık | `spring-cloud-config-server` |
| Profil | `native` (Git yerine dosya sistemi) |
| Kaynak | `configs/` klasörü |

Servislerin her biri kendi `application.yml`'inde yalnızca şu iki satırı taşır:
```yaml
spring:
  application:
    name: <servis-adı>
  config:
    import: "optional:configserver:http://localhost:8888"
```
Gerisi — datasource, güvenlik, Kafka, Eureka, port — config-server'dan çekilir.

---

## 2. Çalışma Mantığı

### Native profil
Config-server `native` modda çalışır: konfigürasyonu bir Git deposundan değil, yerel dosya sisteminden okur. Arama yolları:
```
file:./configs/
file:./configs/{application}/
file:../configs/
file:../configs/{application}/
```
`{application}` istekte bulunan servisin adıyla değiştirilir; böylece hem paylaşımlı hem servise özel dosyalar bulunur.

### Konfigürasyon çözümleme sırası
Bir servis (örn. `product-catalog-service`, `dev` profili) açılışta config-server'dan ayarlarını ister. Sunucu şu dosyaları katman katman birleştirir (spesifik olan geneli ezer):

1. `configs/application.yaml` — **tüm servisler için ortak** ayarlar (ör. actuator endpoint'leri).
2. `configs/{servis}/application.yml` — servis kimliği.
3. `configs/{servis}/application-dev.yml` — servisin `dev` ortamına özel ayarları (port, DB, Keycloak, Kafka…).

### İstek biçimi
Config-server şu HTTP deseniyle sorgulanır:
```
GET http://localhost:8888/{application}/{profile}
GET http://localhost:8888/product-catalog-service/dev
```
Yanıt, ilgili servis için birleştirilmiş property kümesidir.

---

## 3. Yönetilen Servisler

`configs/` altında her mikroservis için bir klasör bulunur:

```
configs/
├── application.yaml            # ortak ayarlar
├── product-catalog-service/
├── ticket-service/
├── usage-service/
├── order-service/
├── subscription-service/
├── payment-service/
├── billing-service/
├── customer-service/
├── identity-service/
├── notification-service/
├── bff-server/
├── gateway-server/
└── discovery-server/
```

---

## 4. Mimarideki Rolü

Config-server, platformun **açılış sırasındaki temel taşıdır**: diğer servisler başlarken ayarlarını buradan çektiği için önce ayağa kalkmış olmalıdır. Sağladığı faydalar:

- **Tek kaynak**: port, DB bağlantısı, Keycloak issuer, Kafka broker gibi tekrar eden ayarlar tek yerde tutulur; 13+ serviste kopyala-yapıştır ve tutarsızlık ortadan kalkar.
- **Ortam ayrımı**: `-dev`, (ileride `-prod`) gibi profil dosyalarıyla aynı servis farklı ortamlarda farklı ayarlarla çalışır; kod değişmez.
- **Merkezi değişiklik**: örneğin Keycloak realm adresi değişince tek dosya güncellenir.

> Not: `native` profil yerel geliştirme içindir. Üretimde tipik olarak Git destekli bir config kaynağına ve şifreli property'lere (encrypt/decrypt) geçilir.
