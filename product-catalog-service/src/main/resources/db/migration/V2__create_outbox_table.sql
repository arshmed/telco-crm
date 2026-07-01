CREATE TABLE outbox (
    id            UUID         PRIMARY KEY,
    aggregatetype VARCHAR(50)  NOT NULL,
    aggregateid   VARCHAR(100) NOT NULL,
    type          VARCHAR(100) NOT NULL,
    payload       TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_created_at ON outbox(created_at);
