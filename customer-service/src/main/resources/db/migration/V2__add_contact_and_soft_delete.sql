ALTER TABLE customers
    ADD COLUMN email      VARCHAR(150),
    ADD COLUMN phone      VARCHAR(20),
    ADD COLUMN deleted_at TIMESTAMP;

CREATE INDEX idx_customers_email      ON customers(email);
CREATE INDEX idx_customers_deleted_at ON customers(deleted_at);
