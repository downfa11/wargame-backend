package com.ns.common;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@Builder
@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class RollbackUpdateQueryEvent extends SelfValidating<RollbackUpdateQueryEvent> {
    private String spaceId;
    private String winTeam;
    private String loseTeam;

    private List<ClientRequest> blueTeams;
    private List<ClientRequest> redTeams;

    public RollbackUpdateQueryEvent(@NotNull String spaceId,
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