ALTER TABLE customers
ADD COLUMN company_id BIGINT;

UPDATE customers c
SET company_id = u.company_id
FROM app_users u
WHERE c.user_id = u.id;

ALTER TABLE customers
ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE customers
ADD CONSTRAINT fk_customers_companies
    FOREIGN KEY (company_id)
    REFERENCES companies(id);

CREATE INDEX idx_customers_company_id
    ON customers(company_id);