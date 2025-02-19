package com.ns.match.application.service;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserMatch {
    private String membershipId;
    private String name;
    private Long elo;
}
