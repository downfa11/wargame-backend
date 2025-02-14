package com.ns.resultquery.adapter.out.persistence;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.resultquery.adapter.axon.QueryResultSumByChampName;
import com.ns.resultquery.adapter.axon.QueryResultSumByUserName;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.application.port.out.FindStatisticsPort;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryGateway;
import reactor.core.publisher.Mono;

@PersistanceAdapter
@RequiredArgsConstructor
public class FindStatisticsAdapter implements FindStatisticsPort {
    private final QueryGateway queryGateway;


    @Override
    public Mono<CountSumByChamp> queryToResultSumByChampName(String champName) {
        // 챔프의 전체 판수와 승률을 쿼리
        return Mono.fromFuture(() ->
                queryGateway.query(new QueryResultSumByChampName(champName), CountSumByChamp.class));
    }

    @Override
    public Mono<CountSumByMembership> queryToResultByUserName(String champName) {
        return Mono.fromFuture(() ->
                queryGateway.query(new QueryResultSumByUserName(champName), CountSumByMembership.class));
    }
}
