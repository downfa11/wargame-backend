package com.ns.match.adapter.in.web;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchRequest {
    private Long membershipId;
}
