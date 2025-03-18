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
public class CreateResultQueryEvent {
    private String spaceId;
    private String winTeam;
    private String loseTeam;

    private List<ClientRequest> blueTeams;
    private List<ClientRequest> redTeams;
}
