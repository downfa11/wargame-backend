package com.ns.result.adapter.out.persistence.elasticsearch;

import com.ns.common.CreateResultEvent;
import com.ns.common.GameFinishedEvent;
import org.springframework.stereotype.Component;

@Component
public class ResultMapper {

    public static Result mapToResultDocument(CreateResultEvent event) {
        return Result.builder()
                .spaceId(event.getSpaceId())
                .state("success")
                .channel(event.getChannel())
                .room(event.getRoom())
                .winTeam(event.getWinTeam())
                .loseTeam(event.getLoseTeam())
                .blueTeams(event.getBlueTeams())
                .redTeams(event.getRedTeams())
                .dateTime(event.getDateTime())
                .gameDuration(event.getGameDuration())
                .timelineData(event.getTimelineData())
                .build();
    }
}
