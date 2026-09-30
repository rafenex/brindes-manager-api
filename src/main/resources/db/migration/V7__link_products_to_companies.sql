ALTER TABLE products
ADD COLUMN company_id BIGINT;

UPDATE products p
SET company_id = c.company_id
FROM categories c
WHERE p.category_id = c.id;

ALTER TABLE products
ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE products
ADD CONSTRAINT fk_products_companies
    FOREIGN KEY (company_id)
    REFERENCES companies(id);

ALTER TABLE products
DROP CONSTRAINT products_reference_key;

CREATE UNIQUE INDEX uk_products_company_reference
    ON products(company_id, LOWER(reference));

CREATE INDEX idx_products_company_id
    ON products(company_id);