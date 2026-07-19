ALTER TABLE outbox DROP COLUMN error_message;
ALTER TABLE outbox DROP COLUMN retry_count;
ALTER TABLE outbox DROP COLUMN status;
ALTER TABLE outbox DROP COLUMN processed_at;

ALTER TABLE outbox RENAME COLUMN event_type TO topic;

DROP INDEX IF EXISTS idx_outbox_status_retry;
CREATE INDEX idx_outbox_created_at ON outbox(created_at);
