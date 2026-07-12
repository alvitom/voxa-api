ALTER TABLE users
    ADD COLUMN is_account_non_expired BOOLEAN AFTER password,
    ADD COLUMN is_account_non_locked BOOLEAN AFTER is_account_non_expired,
    ADD COLUMN is_credentials_non_expired BOOLEAN AFTER is_account_non_locked,
    ADD COLUMN is_enabled BOOLEAN,
    DROP COLUMN status,
    DROP COLUMN refresh_token,
    DROP COLUMN password_reset_token,
    DROP COLUMN password_reset_expired;