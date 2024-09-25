package com.ns.result.domain;

import com.ns.common.ClientRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResultRequest {
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
