package com.ns.resultquery.application.service;

import com.ns.common.anotation.UseCase;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.application.port.in.FindStatisticsUseCase;
import com.ns.resultquery.application.port.out.FindStatisticsPort;
import com.ns.resultquery.application.port.out.cache.FindRedisPort;
import com.ns.resultquery.application.port.out.cache.PushRedisPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@UseCase
@RequiredArgsConstructor
public class FindStatisticsService implements FindStatisticsUseCase {
    private final PushRedisPort pushRedisPort;
    private final FindRedisPort findRedisPort;

    private final FindStatisticsPort findStatisticsPort;


    @Override
    public Mono<CountSumByChamp> findStatisticsByChampion(String champName) {
        String key = "statistics:name:" + champName;
        return getCountSumByChampsByKey(key, findStatisticsPort.queryToResultSumByChampName(champName));
    }

    private Mono<CountSumByChamp> getCountSumByChampsByKey(String key, Mono<CountSumByChamp> dbResults) {
        return findRedisPort.findCountSumByChampInRange(key)
                .switchIfEmpty(dbResults.flatMap(result -> pushRedisPort.pushCountSumByChamp(key, result).thenReturn(result)));
    }

    @Override
    public Mono<CountSumByMembership> findStatisticsByUserName(String userName) {
        String key = "statistics:name:" + userName;
        return getCountSumByMembershipsByKey(key, findStatisticsPort.queryToResultByUserName(userName));
    }

    private Mono<CountSumByMembership> getCountSumByMembershipsByKey(String key, Mono<CountSumByMembership> dbResults) {
        return findRedisPort.findCountSumByMembershipInRange(key)
                .switchIfEmpty(dbResults.flatMap(result -> pushRedisPort.pushCountSumByMembership(key, result).thenReturn(result)));
    }
}
