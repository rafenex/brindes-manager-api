CREATE UNIQUE INDEX uk_app_users_email_upper ON app_users (UPPER(email));

DROP INDEX uk_app_users_email_lower;
