ALTER TABLE users
	MODIFY COLUMN is_account_non_expired BOOLEAN DEFAULT TRUE,
    MODIFY COLUMN is_account_non_locked BOOLEAN DEFAULT TRUE,
    MODIFY COLUMN is_credentials_non_expired BOOLEAN DEFAULT TRUE,
    MODIFY COLUMN is_enabled BOOLEAN DEFAULT TRUE,
    ADD COLUMN verification_token VARCHAR(255) AFTER is_enabled,
    ADD COLUMN verification_token_expired_at TIMESTAMP AFTER verification_token,
    ADD COLUMN refresh_token VARCHAR(255) AFTER verification_token_expired_at,
    ADD COLUMN refresh_token_expired_at TIMESTAMP AFTER refresh_token,
    ADD COLUMN password_reset_token VARCHAR(255) AFTER refresh_token_expired_at,
    ADD COLUMN password_reset_token_expired_at TIMESTAMP AFTER password_reset_token;