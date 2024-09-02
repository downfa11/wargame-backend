package com.ns.membership.entity.dto;

import lombok.Data;

@Data
public class MatchUserResponse {
    private Long membershipId;
    private Long elo;
    private String name;
    private String spaceCode;
}
