ALTER TABLE users
    MODIFY COLUMN is_enabled BOOLEAN AFTER is_credentials_non_expired;