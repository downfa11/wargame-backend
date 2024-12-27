package com.ns.result;

import com.ns.common.ResultRequestEvent;
import com.ns.result.domain.entity.Result;
import org.springframework.stereotype.Component;

@Component
public class ResultMapper {
    public static ResultRequestEvent mapToResultReqeustEvent(Result saveResult){
        return new ResultRequestEvent()
                .builder()
                .spaceId(saveResult.getSpaceId())
                .blueTeams(saveResult.getBlueTeams())
                .redTeams(saveResult.getRedTeams())
                .winTeam(saveResult.getWinTeam())
                .loseTeam(saveResult.getLoseTeam())
                .dateTime(saveResult.getDateTime())
                .gameDuration(saveResult.getGameDuration()).build();
    }

    public static Result mapToResultDocument(ResultRequestEvent request) {
        return Result.builder()
                .spaceId(request.getSpaceId())
                .state("success")
                .channel(request.getChannel())
                .room(request.getRoom())
                .winTeam(request.getWinTeam())
                .loseTeam(request.getLoseTeam())
                .blueTeams(request.getBlueTeams())
                .redTeams(request.getRedTeams())
                .dateTime(request.getDateTime())
                .gameDuration(request.getGameDuration())
                .build();
    }
}
