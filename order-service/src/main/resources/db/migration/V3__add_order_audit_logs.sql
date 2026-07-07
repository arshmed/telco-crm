CREATE TABLE order_audit_logs (
    id            UUID          PRIMARY KEY,
    order_id      UUID          NOT NULL,
    order_status  VARCHAR(255)  NOT NULL,
    saga_step     VARCHAR(255)  NOT NULL,
    detail        VARCHAR(500),
    performed_by  VARCHAR(255)  NOT NULL,
    created_at    TIMESTAMP     NOT NULL
);

CREATE INDEX idx_order_audit_logs_order_id ON order_audit_logs(order_id);
