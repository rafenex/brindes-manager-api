ALTER TABLE companies
    ADD COLUMN address VARCHAR(255),
    ADD COLUMN email VARCHAR(160),
    ADD COLUMN phone VARCHAR(30),
    ADD COLUMN logo BYTEA,
    ADD COLUMN logo_content_type VARCHAR(100);