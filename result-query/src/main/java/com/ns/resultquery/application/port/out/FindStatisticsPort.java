package com.ns.resultquery.application.port.out;

import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import reactor.core.publisher.Mono;

public interface FindStatisticsPort {

    Mono<CountSumByChamp> queryToResultSumByChampName(String champName);
    Mono<CountSumByMembership> queryToResultByUserName(String champName);
}
