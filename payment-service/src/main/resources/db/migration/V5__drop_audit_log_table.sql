-- audit_log tablosu ve onu besleyen PaymentAuditListener hiçbir servis sınıfından
-- çağrılmıyordu (yalnızca kendi birim testi tarafından tetikleniyordu, üretim akışına
-- hiç bağlanmamıştı). Gerçek denetim mekanizması payment_audit_logs / PaymentAuditService'tir
-- (bkz. V3 migration'ı). Kullanılmayan tablo kaldırılıyor.
DROP TABLE IF EXISTS audit_log;
