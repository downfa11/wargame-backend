package com.ns.result.adapter.axon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class MembershipEloRequest {
    private Long membershipId;
    private String team;
    private Long elo;
}
