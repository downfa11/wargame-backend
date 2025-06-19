package com.ns.resultquery.application.port.out.cache;

import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PushRedisPort {
    CountSumByChamp pushCountSumByChamp(String key, CountSumByChamp countSumByChamp);
    List<CountSumByChamp> pushStatisticsByAllChampionInCurrentSeason(String key, List<CountSumByChamp> countSumByChamps);
    CountSumByMembership pushCountSumByMembership(String key, CountSumByMembership countSumByMembership);
}
