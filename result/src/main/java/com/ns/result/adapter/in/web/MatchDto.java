package com.ns.result.adapter.in.web;

import com.ns.common.ClientRequest;
import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import com.ns.common.TimelineData;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchDto {
    private String id;
    private String state;
    private int channel;
    private int room;
    private String winTeam;
    private String loseTeam;
    private String dateTime;
    private int gameDuration;
    private List<ClientRequest> winTeams;
    private List<ClientRequest> loseTeams;
    private List<TimelineData> timelineData;

    public static MatchDto from(Result result) {
        return MatchDto.builder()
                .id(result.getSpaceId())
                .state(result.getState())
                .channel(result.getChannel())
                .room(result.getRoom())
                .winTeam(result.getWinTeam())
                .loseTeam(result.getLoseTeam())
                .dateTime(result.getDateTime())
                .gameDuration(result.getGameDuration())
                .winTeams(result.getBlueTeams())
                .loseTeams(result.getRedTeams())
                .timelineData(result.getTimelineData())
                .build();
    }
}

