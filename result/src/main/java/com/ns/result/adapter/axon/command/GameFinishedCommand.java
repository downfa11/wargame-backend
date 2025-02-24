package com.ns.result.adapter.axon.command;

import com.ns.common.ClientRequest;
import com.ns.common.utils.SelfValidating;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Getter
@Builder  // todo. test를 위해 잠시 열어둠
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class GameFinishedCommand extends SelfValidating<GameFinishedCommand> {
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

    public GameFinishedCommand(String spaceId, String state, Integer channel, Integer room, String winTeam, String loseTeam,
                               List<ClientRequest> blueTeams, List<ClientRequest> redTeams, String dateTime, Integer gameDuration) {
        this.spaceId = spaceId;
        this.state = state;
        this.channel = channel;
        this.room = room;
        this.winTeam = winTeam;
        this.loseTeam = loseTeam;
        this.blueTeams = blueTeams;
        this.redTeams = redTeams;
        this.dateTime = dateTime;
        this.gameDuration = gameDuration;
        this.validateSelf();
    }
}


