package com.ns.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerQuery {
    private Long membershipId;
    private String nickname;
    private Long elo;
    private String code;
}
