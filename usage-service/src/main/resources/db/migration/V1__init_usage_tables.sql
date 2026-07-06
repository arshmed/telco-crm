CREATE TABLE quotas (
    id                UUID          PRIMARY KEY,
    subscription_id   UUID          NOT NULL,
    customer_id       UUID          NOT NULL,
    msisdn            VARCHAR(20)   NOT NULL,
    tariff_code       VARCHAR(255)  NOT NULL,
    period_start      DATE          NOT NULL,
    period_end        DATE          NOT NULL,
    minutes_included  INTEGER       NOT NULL,
    sms_included      INTEGER       NOT NULL,
    data_mb_included  INTEGER       NOT NULL,
    minutes_used      INTEGER       NOT NULL DEFAULT 0,
    sms_used          INTEGER       NOT NULL DEFAULT 0,
    data_mb_used      INTEGER       NOT NULL DEFAULT 0,
    aggregated_at     TIMESTAMP,
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL,
    CONSTRAINT uq_quotas_subscription_period UNIQUE (subscription_id, period_start)
);

CREATE TABLE usage_records (
    id               UUID          PRIMARY KEY,
    subscription_id  UUID          NOT NULL,
    type             VARCHAR(10)   NOT NULL,
    quantity         INTEGER       NOT NULL,
    recorded_at      TIMESTAMP     NOT NULL,
    cdr_ref          VARCHAR(255)  NOT NULL
);

CREATE TABLE outbox (
    id              UUID  PRIMARY KEY,
    aggregate_type  VARCHAR(255),
    aggregate_id    VARCHAR(255),
    topic           VARCHAR(255),
    payload         TEXT,
    created_at      TIMESTAMP
);

CREATE TABLE processed_events (
    event_id      UUID PRIMARY KEY,
    processed_at  TIMESTAMP
);

CREATE INDEX idx_quotas_subscription_id        ON quotas(subscription_id);
CREATE INDEX idx_usage_records_subscription_id ON usage_records(subscription_id, recorded_at);
CREATE INDEX idx_outbox_created_at             ON outbox(created_at);
