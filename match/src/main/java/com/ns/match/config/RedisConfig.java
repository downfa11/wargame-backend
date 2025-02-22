package com.ns.match.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.data.redis.host}")
    private String REDISSON_HOST;

    @Value("${spring.data.redis.port}")
    private String REDISSON_PORT;


    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;


    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        reactiveRedisTemplate.opsForValue().get("1")
                .doOnSuccess(i -> log.info("Initialize to redis"))
                .doOnError((err) -> log.error("Failed to init redis: {}", err.getMessage()))
                .subscribe();

        // reactiveRedisTemplate.opsForList().leftPush("list1","hello").subscribe();

    }

    @Bean
    public ReactiveRedisTemplate<String, Long> reactiveRedisTemplate_long(
            ReactiveRedisConnectionFactory connectionFactory) {
        RedisSerializationContext<String, Long> serializationContext = RedisSerializationContext
                .<String, Long>newSerializationContext(new StringRedisSerializer())
                .key(new StringRedisSerializer())
                .value(new GenericToStringSerializer<>(Long.class))
                .hashKey(new StringRedisSerializer())
                .hashValue(new GenericToStringSerializer<>(Long.class))
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }

    @Bean
    public RedissonReactiveClient redissonReactiveClient() {
        return createRedissonClient().reactive();
    }

    private RedissonClient createRedissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://"+REDISSON_HOST+":"+REDISSON_PORT);
        return Redisson.create(config);
    }


}