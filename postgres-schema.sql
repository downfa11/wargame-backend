CREATE TABLE BoardKinds (
                            id SERIAL PRIMARY KEY,
                            kind CHAR(20)
);

CREATE TABLE Boards (
                        id SERIAL PRIMARY KEY,
                        kind_id INT,
                        boardId INT,
                        FOREIGN KEY (kind_id) REFERENCES BoardKinds(id),
                        UNIQUE (kind_id, boardId)
);

CREATE TABLE results (
                         id BIGSERIAL PRIMARY KEY,
                         code VARCHAR(255),
                         channel INT,
                         room INT,
                         win_team VARCHAR(255),
                         lose_team VARCHAR(255),
                         date_time TIMESTAMP,
                         game_duration INT
);

CREATE TABLE clients (
                         id BIGSERIAL PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         game_result_id BIGINT,
                         socket INT,
                         champ INT,
                         name VARCHAR(255),
                         team VARCHAR(255),
                         channel INT,
                         room INT,
                         kills INT,
                         deaths INT,
                         assists INT,
                         gold INT,
                         level INT,
                         maxhp INT,
                         maxmana INT,
                         attack INT,
                         absorptionRate FLOAT,
                         defense INT,
                         critical INT,
                         cri_probability INT,
                         attrange INT,
                         attspeed FLOAT,
                         movespeed FLOAT,
                         item_list VARCHAR(255),
                         FOREIGN KEY (game_result_id) REFERENCES results(id),
                         FOREIGN KEY (champ) REFERENCES champion_stats(champion_id)
);