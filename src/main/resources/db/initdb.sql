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

CREATE TABLE IF NOT EXISTS movies
(
    id          mediumint auto_increment not null,
    title       varchar(128) unique      not null,
    description text                     not null,
    user_id     mediumint                not null,
    date        text                     not null,
    likes       mediumint                not null,
    hates       mediumint                not null,
    PRIMARY KEY (id),
    KEY FK_users (user_id),
    CONSTRAINT FK_users FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

