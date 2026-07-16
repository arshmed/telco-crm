-- findByCodeAndChannel kanal başına bir satır bekliyor; UNIQUE(code) bunu imkansız
-- kılıyordu ve V1'in ON CONFLICT (code) DO NOTHING'i SMS şablonlarını sessizce eledi.
ALTER TABLE notification_templates DROP CONSTRAINT notification_templates_code_key;
ALTER TABLE notification_templates ADD CONSTRAINT uq_notification_templates_code_channel_locale
    UNIQUE (code, channel, locale);

-- V1'de yazılmış ama constraint yüzünden hiç insert edilememiş SMS şablonları.
INSERT INTO notification_templates (code, channel, locale, subject, body_template) VALUES
('CUSTOMER_REGISTERED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, TelcoX ailesine hos geldiniz.'),
('CUSTOMER_KYC_APPROVED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, KYC onayiniz basariyla tamamlanmistir.'),
('CUSTOMER_KYC_REJECTED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, KYC onayiniz reddedilmistir.')
ON CONFLICT (code, channel, locale) DO NOTHING;

INSERT INTO notification_templates (code, channel, locale, subject, body_template) VALUES
('TICKET_OPENED', 'EMAIL', 'tr', 'Talebiniz Alındı — {{ticketId}}', 'Sayın {{firstName}} {{lastName}}, {{category}} kaydınız oluşturuldu. Talep numaranız: {{ticketId}}. Öncelik: {{priority}}. İlgili ekip: {{assignedTeam}}. Tahmini yanıt zamanı: {{slaDueAt}}'),
('TICKET_OPENED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, talebiniz alindi. Talep no: {{ticketId}}'),
('TICKET_RESOLVED', 'EMAIL', 'tr', 'Talebiniz Çözümlendi — {{ticketId}}', 'Sayın {{firstName}} {{lastName}}, {{ticketId}} numaralı talebiniz çözümlenmiştir. Çözüm: {{resolution}}'),
('TICKET_RESOLVED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, {{ticketId}} numarali talebiniz cozumlendi.')
ON CONFLICT (code, channel, locale) DO NOTHING;
