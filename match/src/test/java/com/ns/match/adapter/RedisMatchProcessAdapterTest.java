package com.ns.match.adapter;


import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.match.adapter.out.RedisMatchProcessAdapter;
import com.ns.match.application.port.out.task.TaskProducerPort;
import com.ns.match.application.service.MatchResponse;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
class RedisMatchProcessAdapterTest {

    @Mock private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    @Mock private TaskProducerPort taskProducerPort;
    @Mock private ObjectMapper objectMapper;

    private RedisMatchProcessAdapter redisMatchProcessAdapter;


    @BeforeEach
    void init() {
        redisMatchProcessAdapter = new RedisMatchProcessAdapter(reactiveRedisTemplate, taskProducerPort, objectMapper);
    }

    @Test
    void 매칭_큐_프로세스_성공시() {
        String queue = "queue1";
        String member1 = "member1";
        String member2 = "member2";
        List<String> members = Arrays.asList(member1, member2);

        when(reactiveRedisTemplate.opsForZSet().popMin("users:queue1:wait", 2L))
                .thenAnswer(invocation -> Flux.fromIterable(members));
        when(taskProducerPort.sendTask(any(), any())).thenReturn(Mono.empty());

        Mono<Void> result = redisMatchProcessAdapter.processQueue(queue);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(reactiveRedisTemplate, times(1)).opsForZSet().popMin("users:queue1:wait", 2L);
        verify(taskProducerPort, times(1)).sendTask(eq("task.match.response"), any());
    }

    @Test
    void 매칭_큐_프로세스_방별_인원을_충족하지_못하는_경우() {
        String queue = "queue1";
        String member1 = "member1";
        List<String> members = Arrays.asList(member1);

        when(reactiveRedisTemplate.opsForZSet().popMin("users:queue1:wait", 2L))
                .thenAnswer(invocation -> Flux.fromIterable(members));

        Mono<Void> result = redisMatchProcessAdapter.processQueue(queue);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(reactiveRedisTemplate, times(1)).opsForZSet().popMin("users:queue1:wait", 2L);
    }

    @Test
    void 매칭에_성공한_경우() {
        String queue = "queue1";
        String member1 = "member1";
        String member2 = "member2";
        List<String> members = Arrays.asList(member1, member2);

        String spaceId = UUID.randomUUID().toString();
        MatchResponse matchResponse = MatchResponse.fromMembers(spaceId, members);

        when(taskProducerPort.sendTask(any(), any())).thenReturn(Mono.empty());

        Mono<Void> result = redisMatchProcessAdapter.handleMatchFound(queue, members);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(taskProducerPort, times(1)).sendTask(eq("task.match.response"), any());
        verify(reactiveRedisTemplate, times(1)).opsForZSet().remove(eq("users:queue1:wait"), any());
    }

    @Test
    void 매칭에_실패한_경우() {
        String queue = "queue1";
        String member1 = "member1";
        List<String> members = Arrays.asList(member1);

        Mono<Void> result = redisMatchProcessAdapter.handleMatchError(queue, members);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(reactiveRedisTemplate, times(1)).opsForZSet().add(eq("users:queue1:wait"), eq(member1), eq(0.0));
    }

    @Test
    void 매칭_결과를_기록하는_메서드() throws JsonProcessingException {
        String memberId = "member1";
        String spaceId = UUID.randomUUID().toString();
        MatchResponse matchResponse = MatchResponse.fromMembers(spaceId, Arrays.asList(memberId));

        when(objectMapper.writeValueAsString(matchResponse)).thenReturn("json");

        redisMatchProcessAdapter.saveMatchInfo(memberId, matchResponse);
        verify(reactiveRedisTemplate, times(1)).opsForValue().set(eq("matchInfo:member1"), eq("json"));
    }

    @Test
    void 모든_종류의_매칭_큐에_대한_프로세스를_진행하는_메서드() {
        List<String> queues = Arrays.asList("queue1", "queue2");

        when(reactiveRedisTemplate.scan(any())).thenReturn(Flux.fromIterable(queues));
        when(reactiveRedisTemplate.executeInSession(any())).thenReturn(Flux.empty());

        Mono<Void> result = redisMatchProcessAdapter.processAllQueue(queues);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(reactiveRedisTemplate, times(1)).scan(any());
        verify(reactiveRedisTemplate, times(2)).executeInSession(any());
    }
}
