package com.ns.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchUserResponse {
    private Long membershipId;
    private Long elo;
    private String name;
    private String spaceCode;
}
