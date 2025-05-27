package com.ns.player.adapter.axon.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QueryPlayer {
    private String membershipId;
    private String nickname;
    private String code;
    private Long elo;
}
