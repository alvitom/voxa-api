ALTER TABLE users
    ADD COLUMN delete_verification_code VARCHAR(255) AFTER password_reset_token_expired_at,
    ADD COLUMN delete_verification_code_expired_at TIMESTAMP AFTER delete_verification_code;