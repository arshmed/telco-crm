CREATE TABLE audit_log (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(50)  NOT NULL,
    action          VARCHAR(20)  NOT NULL,
    changed_by      VARCHAR(100),
    changed_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    old_values      JSONB,
    new_values      JSONB
);

CREATE INDEX idx_audit_log_entity   ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_changed  ON audit_log(changed_at);

ALTER TABLE customers ALTER COLUMN identity_number TYPE VARCHAR(255);
