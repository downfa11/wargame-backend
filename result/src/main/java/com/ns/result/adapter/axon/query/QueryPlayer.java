package com.ns.result.adapter.axon.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueryPlayer {
    private String membershipId;
    private String code;
    private Long elo;
}
