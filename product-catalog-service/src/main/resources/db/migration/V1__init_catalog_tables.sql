CREATE TABLE tariffs (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(50)   NOT NULL,
    version          INTEGER       NOT NULL DEFAULT 1,
    "current"        BOOLEAN       NOT NULL DEFAULT TRUE,
    name             VARCHAR(150)  NOT NULL,
    type             VARCHAR(20)   NOT NULL,
    segment          VARCHAR(20)   NOT NULL DEFAULT 'ALL',
    monthly_fee      NUMERIC(12,2) NOT NULL,
    currency         VARCHAR(3)    NOT NULL DEFAULT 'TRY',
    minutes_included INTEGER,
    sms_included     INTEGER,
    data_mb_included INTEGER,
    status           VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    effective_from   DATE          NOT NULL,
    effective_to     DATE,
    deleted          BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tariffs_code_version UNIQUE (code, version)
);

CREATE TABLE addons (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(50)   NOT NULL UNIQUE,
    name          VARCHAR(150)  NOT NULL,
    type          VARCHAR(20)   NOT NULL,
    price         NUMERIC(12,2) NOT NULL,
    currency      VARCHAR(3)    NOT NULL DEFAULT 'TRY',
    validity_days INTEGER,
    deleted       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE tariff_addon (
    tariff_id UUID NOT NULL REFERENCES tariffs(id),
    addon_id  UUID NOT NULL REFERENCES addons(id),
    PRIMARY KEY (tariff_id, addon_id)
);

CREATE INDEX idx_tariffs_code          ON tariffs(code);
CREATE INDEX idx_tariffs_status        ON tariffs(status);
CREATE INDEX idx_tariffs_current       ON tariffs("current");
CREATE INDEX idx_addons_type           ON addons(type);
CREATE INDEX idx_tariff_addon_addon_id ON tariff_addon(addon_id);
