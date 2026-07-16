ALTER TABLE payments ADD COLUMN payment_request_id VARCHAR(255) UNIQUE;
ALTER TABLE payments ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN next_retry_at TIMESTAMP;

CREATE TABLE payment_audit_logs (
    id              UUID          PRIMARY KEY,
    payment_id      UUID          NOT NULL,
    payment_status  VARCHAR(255)  NOT NULL,
    detail          VARCHAR(500),
    performed_by    VARCHAR(255)  NOT NULL,
    created_at      TIMESTAMP     NOT NULL
);

CREATE INDEX idx_payment_audit_logs_payment_id ON payment_audit_logs(payment_id);
