package com.ns.resultquery.application.service;

import com.ns.common.anotation.UseCase;
import com.ns.resultquery.application.port.in.InsertChampStatisticsUseCase;
import com.ns.resultquery.application.port.in.InsertUserStatisticsUseCase;
import com.ns.resultquery.application.port.out.InsertChampStatisticsPort;
import com.ns.resultquery.application.port.out.InsertUserStatisticsPort;
import com.ns.resultquery.domain.dto.InsertResultCountDto;
import com.ns.resultquery.domain.dto.MembershipResultEventDto;
import com.ns.resultquery.domain.dto.ResultEventDto;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@UseCase
@RequiredArgsConstructor
public class InsertUserStatisticsService implements InsertUserStatisticsUseCase, InsertChampStatisticsUseCase {

    private final InsertUserStatisticsPort insertUserStatisticsPort;
    private final InsertChampStatisticsPort insertChampStatisticsPort;

    public Mono<Void> insertResultCountIncreaseEventByChampName(ResultEventDto eventDto){
        return insertChampStatisticsPort.insertResultCountIncreaseEventByChampName(
                eventDto.getChampIndex(),
                eventDto.getChampName(),
                eventDto.getResultCount(),
                eventDto.getWinCount(),
                eventDto.getLoseCount());
    }


    public Mono<Void> insertResultCountIncreaseEventByUserName(MembershipResultEventDto eventDto){
        return insertUserStatisticsPort.insertResultCountIncreaseEventByUserName(
                eventDto.getMembershipId(),
                eventDto.getUserName(),
                createInsertResultCountDto(eventDto));
    }

    private InsertResultCountDto createInsertResultCountDto(MembershipResultEventDto eventDto){
        return InsertResultCountDto.builder()
                .champIndex(eventDto.getChampIndex())
                .champName(eventDto.getChampName())
                .champResult(eventDto.getResultCount())
                .champWin(eventDto.getWinCount())
                .champLose(eventDto.getLoseCount())
                .resultCount(eventDto.getResultCount())
                .winCount(eventDto.getWinCount())
                .loseCount(eventDto.getLoseCount()).build();
    }
}
