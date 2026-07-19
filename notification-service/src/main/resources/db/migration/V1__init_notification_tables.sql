CREATE TABLE notification_templates (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(100) NOT NULL UNIQUE,
    channel         VARCHAR(20)  NOT NULL,
    locale          VARCHAR(10)  NOT NULL DEFAULT 'tr',
    subject         VARCHAR(500),
    body_template   TEXT         NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE TABLE notifications (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    template_code   VARCHAR(100) NOT NULL,
    channel         VARCHAR(20)  NOT NULL,
    subject         VARCHAR(500),
    body            TEXT         NOT NULL,
    payload_json    JSONB,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    sent_at         TIMESTAMP,
    error_message   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE user_communication_preferences (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    channel         VARCHAR(20)  NOT NULL,
    opt_in          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    UNIQUE (user_id, channel)
);

CREATE TABLE outbox (
    id              UUID         PRIMARY KEY,
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    topic           VARCHAR(100) NOT NULL,
    payload         TEXT         NOT NULL,
    error_message   TEXT,
    retry_count     INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ
);

CREATE INDEX idx_notifications_user_id     ON notifications(user_id);
CREATE INDEX idx_notifications_status      ON notifications(status);
CREATE INDEX idx_notifications_created_at  ON notifications(created_at);
CREATE INDEX idx_notification_templates_code ON notification_templates(code);
CREATE INDEX idx_user_prefs_user_id        ON user_communication_preferences(user_id);
CREATE INDEX idx_outbox_status_retry       ON outbox (status, retry_count, created_at);

INSERT INTO notification_templates (code, channel, locale, subject, body_template) VALUES
('CUSTOMER_REGISTERED', 'EMAIL', 'tr', 'Hoş Geldiniz!', 'Sayın {{firstName}} {{lastName}}, TelcoX ailesine hoş geldiniz. Müşteri numaranız: {{customerId}}'),
('CUSTOMER_REGISTERED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, TelcoX ailesine hos geldiniz.'),
('CUSTOMER_KYC_APPROVED', 'EMAIL', 'tr', 'KYC Onaylandı', 'Sayın {{firstName}} {{lastName}}, kimlik doğrulama süreciniz başarıyla onaylanmıştır.'),
('CUSTOMER_KYC_APPROVED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, KYC onayiniz basariyla tamamlanmistir.'),
('CUSTOMER_KYC_REJECTED', 'EMAIL', 'tr', 'KYC Reddedildi', 'Sayın {{firstName}} {{lastName}}, kimlik doğrulama süreciniz reddedilmiştir. Lütfen müşteri hizmetleri ile iletişime geçiniz.'),
('CUSTOMER_KYC_REJECTED', 'SMS', 'tr', NULL, 'Sayin {{firstName}} {{lastName}}, KYC onayiniz reddedilmistir.'),
('CUSTOMER_UPDATED', 'EMAIL', 'tr', 'Bilgileriniz Güncellendi', 'Sayın {{firstName}} {{lastName}}, müşteri bilgileriniz başarıyla güncellenmiştir.')
ON CONFLICT (code) DO NOTHING;
