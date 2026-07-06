ALTER TABLE customers ADD COLUMN identity_number_hash VARCHAR(64);

CREATE UNIQUE INDEX idx_customers_identity_number_hash ON customers(identity_number_hash);
