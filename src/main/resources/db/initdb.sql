CREATE DATABASE IF NOT EXISTS movierama;
Use movierama;

CREATE TABLE IF NOT EXISTS users
(
    id       mediumint auto_increment not null,
    name varchar(128) unique not null,
    password text                     not null,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;