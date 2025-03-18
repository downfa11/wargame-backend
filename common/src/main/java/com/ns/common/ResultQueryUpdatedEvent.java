package com.ns.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResultQueryUpdatedEvent {
    private String spaceId;
    private String winTeam;
    private String loseTeam;

    private List<ClientRequest> blueTeams;
    private List<ClientRequest> redTeams;
}
