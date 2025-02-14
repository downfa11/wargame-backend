package com.ns.match.application.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserMatch {
    private String membershipId;
    private String name;
    private Long elo;
}
