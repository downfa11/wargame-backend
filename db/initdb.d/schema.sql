CREATE TABLE BoardKinds (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            kind CHAR(20)
);

CREATE TABLE Boards (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        kind_id INT,
                        boardId INT,
                        FOREIGN KEY (kind_id) REFERENCES BoardKinds(id),
                        UNIQUE (kind_id, boardId)
);

CREATE TABLE champion_stats (
                                champion_id INT PRIMARY KEY,
                                name VARCHAR(255),
                                maxhp INT,
                                maxmana INT,
                                attack INT,
                                absorptionRate FLOAT,
                                defense INT,
                                movespeed FLOAT,
                                maxdelay FLOAT,
                                attspeed FLOAT,
                                attrange INT,
                                critical INT,
                                criProbability INT,
                                growHp INT,
                                growMana INT,
                                growAtt INT,
                                growCri INT,
                                growCriPob INT
);

INSERT INTO champion_stats (champion_id, name, maxhp, maxmana, attack, absorptionRate, defense, movespeed, maxdelay, attspeed, attrange, critical, criProbability, growHp, growMana, growAtt, growCri, growCriPob)
VALUES
    (0, 'toy0', 580, 200, 60, 0, 5, 10, 3, 0.8, 12, 30, 50, 100, 30, 10, 10, 0),
    (1, 'toy1', 500, 200, 50, 0, 5, 10, 3, 0.8, 25, 100, 10, 100, 30, 10, 10, 0),
    (2, 'Spearman', 620, 200, 0, 5, 80, 10, 2, 1, 10, 50, 30, 100, 30, 10, 10, 0),
    (3, 'Lux', 490, 200, 53, 0, 5, 10, 2, 1, 10, 10, 0, 100, 30, 10, 10, 0),
    (4, 'Yasuo', 490, 200, 60, 0, 5, 10, 2, 1, 10, 10, 0, 100, 30, 10, 10, 0);

CREATE TABLE item_stats (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(255) NOT NULL,
                            gold INT NOT NULL,
                            maxhp INT NOT NULL,
                            attack INT NOT NULL,
                            movespeed FLOAT NOT NULL,
                            maxdelay FLOAT NOT NULL,
                            attspeed FLOAT NOT NULL,
                            criProbability INT NOT NULL,
                            absorptionRate FLOAT NOT NULL,
                            defense INT NOT NULL
);

INSERT INTO item_stats (name, gold, maxhp, attack, movespeed, maxdelay, attspeed, criProbability, absorptionRate, defense)
VALUES
    ('Sword of the Brave', 100, 50, 20, 10, 0, 5, 10, 0.1, 3),
    ('Staff of Wisdom', 120, 30, 10, 5, 0, 10, 15, 0, 5),
    ('Armor of Valor', 200, 100, 0, 0, 20, 0, 5, 0, 0),
    ('Boots of Swiftness', 80, 0, 0, 15, 0, 0, 0, 0, 0),
    ('Ring of Power', 150, 0, 30, 0, 0, 0, 20, 0, 0);

CREATE TABLE results (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         code VARCHAR(255),
                         channel INT,
                         room INT,
                         win_team VARCHAR(255),
                         lose_team VARCHAR(255),
                         date_time DATETIME,
                         game_duration INT
);

CREATE TABLE clients (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
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

CREATE TABLE client_items (
                              client_id BIGINT,
                              item_id INT,
                              FOREIGN KEY (client_id) REFERENCES clients(id),
                              FOREIGN KEY (item_id) REFERENCES item_stats(id),
                              PRIMARY KEY (client_id, item_id)
);
