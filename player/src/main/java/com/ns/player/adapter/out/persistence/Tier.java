package com.ns.player.adapter.out.persistence;

import lombok.Getter;

@Getter
public enum Tier {
    RANKER("Ranker"),
    DIAMOND("Diamond"),
    PLATINUM("Platinum"),
    GOLD("Gold"),
    SILVER("Silver"),
    BRONZE("Bronze"),
    UNRANKED("Unranked");

    private final String name;

    Tier(String name) {
        this.name = name;
    }

    public static Tier fromElo(long elo) {
        if (elo <= 800) return BRONZE;
        if (elo <= 1200) return SILVER;
        if (elo <= 1600) return GOLD;
        if (elo <= 2000) return PLATINUM;
        return DIAMOND;
    }
}