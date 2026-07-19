DROP INDEX IF EXISTS idx_outbox_status_created_at;

ALTER TABLE outbox
    DROP COLUMN status,
    DROP COLUMN sent_at,
    DROP COLUMN retry_count,
    DROP COLUMN last_retry;
