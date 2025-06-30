package com.ns.common;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CreateResultEvent extends SelfValidating<CreateResultEvent> {

    @NotNull private String spaceId;
    @NotNull private String winTeam;
    @NotNull private String loseTeam;

    @NotNull private List<ClientRequest> blueTeams;
    @NotNull private List<ClientRequest> redTeams;

    @NotNull private String dateTime;
    @NotNull private int gameDuration;
    @NotNull private int channel;
    @NotNull private int room;
    @NotNull private List<TimelineData> timelineData;

    public CreateResultEvent(String spaceId, String winTeam, String loseTeam, List<ClientRequest> blueTeams, List<ClientRequest> redTeams,
            String dateTime, int gameDuration, int channel, int room, List<TimelineData> timelineData) {
        this.spaceId = spaceId;
        this.winTeam = winTeam;
        this.loseTeam = loseTeam;
        this.blueTeams = blueTeams;
        this.redTeams = redTeams;
        this.dateTime = dateTime;
        this.gameDuration = gameDuration;
        this.channel = channel;
        this.room = room;
        this.timelineData = timelineData;
        this.validateSelf();
    }
}
