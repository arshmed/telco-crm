CREATE TABLE tickets (
    id             UUID          PRIMARY KEY,
    customer_id    UUID          NOT NULL,
    category       VARCHAR(255)  NOT NULL,
    priority       VARCHAR(255)  NOT NULL,
    status         VARCHAR(255)  NOT NULL,
    description    VARCHAR(2000) NOT NULL,
    assigned_team  VARCHAR(100)  NOT NULL,
    sla_due_at     TIMESTAMP     NOT NULL,
    sla_breached   BOOLEAN       NOT NULL DEFAULT FALSE,
    resolution     VARCHAR(1000),
    resolved_at    TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    version        BIGINT        NOT NULL
);

CREATE TABLE ticket_comments (
    id          UUID          PRIMARY KEY,
    ticket_id   UUID          NOT NULL REFERENCES tickets(id),
    author_id   VARCHAR(255)  NOT NULL,
    body        VARCHAR(2000) NOT NULL,
    created_at  TIMESTAMP     NOT NULL
);

CREATE TABLE outbox (
    id              UUID  PRIMARY KEY,
    aggregate_type  VARCHAR(255),
    aggregate_id    VARCHAR(255),
    topic           VARCHAR(255),
    payload         TEXT,
    created_at      TIMESTAMP
);

CREATE INDEX idx_tickets_customer_id       ON tickets(customer_id);
CREATE INDEX idx_ticket_comments_ticket_id ON ticket_comments(ticket_id);
CREATE INDEX idx_outbox_created_at         ON outbox(created_at);

CREATE INDEX idx_tickets_sla_scan ON tickets(sla_due_at) WHERE sla_breached = FALSE;
