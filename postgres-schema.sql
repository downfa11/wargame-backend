CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       aggregate_identifier VARCHAR(255),
                       account VARCHAR(255),
                       email VARCHAR(255),
                       password VARCHAR(255),
                       name VARCHAR(255),
                       refresh_token VARCHAR(255),
                       created_at TIMESTAMP DEFAULT now(),
                       updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE posts (
                       id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT REFERENCES users(id),
                       nickname VARCHAR(255),
                       category_id BIGINT,
                       title VARCHAR(255),
                       content TEXT,
                       comments BIGINT,
                       sort_status VARCHAR(32),
                       event_start_date TIMESTAMP,
                       event_end_date TIMESTAMP,
                       created_at TIMESTAMP DEFAULT now(),
                       updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE comments (
                          id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT REFERENCES users(id),
                          nickname VARCHAR(255),
                          board_id BIGINT REFERENCES posts(id),
                          content TEXT,
                          created_at TIMESTAMP DEFAULT now(),
                          updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE images (
                        id BIGSERIAL PRIMARY KEY,
                        post_id BIGINT REFERENCES posts(id),
                        url VARCHAR(2048)
);

CREATE TABLE players (
                         id BIGSERIAL PRIMARY KEY,
                         membership_id VARCHAR(255),
                         aggregate_identifier VARCHAR(255),
                         elo BIGINT,
                         code VARCHAR(255)
);
