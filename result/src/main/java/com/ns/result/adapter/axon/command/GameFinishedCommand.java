package com.ns.result.adapter.axon.command;

import com.ns.common.ClientRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameFinishedCommand {
    @TargetAggregateIdentifier
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
