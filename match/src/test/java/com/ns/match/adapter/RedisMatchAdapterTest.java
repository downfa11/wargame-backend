package com.ns.match.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.common.task.Task;
import com.ns.match.adapter.out.RedisMatchAdapter;
import com.ns.match.adapter.out.RedisMatchAdapter.MatchStatus;
import com.ns.match.application.port.out.task.TaskConsumerPort;
import com.ns.match.application.port.out.task.TaskProducerPort;
import com.ns.match.application.service.MatchResponse;
import com.ns.match.application.service.UserMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

@ExtendWith(SpringExtension.class)
class RedisMatchAdapterTest {

    @Mock private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    @Mock private TaskProducerPort taskProducerPort;
    @Mock private TaskConsumerPort taskConsumerPort;

    @InjectMocks private RedisMatchAdapter redisMatchAdapter;

    private UserMatch userMatch;

    @BeforeEach
    void init() {
        userMatch = UserMatch.builder()
                .membershipId("123")
                .elo(1500L)
                .name("user")
                .build();
    }

    @Test
    void 매칭_큐에_사용자가_없는_경우_매칭을_등록하는_메서드() {
        Long userId = 123L;
        String queue = "testQueue";

        when(redisMatchAdapter.getUserHasCode(userId)).thenReturn(Mono.just(false));
        when(redisMatchAdapter.getUserMatch(userId)).thenReturn(Mono.just(userMatch));
        when(reactiveRedisTemplate.opsForZSet().add(anyString(), anyString(), anyDouble())).thenReturn(Mono.just(true));

        Mono<String> result = redisMatchAdapter.registerMatchQueue(queue, userId);

        StepVerifier.create(result)
                .expectNext("{\"userId\":\"123\", \"name\":\"user123\", \"elo\":\"1500\"}")
                .verifyComplete();

        verify(reactiveRedisTemplate.opsForZSet(), times(1)).add(anyString(), anyString(), anyDouble());
    }

    @Test
    void 매칭을_취소하는_메서드() {
        Long userId = 123L;

        when(redisMatchAdapter.getUserMatch(userId)).thenReturn(Mono.just(userMatch));
        when(reactiveRedisTemplate.keys(anyString())).thenReturn(Flux.just("testKey"));
        when(reactiveRedisTemplate.opsForZSet().remove(anyString(), anyString())).thenReturn(Mono.just(1L));

        Mono<Void> result = redisMatchAdapter.cancelMatchQueue(userId);

        StepVerifier.create(result)
                .verifyComplete();

        verify(reactiveRedisTemplate, times(1)).keys(anyString());
        verify(reactiveRedisTemplate.opsForZSet(), times(1)).remove(anyString(), anyString());
    }

    @Test
    void 매칭이_완료된_경우_매칭정보를_반환하는_메서드() {
        Long userId = 123L;
        String key = "matchInfo:123";
        MatchResponse matchResponse = MatchResponse.builder().spaceId("space123").build();

        when(reactiveRedisTemplate.opsForValue().get(key)).thenReturn(Mono.just("{\"spaceId\":\"space123\"}"));
        when(taskProducerPort.sendTask(anyString(), any(Task.class))).thenReturn(Mono.empty());

        Mono<Tuple2<MatchStatus, MatchResponse>> result = redisMatchAdapter.getMatchResponse(userId);

        StepVerifier.create(result)
                .expectNextMatches(tuple -> tuple.getT1() == RedisMatchAdapter.MatchStatus.MATCH_FOUND)
                .verifyComplete();

        verify(reactiveRedisTemplate, times(1)).opsForValue();
    }

    @Test
    void 사용자가_없는_경우_매칭_현황을_조회하는_메서드() {
        Long userId = 123L;

        when(redisMatchAdapter.getUserNickname(userId)).thenReturn(Mono.just("None"));
        Mono<Long> result = redisMatchAdapter.getRank("testQueue", userId);

        StepVerifier.create(result)
                .expectNext(-1L)
                .verifyComplete();
    }

    @Test
    void 사용자가_있는_경우_매칭_현황을_조회하는_메서드() {
        Long userId = 123L;
        String nickname = "user123";

        when(redisMatchAdapter.getUserNickname(userId)).thenReturn(Mono.just(nickname));
        when(reactiveRedisTemplate.opsForZSet().rank(anyString(), anyString())).thenReturn(Mono.just(0L));

        Mono<Long> result = redisMatchAdapter.getRank("testQueue", userId);

        StepVerifier.create(result)
                .expectNext(1L)
                .verifyComplete();
    }
}

