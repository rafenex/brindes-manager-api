-- HU-003 v2: execution requires specific authorization for the confirmed environment.
ALTER TABLE customers
    ADD COLUMN postal_code TEXT,
    ADD COLUMN street TEXT,
    ADD COLUMN address_number TEXT,
    ADD COLUMN address_complement TEXT,
    ADD COLUMN neighborhood TEXT,
    ADD COLUMN city TEXT,
    ADD COLUMN state TEXT;

ALTER TABLE customers DROP COLUMN address;
