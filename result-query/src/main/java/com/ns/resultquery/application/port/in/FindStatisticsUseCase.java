package com.ns.resultquery.application.port.in;

import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import reactor.core.publisher.Mono;

public interface FindStatisticsUseCase {
    Mono<CountSumByChamp> findStatisticsByChampion(String champName);
    Mono<CountSumByMembership> findStatisticsByUserName(String userName);
}
