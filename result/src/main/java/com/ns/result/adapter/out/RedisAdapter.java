package com.ns.result.adapter.out;

import static com.ns.result.adapter.out.persistence.elasticsearch.ResultPersistenceAdapter.RESULT_SEARCH_SIZE;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import com.ns.result.application.port.out.cache.FindRedisPort;
import com.ns.result.application.port.out.cache.PushRedisPort;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class RedisAdapter implements PushRedisPort, FindRedisPort {

    private final ReactiveRedisOperations<String, Result> resultRedisTemplate;


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

}
