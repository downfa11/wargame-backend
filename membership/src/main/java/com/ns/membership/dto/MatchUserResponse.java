package com.ns.membership.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchUserResponse {
    private Long membershipId;
    private Long elo;
    private String name;
    private String spaceCode;
}
