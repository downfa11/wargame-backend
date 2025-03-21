package com.ns.resultquery.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.utils.CustomRedisSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisConfig {
    private final ReactiveRedisConnectionFactory redisConnectionFactory;
    private final ObjectMapper objectMapper;

    @Bean
    public ReactiveRedisOperations<String, CountSumByChamp> champRedisTemplate() {
        return createCustomTemplate(objectMapper, new TypeReference<>() {});
    }

    @Bean
    public ReactiveRedisOperations<String, CountSumByMembership> membershipRedisTemplate() {
        return createCustomTemplate(objectMapper, new TypeReference<>() {});
    }


    private <V> ReactiveRedisOperations<String, V> createCustomTemplate(ObjectMapper objectMapper, TypeReference<V> typeRef) {
        RedisSerializationContext.RedisSerializationContextBuilder<String, V> builder = RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, V> context =
                builder.key(new StringRedisSerializer())
                        .value(new CustomRedisSerializer<>(objectMapper, typeRef))
                        .build();

        return new ReactiveRedisTemplate<>(redisConnectionFactory, context);
    }
}