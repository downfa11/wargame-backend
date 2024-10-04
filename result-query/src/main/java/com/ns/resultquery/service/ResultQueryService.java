package com.ns.resultquery.service;

import com.ns.resultquery.axon.QueryResultSumByChampName;
import com.ns.resultquery.axon.QueryResultSumByUserName;
import com.ns.resultquery.axon.query.CountSumByChamp;
import com.ns.resultquery.axon.query.CountSumByMembership;
import com.ns.resultquery.dto.InsertResultCountDto;
import com.ns.resultquery.dto.MembershipResultEventDto;
import com.ns.resultquery.dto.ResultEventDto;
import com.ns.resultquery.dynamo.DynamoDBAdapter;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ResultQueryService {

    private final QueryGateway queryGateway;
    private final DynamoDBAdapter dynamoDBAdapter;

    public Mono<CountSumByChamp> queryToResultSumByChampName(String champName){
        // 챔프의 전체 판수와 승률을 쿼리
        return Mono.fromFuture(() ->
                queryGateway.query(new QueryResultSumByChampName(champName), CountSumByChamp.class));
    }

    public Mono<CountSumByMembership> queryToResultByUserName(String champName){
        return Mono.fromFuture(() ->
                queryGateway.query(new QueryResultSumByUserName(champName), CountSumByMembership.class));
    }

    public Mono<Void> insertResultCountIncreaseEventByChampName(ResultEventDto eventDto){
        return dynamoDBAdapter.insertResultCountIncreaseEventByChampName(
                eventDto.getChampIndex(),
                eventDto.getChampName(),
                eventDto.getResultCount(),
                eventDto.getWinCount(),
                eventDto.getLoseCount());
    }




    public Mono<Void> insertResultCountIncreaseEventByUserName(MembershipResultEventDto eventDto){

        InsertResultCountDto insertResultCountDto = InsertResultCountDto.builder()
                .champIndex(eventDto.getChampIndex())
                .champName(eventDto.getChampName())
                .champResult(eventDto.getResultCount())
                .champWin(eventDto.getWinCount())
                .champLose(eventDto.getLoseCount())
                .resultCount(eventDto.getResultCount())
                .winCount(eventDto.getWinCount())
                .loseCount(eventDto.getLoseCount()).build();

        return dynamoDBAdapter.insertResultCountIncreaseEventByUserName(
                        eventDto.getMembershipId(),
                        eventDto.getUserName(),
                        insertResultCountDto
                );
    }

}


