package com.ns.resultquery.application.service;

import com.ns.common.anotation.UseCase;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.application.port.in.FindStatisticsUseCase;
import com.ns.resultquery.application.port.out.FindStatisticsPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@UseCase
@RequiredArgsConstructor
public class FindStatisticsService implements FindStatisticsUseCase {

    private final FindStatisticsPort findStatisticsPort;


    @Override
    public Mono<CountSumByChamp> findStatiscticsByChampion(String champName) {
        return findStatisticsPort.queryToResultSumByChampName(champName);
    }

    @Override
    public Mono<CountSumByMembership> findStatiscticsByUserName(String userName) {
        return findStatisticsPort.queryToResultByUserName(userName);
    }
}
