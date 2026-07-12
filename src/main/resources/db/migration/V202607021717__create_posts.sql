CREATE TABLE IF NOT EXISTS `posts` (
  `id` VARCHAR(40) NOT NULL,
  `user_id` VARCHAR(40) NOT NULL,
  `caption` TEXT,
  `like_count` BIGINT NOT NULL,
  `comment_count` INT NOT NULL,
  `created_at` TIMESTAMP NOT NULL,
  `updated_at` TIMESTAMP NOT NULL,
  `deleted_at` TIMESTAMP NOT NULL,
  PRIMARY KEY (`id`, `user_id`),
  CONSTRAINT `fk_posts_users`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
) ENGINE = InnoDB;