ALTER TABLE tickets ADD COLUMN customer_name VARCHAR(200);

UPDATE tickets SET customer_name = 'Bilinmiyor' WHERE customer_name IS NULL;

ALTER TABLE tickets ALTER COLUMN customer_name SET NOT NULL;
