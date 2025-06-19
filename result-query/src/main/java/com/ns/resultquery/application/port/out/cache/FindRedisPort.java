package com.ns.resultquery.application.port.out.cache;

import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import reactor.core.publisher.Mono;

import java.util.List;

public interface FindRedisPort {
    CountSumByChamp findCountSumByChampInRange(String key);
    List<CountSumByChamp> findStatisticsByAllChampionInCurrentSeason(String key);
    CountSumByMembership findCountSumByMembershipInRange(String key);
}
