-- Fix: unique constraint should be (code, channel), not just (code)
-- This allows the same template code for different channels (EMAIL, SMS, PUSH)
ALTER TABLE notification_templates DROP CONSTRAINT IF EXISTS notification_templates_code_key;
ALTER TABLE notification_templates ADD CONSTRAINT uk_notification_templates_code_channel UNIQUE (code, channel);

-- Re-insert SMS templates that were silently skipped due to the broken unique constraint
INSERT INTO notification_templates (code, channel, locale, subject, body_template) VALUES
('CUSTOMER_REGISTERED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, TelcoX ailesine hos geldiniz.'),
('CUSTOMER_KYC_APPROVED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, KYC onayiniz basariyla tamamlanmistir.'),
('CUSTOMER_KYC_REJECTED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, KYC onayiniz reddedilmistir.')
ON CONFLICT (code, channel) DO NOTHING;

-- Add missing order notification templates
INSERT INTO notification_templates (code, channel, locale, subject, body_template) VALUES
('ORDER_CREATED', 'EMAIL', 'tr', 'Siparisiniz Alindi', 'Sayin {{firstName}} {{lastName}}, siparisiniz basariyla olusturulmustur. Siparis numaraniz: {{orderId}}. Toplam tutar: {{totalAmount}} {{currency}}.'),
('ORDER_CREATED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, siparisiniz olusturulmustur. Siparis no: {{orderId}}.'),
('ORDER_CONFIRMED', 'EMAIL', 'tr', 'Siparisiniz Onaylandi', 'Sayin {{firstName}} {{lastName}}, siparisiniz onaylanmistir. Aboneliginiz aktif edilmistir. Siparis numaraniz: {{orderId}}.'),
('ORDER_CONFIRMED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, siparisiniz onaylanmistir. Aboneliginiz aktif edilmistir.'),
('ORDER_CANCELLED', 'EMAIL', 'tr', 'Siparisiniz Iptal Edildi', 'Sayin {{firstName}} {{lastName}}, siparisiniz iptal edilmistir. Iptal nedeni: {{cancellationReason}}. Siparis numaraniz: {{orderId}}.'),
('ORDER_CANCELLED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, siparisiniz iptal edilmistir. Neden: {{cancellationReason}}.')
ON CONFLICT (code, channel) DO NOTHING;
