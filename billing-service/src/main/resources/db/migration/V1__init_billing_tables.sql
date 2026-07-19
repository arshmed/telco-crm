CREATE TABLE invoice (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     UUID          NOT NULL,
    subscription_id UUID          NOT NULL,
    invoice_number  VARCHAR(30)   NOT NULL UNIQUE,
    period_start    DATE          NOT NULL,
    period_end      DATE          NOT NULL,
    sub_total       DECIMAL(12,2) NOT NULL,
    tax_rate        DECIMAL(5,2)  NOT NULL DEFAULT 20.00,
    tax_amount      DECIMAL(12,2) NOT NULL,
    grand_total     DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    due_date        DATE          NOT NULL,
    issued_at       TIMESTAMP,
    paid_at         TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE TABLE invoice_line (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id  UUID          NOT NULL REFERENCES invoice(id),
    description VARCHAR(255)  NOT NULL,
    quantity    INT           NOT NULL DEFAULT 1,
    unit_price  DECIMAL(12,2) NOT NULL,
    line_total  DECIMAL(12,2) NOT NULL
);

CREATE TABLE bill_cycle (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     UUID        NOT NULL,
    subscription_id UUID        NOT NULL,
    tariff_code     VARCHAR(50) NOT NULL,
    day_of_month    INT         NOT NULL DEFAULT 1,
    next_run_date   DATE        NOT NULL,
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE TABLE outbox (
    id            UUID         PRIMARY KEY,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   VARCHAR(100) NOT NULL,
    topic          VARCHAR(100) NOT NULL,
    payload        TEXT         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE processed_events (
    event_id      UUID         PRIMARY KEY,
    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoice_customer_id      ON invoice(customer_id);
CREATE INDEX idx_invoice_subscription_id  ON invoice(subscription_id);
CREATE INDEX idx_invoice_status           ON invoice(status);
CREATE INDEX idx_invoice_period           ON invoice(period_start, period_end);
CREATE INDEX idx_invoice_line_invoice_id  ON invoice_line(invoice_id);
CREATE INDEX idx_bill_cycle_customer_id   ON bill_cycle(customer_id);
CREATE INDEX idx_bill_cycle_next_run      ON bill_cycle(next_run_date, active);
CREATE INDEX idx_outbox_created_at        ON outbox(created_at);
