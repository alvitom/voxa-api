ALTER TABLE users
    DROP COLUMN phone_number,
    ADD CONSTRAINT `email_unique` UNIQUE (`email`);