CREATE TABLE customers (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    type            VARCHAR(20)  NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    identity_number VARCHAR(20)  NOT NULL UNIQUE,
    date_of_birth   DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE TABLE addresses (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID         NOT NULL REFERENCES customers(id),
    line1       VARCHAR(255) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    district    VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE TABLE documents (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID         NOT NULL REFERENCES customers(id),
    type        VARCHAR(30)  NOT NULL,
    file_ref    VARCHAR(255) NOT NULL,
    verified_at TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE INDEX idx_customers_identity_number ON customers(identity_number);
CREATE INDEX idx_customers_status          ON customers(status);
CREATE INDEX idx_addresses_customer_id     ON addresses(customer_id);
CREATE INDEX idx_documents_customer_id     ON documents(customer_id);
