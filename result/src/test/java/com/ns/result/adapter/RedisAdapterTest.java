package com.ns.result.adapter;


import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.common.ClientRequest;
import com.ns.result.adapter.out.RedisAdapter;
import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveListOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class RedisAdapterTest {

    @Mock private ReactiveListOperations<String, Result> listOperations;
    @Mock private ReactiveRedisTemplate<String, Result> resultRedisTemplate;

    @InjectMocks private RedisAdapter redisAdapter;

    private Result result;

    @BeforeEach
    void init() {
        when(resultRedisTemplate.opsForList()).thenReturn(listOperations);

        result = Result.builder()
                .spaceId("12345")
                .state("success")
                .channel(1)
                .room(101)
                .winTeam("blue")
                .loseTeam("red")
                .blueTeams(List.of(ClientRequest.builder().build(), ClientRequest.builder().build()))
                .redTeams(List.of(ClientRequest.builder().build(), ClientRequest.builder().build()))
                .dateTime("2025-02-01T12:00:00Z")
                .gameDuration(120)
                .build();
    }

    @Test
    void 특정_범위의_전적_데이터를_캐싱_조회하는_메서드() {
        // given
        String key = "testKey";
        int offset = 0;
        when(listOperations.range(key, offset, offset + 29)).thenReturn(Flux.just(result));

        // when
        Flux<Result> resultFlux = redisAdapter.findResultInRange(key, offset);

        // then
        StepVerifier.create(resultFlux)
                .expectNext(result)
                .verifyComplete();

        verify(resultRedisTemplate, times(1)).opsForList();
        verify(listOperations, times(1)).range(key, offset, offset + 29);
    }

    @Test
    void 전적_결과를_캐싱_등록하는_메서드() {
        // given
        String key = "testKey";
        when(listOperations.rightPush(key, result)).thenReturn(Mono.just(1L));
        when(resultRedisTemplate.expire(key, Duration.ofHours(1))).thenReturn(Mono.just(true));

        // when
        Mono<Result> pushedResultMono = redisAdapter.pushResult(key, result);

        // then
        StepVerifier.create(pushedResultMono)
                .expectNext(result)
                .verifyComplete();

        verify(resultRedisTemplate, times(1)).opsForList();
        verify(listOperations, times(1)).rightPush(key, result);
        verify(resultRedisTemplate, times(1)).expire(key, Duration.ofHours(1));
    }
}
