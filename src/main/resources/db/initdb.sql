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
    KEY FK_movies (user_id),
    CONSTRAINT FK_movies FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS opinions
(
    id       mediumint auto_increment not null,
    opinion  varchar(10)              not null,
    user_id  mediumint                not null,
    movie_id mediumint                not null,
    PRIMARY KEY (id),
    KEY FK_actions_1 (user_id),
    KEY FK_actions_2 (movie_id),
    CONSTRAINT FK_actions_1 FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT FK_actions_2 FOREIGN KEY (movie_id) REFERENCES movies (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
