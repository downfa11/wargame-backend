package com.ns.player.application.port.in;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankPlayer {
    private Long rank;
    private String nickname;
    private String tier;
    private Long elo;
}
