-- Test tariff: Süper Paket (100DK + 50SMS + 10GB, aylık 299₺)
INSERT INTO tariffs (id, code, version, "current", name, type, segment, monthly_fee, currency, minutes_included, sms_included, data_mb_included, status, effective_from, effective_to, deleted, created_at, updated_at)
VALUES (
    '7e4a5b6c-8d9e-0f1a-2b3c-4d5e6f7a8b9c',
    'SUPER-299',
    1,
    TRUE,
    'Süper Paket',
    'POSTPAID',
    'INDIVIDUAL',
    299.00,
    'TRY',
    100,
    50,
    10240,
    'ACTIVE',
    '2024-01-01',
    NULL,
    FALSE,
    NOW(),
    NOW()
);

-- Test tariff: Ekonomik Paket (50DK + 25SMS + 5GB, aylık 149₺)
INSERT INTO tariffs (id, code, version, "current", name, type, segment, monthly_fee, currency, minutes_included, sms_included, data_mb_included, status, effective_from, effective_to, deleted, created_at, updated_at)
VALUES (
    '8f5b6c7d-9e0f-1a2b-3c4d-5e6f7a8b9c0d',
    'ECONOMY-149',
    1,
    TRUE,
    'Ekonomik Paket',
    'POSTPAID',
    'INDIVIDUAL',
    149.00,
    'TRY',
    50,
    25,
    5120,
    'ACTIVE',
    '2024-01-01',
    NULL,
    FALSE,
    NOW(),
    NOW()
);

-- Test tariff: Kurumsal Paket (500DK + 200SMS + 50GB, aylık 899₺)
INSERT INTO tariffs (id, code, version, "current", name, type, segment, monthly_fee, currency, minutes_included, sms_included, data_mb_included, status, effective_from, effective_to, deleted, created_at, updated_at)
VALUES (
    '9a6c7d8e-0f1a-2b3c-4d5e-6f7a8b9c0d1e',
    'CORP-899',
    1,
    TRUE,
    'Kurumsal Paket',
    'POSTPAID',
    'CORPORATE',
    899.00,
    'TRY',
    500,
    200,
    51200,
    'ACTIVE',
    '2024-01-01',
    NULL,
    FALSE,
    NOW(),
    NOW()
);
