CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    company_name VARCHAR(150),
    document VARCHAR(30),
    email VARCHAR(160),
    phone VARCHAR(30),
    active BOOLEAN NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    CONSTRAINT fk_customers_app_users
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
);