package com.ns.wargame.Domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Client {
    private int socket;
    private int champindex;
    private String user_name;
    private int out_time;
    private int channel;
    private int room;
    private String code;
    private int clientindex;
    private int kill;
    private int death;
    private int assist;
    private int x;
    private int y;
    private int z;
    private int gold;
    private int rotationX;
    private int rotationY;
    private int rotationZ;
    private int level;
    private int maxexp;
    private int exp;
    private boolean stopped;
    private boolean attacked;
    private int curhp;
    private int maxhp;
    private int curmana;
    private int maxmana;
    private int attack;
    private int critical;
    private int criProbability;
    private int maxdelay;
    private int curdelay;
    private int attrange;
    private int attspeed;
    private int movespeed;
    private int growhp;
    private int growmana;
    private int growAtt;
    private int growCri;
    private int growCriPro;
    private int team;
    private boolean ready;
    private List<Integer> itemList;

}
