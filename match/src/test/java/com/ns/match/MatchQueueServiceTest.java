package com.ns.match;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ns.match.dto.MatchUserResponse;
import com.ns.match.service.KafkaService;
import com.ns.match.service.MatchQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class MatchQueueServiceTest {

    @Autowired
    private MatchQueueService matchQueueService;

    @Mock
    KafkaService kafkaService;

    @BeforeEach
    void init(){
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void 동시에_매칭_큐에_등록하는_경우() {
        // given
        String queue = "registerQueue";
        Long userId = 5L;

        when(kafkaService.waitForUserHasCodeTaskResult(anyString())).thenReturn(Mono.just(true));
        when(kafkaService.waitForUserResponseTaskResult(anyString()))
                .thenReturn(Mono.just(MatchUserResponse.builder()
                                .membershipId(userId)
                                .name("player")
                                .elo(2000L)
                                .spaceCode("1234")
                                .build()));

        // when (동시에 매칭 등록)
        Mono<String> task1 = matchQueueService.registerMatchQueue(queue, userId);
        Mono<String> task2 = matchQueueService.registerMatchQueue(queue, userId);
        Flux<String> concurrentTasks = Flux.merge(task1, task2);

        // then (중복되는 registerMatchQueue 실행하면 안됨)
        StepVerifier.create(concurrentTasks)
                .expectNextMatches(result -> result.contains("\"userId\":\"" + userId + "\""))
                .expectNextMatches(result -> result.equals("fail"))
                .verifyComplete();
    }


    @Test
    void 동시에_매칭_큐에_취소하는_경우() {
        // given
        String queue = "cancelQueue";
        Long userId = 1L;

        // when (동시에 매칭을 취소)
        when(kafkaService.waitForUserHasCodeTaskResult(anyString())).thenReturn(Mono.just(true));
        when(kafkaService.waitForUserResponseTaskResult(anyString()))
                .thenReturn(Mono.just(MatchUserResponse.builder()
                        .membershipId(userId)
                        .name("player")
                        .elo(2000L)
                        .spaceCode("1234")
                        .build()));
        matchQueueService.registerMatchQueue(queue, userId);

        Mono<Void> task1 = matchQueueService.cancelMatchQueue(userId);
        Mono<Void> task2 = matchQueueService.cancelMatchQueue(userId);
        Flux<Void> concurrentTasks = Flux.merge(task1, task2);

        // then (중복되는 cancelMatchQueue는 실행하면 안됨)
        StepVerifier.create(concurrentTasks)
                .expectNextCount(1)
                .verifyComplete();
    }
}
