CREATE TABLE payments (
    id              UUID          PRIMARY KEY,
    order_id        UUID          NOT NULL UNIQUE,
    customer_id     UUID          NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL,
    method          VARCHAR(255)  NOT NULL,
    status          VARCHAR(255)  NOT NULL,
    external_ref    VARCHAR(255),
    failure_reason  VARCHAR(500),
    paid_at         TIMESTAMP,
    retry_count     INTEGER       NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE payment_attempts (
    id            UUID     PRIMARY KEY,
    payment_id    UUID     NOT NULL REFERENCES payments(id),
    attempt_no    INTEGER  NOT NULL,
    response      TEXT,
    attempted_at  TIMESTAMP NOT NULL
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

CREATE INDEX idx_payments_customer_id       ON payments(customer_id);
CREATE INDEX idx_payments_status            ON payments(status);
CREATE INDEX idx_payment_attempts_payment_id ON payment_attempts(payment_id);
CREATE INDEX idx_outbox_created_at          ON outbox(created_at);
