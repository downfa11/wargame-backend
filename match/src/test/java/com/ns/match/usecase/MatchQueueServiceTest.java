package com.ns.match.usecase;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ns.match.application.port.in.CancleMatchQueueUseCase;
import com.ns.match.application.port.in.RegisterMatchQueueUseCase;
import com.ns.match.application.port.out.task.TaskConsumerPort;
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
public class MatchQueueServiceTest {

    private RegisterMatchQueueUseCase registerMatchQueueUseCase;
    private CancleMatchQueueUseCase cancleMatchQueueUseCase;

    @Mock TaskConsumerPort taskConsumerPort;


    @Test
    void 동시에_매칭_큐에_등록하는_경우() {
        // given
        String queue = "registerQueue";
        Long userId = 5L;

        when(taskConsumerPort.waitForUserHasCodeTaskResult(anyString())).thenReturn(Mono.just(true));
        when(taskConsumerPort.waitForUserResponseTaskResult(anyString()))
                .thenReturn(Mono.just(2000L));

        // when (동시에 매칭 등록)
        Mono<String> task1 = registerMatchQueueUseCase.registerMatchQueue(queue, userId);
        Mono<String> task2 = registerMatchQueueUseCase.registerMatchQueue(queue, userId);
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
        when(taskConsumerPort.waitForUserHasCodeTaskResult(anyString())).thenReturn(Mono.just(true));
        when(taskConsumerPort.waitForUserResponseTaskResult(anyString())).thenReturn(Mono.just(2000L));
        registerMatchQueueUseCase.registerMatchQueue(queue, userId);

        Mono<Void> task1 = cancleMatchQueueUseCase.cancelMatchQueue(userId);
        Mono<Void> task2 = cancleMatchQueueUseCase.cancelMatchQueue(userId);
        Flux<Void> concurrentTasks = Flux.merge(task1, task2);

        // then (중복되는 cancelMatchQueue는 실행하면 안됨)
        StepVerifier.create(concurrentTasks)
                .expectNextCount(1)
                .verifyComplete();
    }
}
