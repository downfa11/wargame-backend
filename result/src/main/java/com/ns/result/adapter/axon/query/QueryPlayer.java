package com.ns.result.adapter.axon.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QueryPlayer {
    private String membershipId;
    private String code;
    private Long elo;
}
