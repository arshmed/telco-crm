ALTER TABLE customers ADD COLUMN customer_no VARCHAR(20);

CREATE UNIQUE INDEX idx_customers_customer_no ON customers(customer_no) WHERE deleted_at IS NULL;

WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at) AS rn
    FROM customers
)
UPDATE customers c SET customer_no = 'C-' || LPAD(CAST(n.rn AS TEXT), 6, '0')
FROM numbered n WHERE c.id = n.id;

ALTER TABLE customers ALTER COLUMN customer_no SET NOT NULL;
