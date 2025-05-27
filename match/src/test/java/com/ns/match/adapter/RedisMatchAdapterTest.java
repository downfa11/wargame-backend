package com.ns.match.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.PlayerQuery;
import com.ns.common.task.Task;
import com.ns.match.adapter.out.RedisMatchAdapter;
import com.ns.match.application.port.out.task.TaskConsumerPort;
import com.ns.match.application.port.out.task.TaskProducerPort;
import com.ns.match.dto.MatchResponse;
import com.ns.match.dto.UserMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class RedisMatchAdapterTest {

    @Mock private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    @Mock private TaskProducerPort taskProducerPort;
    @Mock private TaskConsumerPort taskConsumerPort;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private RedisMatchAdapter redisMatchAdapter;

    private final Long userId = 123L;
    private PlayerQuery playerQuery;


    @BeforeEach
    void init() {
        redisMatchAdapter = new RedisMatchAdapter(reactiveRedisTemplate, taskProducerPort, taskConsumerPort, objectMapper);
        playerQuery = PlayerQuery.builder()
                .membershipId(userId)
                .elo(1500L)
                .nickname("user123")
                .code("not_playing")
                .build();
    }

    @Test
    void 매칭_큐에_사용자가_없는_경우_매칭을_등록하는_메서드() {
        String memberKey = userId + ":" + playerQuery.getNickname();

        when(taskProducerPort.sendTask(anyString(), any(Task.class))).thenReturn(Mono.empty());
        when(taskConsumerPort.waitForPlayerQuery(anyString())).thenReturn(Mono.just(playerQuery));
        when(reactiveRedisTemplate.opsForZSet().add(anyString(), anyString(), anyDouble())).thenReturn(Mono.just(true));
        when(reactiveRedisTemplate.expire(anyString(), any())).thenReturn(Mono.just(true));

        Mono<Boolean> result = redisMatchAdapter.registerMatchQueue(userId);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(reactiveRedisTemplate.opsForZSet(), times(1)).add(anyString(), eq(memberKey), eq(1500.0));
    }

    @Test
    void 매칭을_취소하는_메서드() {
        String memberKey = userId + ":" + playerQuery.getNickname();

        when(taskProducerPort.sendTask(anyString(), any(Task.class))).thenReturn(Mono.empty());
        when(taskConsumerPort.waitForPlayerQuery(anyString())).thenReturn(Mono.just(playerQuery));
        when(reactiveRedisTemplate.keys(anyString())).thenReturn(Flux.just("someKey"));
        when(reactiveRedisTemplate.opsForZSet().remove(anyString(), eq(memberKey))).thenReturn(Mono.just(1L));

        Mono<Void> result = redisMatchAdapter.cancelMatchQueue(userId);

        StepVerifier.create(result).verifyComplete();
        verify(reactiveRedisTemplate.opsForZSet(), atLeastOnce()).remove(anyString(), eq(memberKey));
    }

    @Test
    void 매칭_결과가_있는_경우_정상_응답() {
        MatchResponse mockResponse = MatchResponse.builder().spaceId("space123").build();
        String redisValue = "{\"spaceId\":\"space123\"}";
        String key = "matchInfo:" + userId;

        when(reactiveRedisTemplate.opsForValue().get(key)).thenReturn(Mono.just(redisValue));
        when(taskProducerPort.sendTask(anyString(), any(Task.class))).thenReturn(Mono.empty());
        when(reactiveRedisTemplate.unlink(key)).thenReturn(Mono.just(1L));

        Mono<MatchResponse> result = redisMatchAdapter.getMatchResponse(userId);

        StepVerifier.create(result)
                .expectNextMatches(res -> "space123".equals(res.getSpaceId()))
                .verifyComplete();
    }

    @Test
    void 매칭_결과가_없는_경우_빈_응답() {
        String key = "matchInfo:" + userId;

        when(reactiveRedisTemplate.opsForValue().get(key)).thenReturn(Mono.empty());
        Mono<MatchResponse> result = redisMatchAdapter.getMatchResponse(userId);

        StepVerifier.create(result)
                .expectNextMatches(match -> match.getSpaceId().isBlank())
                .verifyComplete();
    }
}

