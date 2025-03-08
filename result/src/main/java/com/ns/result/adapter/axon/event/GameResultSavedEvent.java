package com.ns.result.adapter.axon.event;

import com.ns.common.ClientRequest;
import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class GameResultSavedEvent extends SelfValidating<GameResultSavedEvent> {
    private String spaceId;
    private String winTeam;
    private String loseTeam;

    private List<ClientRequest> blueTeams;
    private List<ClientRequest> redTeams;

    public GameResultSavedEvent(@NotNull String spaceId,
                                @NotNull String winTeam,
                                @NotNull String loseTeam,
                                @NotNull List<ClientRequest> blueTeams,
                                @NotNull List<ClientRequest> redTeams) {
        this.spaceId = spaceId;
        this.winTeam = winTeam;
        this.loseTeam = loseTeam;
        this.blueTeams = blueTeams;
        this.redTeams = redTeams;
        this.validateSelf();
    }
}
