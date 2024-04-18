package com.ns.wargame.Domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameResultRequest {
    private String spaceId;
    private String state; // dodge: 비정상적인 상황, success:정상적인 상황

    private String winTeamString;
    private String loseTeamString;

    private List<Client> winTeams;
    private List<Client> loseTeams;

    private String dateTime;
    private int gameDuration;
}
