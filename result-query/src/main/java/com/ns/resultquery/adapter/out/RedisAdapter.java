package com.ns.resultquery.adapter.out;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.application.port.out.cache.FindRedisPort;
import com.ns.resultquery.application.port.out.cache.PushRedisPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class RedisAdapter implements PushRedisPort, FindRedisPort {

    private final RedisTemplate<String, CountSumByChamp> champRedisTemplate;
    private final RedisTemplate<String, CountSumByMembership> membershipRedisTemplate;

    @Override
    public CountSumByChamp pushCountSumByChamp(String key, CountSumByChamp countSumByChamp) {
        ValueOperations<String, CountSumByChamp> valueOps = champRedisTemplate.opsForValue();
        valueOps.set(key, countSumByChamp);
        log.info("Pushed CountSumByChamp to Redis with key: {}", key);
        return countSumByChamp;
    }

    @Override
    public CountSumByMembership pushCountSumByMembership(String key, CountSumByMembership countSumByMembership) {
        ValueOperations<String, CountSumByMembership> valueOps = membershipRedisTemplate.opsForValue();
        valueOps.set(key, countSumByMembership);
        log.info("Pushed CountSumByMembership to Redis with key: {}", key);
        return countSumByMembership;
    }

    @Override
    public CountSumByChamp findCountSumByChampInRange(String key) {
        ValueOperations<String, CountSumByChamp> valueOps = champRedisTemplate.opsForValue();
        CountSumByChamp result = valueOps.get(key);
        log.info("Found CountSumByChamp in Redis for key: {}", key);
        return result;
    }

    @Override
    public CountSumByMembership findCountSumByMembershipInRange(String key) {
        ValueOperations<String, CountSumByMembership> valueOps = membershipRedisTemplate.opsForValue();
        CountSumByMembership result = valueOps.get(key);
        log.info("Found CountSumByMembership in Redis for key: {}", key);
        return result;
    }
}