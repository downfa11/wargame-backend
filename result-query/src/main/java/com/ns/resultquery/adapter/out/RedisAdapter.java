package com.ns.resultquery.adapter.out;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.application.port.out.cache.FindRedisPort;
import com.ns.resultquery.application.port.out.cache.PushRedisPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Mono;


@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class RedisAdapter implements PushRedisPort, FindRedisPort {

    private final ReactiveRedisOperations<String, CountSumByChamp> champRedisTemplate;
    private final ReactiveRedisOperations<String, CountSumByMembership> membershipRedisTemplate;

    @Override
    public Mono<CountSumByChamp> pushCountSumByChamp(String key, CountSumByChamp countSumByChamp) {
        return champRedisTemplate.opsForValue()
                .set(key, countSumByChamp)
                .then(Mono.just(countSumByChamp));
    }

    @Override
    public Mono<CountSumByMembership> pushCountSumByMembership(String key, CountSumByMembership countSumByMembership) {
        return membershipRedisTemplate.opsForValue()
                .set(key, countSumByMembership)
                .then(Mono.just(countSumByMembership));
    }

    @Override
    public Mono<CountSumByChamp> findCountSumByChampInRange(String key) {
        return champRedisTemplate.opsForValue()
                .get(key)
                .doOnTerminate(() -> log.info("findCountSumByChampInRange: {}", key));
    }

    @Override
    public Mono<CountSumByMembership> findCountSumByMembershipInRange(String key) {
        return membershipRedisTemplate.opsForValue()
                .get(key)
                .doOnTerminate(() -> log.info("findCountSumByMembershipInRange: {}", key));
    }
}
