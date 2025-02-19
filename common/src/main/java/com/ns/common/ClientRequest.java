package com.ns.common;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientRequest {
    private Long membershipId;
    private int socket;
    private Long champindex;
    private String user_name;
    private String team;
    private int channel;
    private int room;
    private int kill;
    private int death;
    private int assist;
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
    private List<Integer> itemList;
}
