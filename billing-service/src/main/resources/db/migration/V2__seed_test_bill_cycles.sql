UPDATE bill_cycle
SET next_run_date = CURRENT_DATE
WHERE next_run_date > CURRENT_DATE;

INSERT INTO bill_cycle (id, customer_id, subscription_id, tariff_code, day_of_month, next_run_date, active, created_at)
VALUES
    ('b1000000-0000-0000-0000-000000000002',
     '3a0c85cb-83e7-4442-b41c-0275b5ee0853',
     'a1000000-0000-0000-0000-000000000002',
     'TARIFF-002',
     15,
     CURRENT_DATE,
     TRUE,
     NOW()),
    ('b1000000-0000-0000-0000-000000000003',
     '3a0c85cb-83e7-4442-b41c-0275b5ee0853',
     'a1000000-0000-0000-0000-000000000003',
     'TARIFF-001',
     28,
     CURRENT_DATE,
     TRUE,
     NOW());
