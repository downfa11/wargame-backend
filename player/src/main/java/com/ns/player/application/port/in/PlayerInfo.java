package com.ns.player.application.port.in;


import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Builder
public class PlayerInfo {
    private String nickname;
    private Long elo;
    private String tier;
    private LocalDateTime lastGameTime;
}
