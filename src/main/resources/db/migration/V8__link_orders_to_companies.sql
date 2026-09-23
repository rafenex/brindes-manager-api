ALTER TABLE customer_orders
ADD COLUMN company_id BIGINT;

UPDATE customer_orders o
SET company_id = c.company_id
FROM customers c
WHERE o.customer_id = c.id;

ALTER TABLE customer_orders
ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE customer_orders
ADD CONSTRAINT fk_customer_orders_companies
    FOREIGN KEY (company_id)
    REFERENCES companies(id);

CREATE INDEX idx_customer_orders_company_id
    ON customer_orders(company_id);