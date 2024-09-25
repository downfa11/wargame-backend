package com.ns.resultquery.service;

import com.ns.resultquery.axon.QueryResultSumByChampName;
import com.ns.resultquery.axon.query.CountSumByChamp;
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
        return Mono.fromFuture(() ->
                queryGateway.query(new QueryResultSumByChampName(champName), CountSumByChamp.class));
    }

    public Mono<Void> insertResultCountIncreaseEventByChampName(ResultEventDto eventDto){
        return dynamoDBAdapter.insertResultCountIncreaseEventByChampName(
                eventDto.getChampIndex(),
                eventDto.getChampName(),
                eventDto.getSeason(),
                eventDto.getResultCount(),
                eventDto.getWinCount(),
                eventDto.getLoseCount());
    }

}


