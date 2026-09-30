UPDATE customers
SET company_name = name
WHERE company_name IS NULL
   OR TRIM(company_name) = '';

ALTER TABLE customers
ALTER COLUMN company_name SET NOT NULL;

ALTER TABLE customers
ALTER COLUMN name DROP NOT NULL;

ALTER TABLE customers
ADD COLUMN address VARCHAR(255);