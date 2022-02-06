-- Auth
DROP SCHEMA `auth`;
CREATE SCHEMA `auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin ;

CREATE TABLE `auth`.`users` (
  `id` varchar(40) COLLATE utf8mb4_bin NOT NULL,
  `nickname` varchar(45) CHARACTER SET utf8mb4 NOT NULL,
  `passwordHash` varchar(125) COLLATE utf8mb4_bin NOT NULL,
  `roles` varchar(125) CHARACTER SET utf8mb4 NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_nicknames` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
