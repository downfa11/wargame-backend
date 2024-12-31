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