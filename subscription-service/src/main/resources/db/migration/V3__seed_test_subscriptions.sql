-- Test subscription: Mehmet Yılmaz - Süper Paket (ACTIVE)
INSERT INTO subscriptions (id, customer_id, customer_no, msisdn, tariff_code, status, activated_at, created_at, updated_at, version)
VALUES (
    'a1000000-0000-0000-0000-000000000001',
    '3a0c85cb-83e7-4442-b41c-0275b5ee0853',
    'C-100001',
    '5321234567',
    'SUPER-299',
    'ACTIVE',
    NOW(),
    NOW(),
    NOW(),
    0
);

-- Test subscription: Ayşe Demir - Ekonomik Paket (PENDING)
INSERT INTO subscriptions (id, customer_id, customer_no, msisdn, tariff_code, status, created_at, updated_at, version)
VALUES (
    'a1000000-0000-0000-0000-000000000002',
    '5c2e3f4a-6b7c-8d9e-0f1a-2b3c4d5e6f7a',
    'C-100002',
    '5359876543',
    'ECONOMY-149',
    'PENDING',
    NOW(),
    NOW(),
    0
);

-- Test subscription: TechCorp - Kurumsal Paket (ACTIVE)
INSERT INTO subscriptions (id, customer_id, customer_no, msisdn, tariff_code, status, activated_at, created_at, updated_at, version)
VALUES (
    'a1000000-0000-0000-0000-000000000003',
    '6d3f4a5b-7c8d-9e0f-1a2b-3c4d5e6f7a8b',
    'C-100003',
    '5329876543',
    'CORP-899',
    'ACTIVE',
    NOW(),
    NOW(),
    NOW(),
    0
);

-- Test subscription: TechCorp - Süper Paket (SUSPENDED)
INSERT INTO subscriptions (id, customer_id, customer_no, msisdn, tariff_code, status, activated_at, suspended_at, created_at, updated_at, version)
VALUES (
    'a1000000-0000-0000-0000-000000000004',
    '6d3f4a5b-7c8d-9e0f-1a2b-3c4d5e6f7a8b',
    'C-100003',
    '5321112233',
    'SUPER-299',
    'SUSPENDED',
    NOW() - INTERVAL '30 days',
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '30 days',
    NOW(),
    0
);
