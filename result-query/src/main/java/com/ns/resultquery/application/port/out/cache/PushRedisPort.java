package com.ns.resultquery.application.port.out.cache;

import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import reactor.core.publisher.Mono;

public interface PushRedisPort {
    Mono<CountSumByChamp> pushCountSumByChamp(String key, CountSumByChamp countSumByChamp);
    Mono<CountSumByMembership> pushCountSumByMembership(String key, CountSumByMembership countSumByMembership);
}
