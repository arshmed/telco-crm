CREATE TABLE orders (
    id                   UUID          PRIMARY KEY,
    customer_id          UUID          NOT NULL,
    status               VARCHAR(255)  NOT NULL,
    total_amount         NUMERIC(12,2) NOT NULL,
    currency             VARCHAR(3)    NOT NULL,
    payment_id           UUID,
    subscription_id      UUID,
    cancellation_reason  VARCHAR(500),
    deleted              BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP     NOT NULL,
    updated_at           TIMESTAMP     NOT NULL
);

CREATE TABLE order_items (
    id            UUID          PRIMARY KEY,
    order_id      UUID          NOT NULL REFERENCES orders(id),
    product_code  VARCHAR(255)  NOT NULL,
    product_name  VARCHAR(255)  NOT NULL,
    product_type  VARCHAR(255)  NOT NULL,
    quantity      INTEGER       NOT NULL,
    unit_price    NUMERIC(12,2) NOT NULL,
    line_total    NUMERIC(12,2) NOT NULL
);

CREATE TABLE saga_states (
    id             UUID          PRIMARY KEY,
    order_id       UUID          NOT NULL UNIQUE REFERENCES orders(id),
    current_step   VARCHAR(255)  NOT NULL,
    payload        TEXT,
    retry_count    INTEGER       NOT NULL DEFAULT 0,
    error_message  TEXT,
    last_updated   TIMESTAMP     NOT NULL
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

CREATE INDEX idx_orders_customer_id   ON orders(customer_id);
CREATE INDEX idx_orders_status        ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_outbox_created_at    ON outbox(created_at);
