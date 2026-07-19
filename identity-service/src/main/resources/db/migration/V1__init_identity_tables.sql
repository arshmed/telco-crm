CREATE TABLE users (
    id                UUID          PRIMARY KEY,
    customer_id       UUID,
    username          VARCHAR(255)  NOT NULL UNIQUE,
    email             VARCHAR(255)  NOT NULL UNIQUE,
    full_name         VARCHAR(255)  NOT NULL,
    phone_number      VARCHAR(255),
    status            VARCHAR(255)  NOT NULL,
    keycloak_user_id  VARCHAR(255),
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL,
    version           BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE roles (
    id           UUID          PRIMARY KEY,
    name         VARCHAR(255)  NOT NULL UNIQUE,
    description  VARCHAR(500),
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP     NOT NULL
);

CREATE TABLE permissions (
    id           UUID          PRIMARY KEY,
    name         VARCHAR(255)  NOT NULL UNIQUE,
    description  VARCHAR(500),
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP     NOT NULL
);

CREATE TABLE user_roles (
    id           UUID          PRIMARY KEY,
    user_id      UUID          NOT NULL REFERENCES users(id),
    role_id      UUID          NOT NULL REFERENCES roles(id),
    assigned_at  TIMESTAMP     NOT NULL,
    assigned_by  VARCHAR(255)  NOT NULL,
    UNIQUE (user_id, role_id)
);

CREATE TABLE role_permissions (
    id             UUID          PRIMARY KEY,
    role_id        UUID          NOT NULL REFERENCES roles(id),
    permission_id  UUID          NOT NULL REFERENCES permissions(id),
    assigned_at    TIMESTAMP     NOT NULL,
    assigned_by    VARCHAR(255)  NOT NULL,
    UNIQUE (role_id, permission_id)
);

CREATE TABLE identity_audit_logs (
    id            UUID          PRIMARY KEY,
    entity_type   VARCHAR(255)  NOT NULL,
    entity_id     UUID          NOT NULL,
    action        VARCHAR(255)  NOT NULL,
    detail        VARCHAR(500),
    performed_by  VARCHAR(255)  NOT NULL,
    created_at    TIMESTAMP     NOT NULL
);

CREATE TABLE outbox (
    id              UUID  PRIMARY KEY,
    aggregate_type  VARCHAR(255),
    aggregate_id    VARCHAR(255),
    topic           VARCHAR(255),
    payload         TEXT,
    created_at      TIMESTAMP
);

CREATE TABLE processed_events (
    event_id      UUID PRIMARY KEY,
    processed_at  TIMESTAMP
);

CREATE INDEX idx_user_roles_user_id          ON user_roles(user_id);
CREATE INDEX idx_role_permissions_role_id    ON role_permissions(role_id);
CREATE INDEX idx_identity_audit_logs_entity_id ON identity_audit_logs(entity_id);
CREATE INDEX idx_outbox_created_at           ON outbox(created_at);

-- Seed data: sistemdeki 7 aktör rolü
INSERT INTO roles (id, name, description, created_at, updated_at) VALUES
    (gen_random_uuid(), 'CUSTOMER',           'Kendi hesabını, faturalarını ve taleplerini self-servis olarak yönetir', now(), now()),
    (gen_random_uuid(), 'CALL_CENTER_AGENT',  'Müşteri taleplerini karşılar, sipariş ve destek talebi oluşturur', now(), now()),
    (gen_random_uuid(), 'FIELD_DEALER',       'Sahada yeni müşteri kaydı ve sipariş oluşturma işlemlerini yürütür', now(), now()),
    (gen_random_uuid(), 'MARKETING_MANAGER',  'Kampanya ve ürün kataloğu yönetimi yapar', now(), now()),
    (gen_random_uuid(), 'SYSTEM_ADMIN',       'Sistem genelinde kullanıcı, rol ve yetki yönetimini yürütür', now(), now()),
    (gen_random_uuid(), 'BILLING_OPERATOR',   'Fatura oluşturma, düzeltme ve tahsilat işlemlerini yönetir', now(), now()),
    (gen_random_uuid(), 'SYSTEM_SERVICE',     'Servisler arası otomatik (service-to-service) işlemleri gerçekleştirir', now(), now());
