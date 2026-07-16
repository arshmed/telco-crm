CREATE TABLE subscriptions (
    id              UUID PRIMARY KEY,
    customer_id     UUID NOT NULL,
    customer_no     VARCHAR(20),
    msisdn          VARCHAR(15),
    tariff_code     VARCHAR(50) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    activated_at    TIMESTAMP,
    suspended_at    TIMESTAMP,
    terminated_at   TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_subscriptions_customer_id ON subscriptions(customer_id);
CREATE INDEX idx_subscriptions_customer_no ON subscriptions(customer_no);
CREATE INDEX idx_subscriptions_msisdn ON subscriptions(msisdn);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);

CREATE TABLE outbox (
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    topic           VARCHAR(255) NOT NULL,
    payload         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE processed_events (
    event_id        UUID PRIMARY KEY,
    processed_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
