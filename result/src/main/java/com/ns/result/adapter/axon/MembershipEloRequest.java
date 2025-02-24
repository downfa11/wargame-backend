package com.ns.result.adapter.axon;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class MembershipEloRequest {
    private Long membershipId;
    private String team;
    private Long elo;
}
