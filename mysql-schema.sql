CREATE TABLE champion_stats
(
    champion_id    INT PRIMARY KEY,
    name           VARCHAR(255) UNIQUE,
    maxhp          INT,
    maxmana        INT,
    attack         INT,
    absorptionRate FLOAT,
    defense        INT,
    movespeed      FLOAT,
    maxdelay       FLOAT,
    attspeed       FLOAT,
    attrange       INT,
    critical       FLOAT,
    criProbability FLOAT,
    growHp         INT,
    growMana       INT,
    growAtt        INT,
    growCri        INT,
    growCriPob     INT,
    unit_type      ENUM('Regiment', 'Brigade'),
    platoon_type   ENUM('Infantry', 'Archer'),
    platoon_count  INT
);

INSERT INTO champion_stats (champion_id, name, maxhp, maxmana, attack, absorptionRate, defense, movespeed, maxdelay,
                            attspeed, attrange, critical, criProbability, growHp, growMana, growAtt, growCri,
                            growCriPob, unit_type, platoon_type, platoon_count)
VALUES (0, 'toy0', 580, 200, 60, 0.0, 5, 10.0, 3.0, 0.8, 12, 30.0, 50.0, 100, 30, 10, 10, 0, 'Regiment', 'Archer', 3),
       (1, 'toy1', 500, 200, 50, 0.0, 5, 10.0, 3.0, 0.8, 25, 100.0, 10.0, 100, 30, 10, 10, 0, 'Brigade', 'Infantry', 4),
       (2, 'Spearman', 620, 200, 0, 5.0, 80, 10.0, 2.0, 1.0, 10, 50.0, 30.0, 100, 30, 10, 10, 0, 'Regiment', 'Infantry', 3),
       (3, 'Lux', 490, 200, 53, 0.0, 5, 10.0, 2.0, 1.0, 10, 10.0, 0.0, 100, 30, 10, 10, 0, 'Brigade', 'Infantry', 3),
       (4, 'Yasuo', 490, 200, 60, 0.0, 5, 10.0, 2.0, 1.0, 10, 10.0, 0.0, 100, 30, 10, 10, 0, 'Regiment', 'Infantry', 3);

CREATE TABLE item_stats
(
    id             INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    gold           INT          NOT NULL,
    maxhp          INT          NOT NULL,
    attack         INT          NOT NULL,
    movespeed      FLOAT        NOT NULL,
    maxdelay       FLOAT        NOT NULL,
    attspeed       FLOAT        NOT NULL,
    criProbability INT          NOT NULL,
    absorptionRate FLOAT        NOT NULL,
    defense        INT          NOT NULL
);

INSERT INTO item_stats (name, gold, maxhp, attack, movespeed, maxdelay, attspeed, criProbability, absorptionRate,
                        defense)
VALUES ('Sword of the Brave', 100, 50, 20, 10.0, 0.0, 5.0, 10, 0.1, 3),
       ('Staff of Wisdom', 120, 30, 10, 5.0, 0.0, 10.0, 15, 0.0, 5),
       ('Armor of Valor', 200, 100, 0, 0.0, 20.0, 0.0, 5, 0.0, 0),
       ('Boots of Swiftness', 80, 0, 0, 15.0, 0.0, 0.0, 0, 0.0, 0),
       ('Ring of Power', 150, 0, 30, 0.0, 0.0, 0.0, 20, 0.0, 0);

CREATE TABLE unit_stats
(
    unit_id       INT AUTO_INCREMENT PRIMARY KEY,
    champion_id   INT,
    unit_kind     ENUM('Infantry', 'Archer') NOT NULL,
    maxhp         INT   NOT NULL,
    curhp         INT   NOT NULL,
    attrange      INT   NOT NULL,
    attack        INT   NOT NULL,
    maxdelay      FLOAT NOT NULL,
    speed         INT   NOT NULL,
    soldier_count INT   NOT NULL,
    unit_type     ENUM('Brigade', 'Regiment') NOT NULL,
    FOREIGN KEY (champion_id) REFERENCES wargame.champion_stats (champion_id)
);
