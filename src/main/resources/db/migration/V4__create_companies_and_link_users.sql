CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

INSERT INTO companies (
    name,
    active,
    created_at
)
VALUES (
    'Empresa Inicial',
    TRUE,
    CURRENT_TIMESTAMP
);

ALTER TABLE app_users
ADD COLUMN company_id BIGINT;

UPDATE app_users
SET company_id = (
    SELECT id
    FROM companies
    WHERE name = 'Empresa Inicial'
    ORDER BY id
    LIMIT 1
);

ALTER TABLE app_users
ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE app_users
ADD CONSTRAINT fk_app_users_companies
    FOREIGN KEY (company_id)
    REFERENCES companies(id);

CREATE INDEX idx_app_users_company_id
    ON app_users(company_id);