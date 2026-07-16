-- Test customer: Mehmet Yılmaz (bireysel, KYC onaylı)
INSERT INTO customers (id, type, first_name, last_name, identity_number, date_of_birth, status, email, phone, customer_no, created_at, updated_at)
VALUES (
    '3a0c85cb-83e7-4442-b41c-0275b5ee0853',
    'INDIVIDUAL',
    'Mehmet',
    'Yılmaz',
    '12345678901',
    '1990-05-15',
    'ACTIVE',
    'mehmet.yilmaz@email.com',
    '5321234567',
    'C-100001',
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;

-- Test address
INSERT INTO addresses (id, customer_id, line1, city, district, postal_code, is_default, created_at)
VALUES (
    '4b1d2e3f-5a6b-7c8d-9e0f-1a2b3c4d5e6f',
    '3a0c85cb-83e7-4442-b41c-0275b5ee0853',
    'Atatürk Caddesi No: 123',
    'İstanbul',
    'Kadıköy',
    '34710',
    TRUE,
    NOW()
)
ON CONFLICT DO NOTHING;

-- Test customer 2: Ayşe Demir (bireysel, KYC bekliyor)
INSERT INTO customers (id, type, first_name, last_name, identity_number, date_of_birth, status, email, phone, customer_no, created_at, updated_at)
VALUES (
    '5c2e3f4a-6b7c-8d9e-0f1a-2b3c4d5e6f7a',
    'INDIVIDUAL',
    'Ayşe',
    'Demir',
    '98765432109',
    '1985-08-20',
    'PENDING',
    'ayse.demir@email.com',
    '5359876543',
    'C-100002',
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;

-- Test customer 3: TechCorp Ltd. (kurumsal, KYC onaylı)
INSERT INTO customers (id, type, first_name, last_name, identity_number, date_of_birth, status, email, phone, customer_no, company_name, tax_office, created_at, updated_at)
VALUES (
    '6d3f4a5b-7c8d-9e0f-1a2b-3c4d5e6f7a8b',
    'CORPORATE',
    'Ali',
    'Kaya',
    '1234567890',
    '1980-01-01',
    'ACTIVE',
    'info@techcorp.com',
    '2125551234',
    'C-100003',
    'TechCorp Ltd.',
    'Kadıköy',
    NOW(),
    NOW()
)
ON CONFLICT DO NOTHING;
