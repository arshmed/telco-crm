CREATE TABLE idempotency_keys (
    id          UUID          PRIMARY KEY,
    key         VARCHAR(255)  NOT NULL,
    order_id    UUID          NOT NULL,
    created_at  TIMESTAMP     NOT NULL,
    CONSTRAINT uk_idempotency_keys_key UNIQUE (key)
);
