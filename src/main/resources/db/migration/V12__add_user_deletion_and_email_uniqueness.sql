ALTER TABLE app_users ADD COLUMN deleted_at timestamp;

CREATE UNIQUE INDEX uk_app_users_email_lower ON app_users (LOWER(email));
