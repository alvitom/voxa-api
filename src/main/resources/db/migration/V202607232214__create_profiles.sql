CREATE TABLE IF NOT EXISTS `profiles` (
    `id` VARCHAR(40) NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `birthday` DATE NOT NULL,
    `gender` VARCHAR(10) NOT NULL,
    `bio` VARCHAR(150),
    `pp_url` VARCHAR(255),
    `post_count` INT DEFAULT 0,
    `follower_count` INT DEFAULT 0,
    `following_count` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_profiles_users`
    FOREIGN KEY (`id`)
    REFERENCES `users` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE = InnoDB