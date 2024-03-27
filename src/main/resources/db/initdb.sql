CREATE DATABASE IF NOT EXISTS template;
Use template;

CREATE TABLE IF NOT EXISTS users
(
    id        mediumint auto_increment not null,
    name      text                     not null,
    last_name text                     not null,
    phone     text                     not null,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;