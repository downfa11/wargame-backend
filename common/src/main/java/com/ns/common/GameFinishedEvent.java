package com.ns.common;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameFinishedEvent {
    private String spaceId;
    private String state; // dodge: 비정상적인 상황, success:정상적인 상황

    private int channel;
    private int room;
    private String winTeam;
    private String loseTeam;

    private List<ClientRequest> blueTeams;
    private List<ClientRequest> redTeams;

    private String dateTime;
    private int gameDuration;
}
