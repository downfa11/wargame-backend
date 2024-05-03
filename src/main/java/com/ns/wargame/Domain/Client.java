package com.ns.wargame.Domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("clients")
public class Client {
    @Id
    private Long id;
    private Long userId; //user index(PK)
    private Long gameResultId; // (FK)

    private int socket;
    private int champ; // champ index
    private String name;
    private String team;
    private int channel;
    private int room;
    private int kills;
    private int deaths;
    private int assists;
    private int gold;
    private int level;
    private int maxhp;
    private int maxmana;
    private int attack;
    private int critical;
    private int criProbability;
    private int attrange;
    private float attspeed;
    private int movespeed;
    private String itemList;

}
