ALTER TABLE categories
ADD COLUMN company_id BIGINT;

UPDATE categories
SET company_id = (
    SELECT MIN(id)
    FROM companies
);

ALTER TABLE categories
ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE categories
ADD CONSTRAINT fk_categories_companies
    FOREIGN KEY (company_id)
    REFERENCES companies(id);

ALTER TABLE categories
DROP CONSTRAINT categories_name_key;

CREATE UNIQUE INDEX uk_categories_company_name
    ON categories(company_id, LOWER(name));

CREATE INDEX idx_categories_company_id
    ON categories(company_id);