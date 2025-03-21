package com.ns.result.adapter.out;

import static com.ns.result.adapter.out.persistence.elasticsearch.ElasticPersistenceAdapter.RESULT_SEARCH_SIZE;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import com.ns.result.application.port.out.cache.FindRedisPort;
import com.ns.result.application.port.out.cache.PushRedisPort;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class RedisAdapter implements PushRedisPort, FindRedisPort {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ReactiveRedisTemplate<String, Result> resultRedisTemplate;


    @Override
    public Flux<Result> findResultInRange(String key, int offset) {
        return resultRedisTemplate.opsForList().range(key, offset, offset + RESULT_SEARCH_SIZE - 1);
    }

    @Override
    public Mono<Result> pushResult(String key, Result result) {
        return resultRedisTemplate.opsForList().rightPush(key, result)
                .thenReturn(result)
                .doOnTerminate(() -> resultRedisTemplate.expire(key, Duration.ofHours(1)));
    }

    @Override
    public Flux<Long> pushString(String key, List<String> values) {
        return Flux.fromIterable(values)
                .flatMap(value -> reactiveRedisTemplate.opsForSet().add(key, value));
    }


    @Override
    public Flux<String> findString(String key) {
        return reactiveRedisTemplate.opsForSet().members(key);
    }
}
