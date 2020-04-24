CREATE DATABASE IF NOT EXISTS `sample00`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `sample00`;

CREATE TABLE IF NOT EXISTS `users`
(
    `user_id`   INTEGER UNSIGNED NOT NULL,
    `user_name` VARCHAR(50)      NOT NULL,
    `password`  VARCHAR(50)      NOT NULL,
    `email`     VARCHAR(200),
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`)
);

CREATE INDEX idx_users_user_name ON `users` (user_name);
CREATE INDEX idx_users_password ON `users` (password);
CREATE INDEX idx_users_email ON `users` (email);
CREATE INDEX idx_users_created_at ON `users` (created_at);
CREATE INDEX idx_users_updated_at ON `users` (updated_at);

INSERT INTO `users` (user_id, user_name, password, email)
SELECT 0, '桃太　郎', 'pass001', 'momo@example.com'
FROM dual
WHERE NOT EXISTS(SELECT * FROM users WHERE user_id = 0);

INSERT INTO `users` (user_id, user_name, password, email)
SELECT 1, 'い　ぬ', 'pass002', NULL
FROM dual
WHERE NOT EXISTS(SELECT * FROM users WHERE user_id = 1);

INSERT INTO `users` (user_id, user_name, password, email)
SELECT 2, 'き　じ', 'pass003', NULL
FROM dual
WHERE NOT EXISTS(SELECT * FROM users WHERE user_id = 2);

INSERT INTO `users` (user_id, user_name, password, email)
SELECT 3, 'さ　る', 'pass004', NULL
FROM dual
WHERE NOT EXISTS(SELECT * FROM users WHERE user_id = 3);
