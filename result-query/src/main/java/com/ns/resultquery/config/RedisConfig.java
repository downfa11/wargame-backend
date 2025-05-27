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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisConfig {
    private final RedisConnectionFactory redisConnectionFactory;
    private final ObjectMapper objectMapper;

    private <V> RedisTemplate<String, V> createCustomTemplate(ObjectMapper objectMapper, TypeReference<V> typeRef) {
        RedisTemplate<String, V> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new CustomRedisSerializer<>(objectMapper, typeRef));
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, CountSumByChamp> champRedisTemplate() {
        return createCustomTemplate(objectMapper, new TypeReference<>() {});
    }

    @Bean
    public RedisTemplate<String, CountSumByMembership> membershipRedisTemplate() {
        return createCustomTemplate(objectMapper, new TypeReference<>() {});
    }

}