package com.ns.match.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisConfig implements ApplicationListener<ApplicationReadyEvent> {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;



    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        reactiveRedisTemplate.opsForValue().get("1")
                .doOnSuccess(i->log.info("Initialize to redis"))
                .doOnError((err) -> log.error("Failed to init redis: {}",err.getMessage()))
                .subscribe();

        // reactiveRedisTemplate.opsForList().leftPush("list1","hello").subscribe();

    }

    @Bean
    public ReactiveRedisTemplate<String, Long> reactiveRedisTemplate_long(ReactiveRedisConnectionFactory connectionFactory){
        RedisSerializationContext<String, Long> serializationContext = RedisSerializationContext
                .<String, Long>newSerializationContext(new StringRedisSerializer())
                .key(new StringRedisSerializer())
                .value(new GenericToStringSerializer<>(Long.class))
                .hashKey(new StringRedisSerializer())
                .hashValue(new GenericToStringSerializer<>(Long.class))
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }

}
