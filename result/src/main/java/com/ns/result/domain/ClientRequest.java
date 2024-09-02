package com.ns.result.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientRequest {
    private Long membershipId;
    private int socket;
    private int champindex;
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
