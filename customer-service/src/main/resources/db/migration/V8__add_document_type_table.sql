CREATE TABLE document_type (
    code        VARCHAR(50)  PRIMARY KEY,
    label       VARCHAR(100) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INT          NOT NULL DEFAULT 0
);

INSERT INTO document_type (code, label, active, sort_order) VALUES
    ('ID_CARD',              'Kimlik Kartı',          TRUE,  1),
    ('PASSPORT',             'Pasaport',              TRUE,  2),
    ('DRIVING_LICENSE',      'Ehliyet',               TRUE,  3),
    ('RESIDENCE_PERMIT',     'İkametgah',             TRUE,  4),
    ('TAX_PLATE',            'Vergi Levhası',         TRUE,  5),
    ('SIGNATURE_CIRCULAR',   'İmza Sirküleri',        TRUE,  6),
    ('TRADE_REGISTRY',       'Ticaret Sicil',         TRUE,  7),
    ('AUTHORIZATION_LETTER', 'Vekaletname',           TRUE,  8);
