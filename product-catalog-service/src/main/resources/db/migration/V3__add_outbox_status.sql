ALTER TABLE outbox
    ADD COLUMN status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN sent_at     TIMESTAMPTZ,
    ADD COLUMN retry_count INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN last_retry  TIMESTAMPTZ;

CREATE INDEX idx_outbox_status_created_at ON outbox(status, created_at);
