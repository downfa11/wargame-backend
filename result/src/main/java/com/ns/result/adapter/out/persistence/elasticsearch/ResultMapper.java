package com.ns.result.adapter.out.persistence.elasticsearch;

import com.ns.common.GameFinishedEvent;
import org.springframework.stereotype.Component;

@Component
public class ResultMapper {
    public static GameFinishedEvent mapToResultReqeustEvent(Result saveResult){
        return new GameFinishedEvent()
                .builder()
                .spaceId(saveResult.getSpaceId())
                .blueTeams(saveResult.getBlueTeams())
                .redTeams(saveResult.getRedTeams())
                .winTeam(saveResult.getWinTeam())
                .loseTeam(saveResult.getLoseTeam())
                .dateTime(saveResult.getDateTime())
                .gameDuration(saveResult.getGameDuration()).build();
    }

    public static Result mapToResultDocument(GameFinishedEvent event) {
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
                .build();
    }
}
