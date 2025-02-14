package com.ns.resultquery.application.port.in;

import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import reactor.core.publisher.Mono;

public interface FindStatisticsUseCase {
    Mono<CountSumByChamp> findStatiscticsByChampion(String champName);
    Mono<CountSumByMembership> findStatiscticsByUserName(String userName);
}
