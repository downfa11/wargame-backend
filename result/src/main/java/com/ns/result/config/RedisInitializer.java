package com.ns.result.config;

import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final ReactiveRedisOperations<String, Result> resultRedisTemplate;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        resultRedisTemplate.opsForValue().get("1")
                .doOnSuccess(result -> log.info("Initialized Redis with result: {}", result))
                .doOnError(err -> log.error("Failed to fetch result from Redis: {}", err.getMessage()))
                .subscribe();
    }
}