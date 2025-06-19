package com.ns.resultquery.application.port.in;

import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import reactor.core.publisher.Mono;

import java.util.List;

public interface FindStatisticsUseCase {
    CountSumByChamp findStatisticsByChampion(String champName);
    List<CountSumByChamp> findStatisticsByAllChampionInCurrentSeason();
    CountSumByMembership findStatisticsByUserName(String userName);
}
