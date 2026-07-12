CREATE TABLE IF NOT EXISTS `users` (
  `id` VARCHAR(40) NOT NULL,
  `email` VARCHAR(255) NOT NULL,
  `phone_number` VARCHAR(15) NOT NULL,
  `username` VARCHAR(30) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `refresh_token` VARCHAR(255),
  `password_reset_token` VARCHAR(255),
  `password_reset_expired` VARCHAR(45),
  `created_at` TIMESTAMP NOT NULL,
  `updated_at` TIMESTAMP NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username_unique` (`username`)
) ENGINE = InnoDB;