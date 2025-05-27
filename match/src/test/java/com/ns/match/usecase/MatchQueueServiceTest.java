package com.ns.match.usecase;

import com.ns.match.application.port.in.CancleMatchQueueUseCase;
import com.ns.match.application.port.in.RegisterMatchQueueUseCase;
import com.ns.match.application.port.out.task.TaskConsumerPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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

        // when
        Mono<Boolean> task1 = registerMatchQueueUseCase.registerMatchQueue(queue, userId);
        Mono<Boolean> task2 = registerMatchQueueUseCase.registerMatchQueue(queue, userId);
        Flux<Boolean> concurrentTasks = Flux.merge(task1, task2);

        // then
        StepVerifier.create(concurrentTasks)
                .expectNext(true)
                .expectNext(false)
                .verifyComplete();
    }


    @Test
    void 동시에_매칭_큐에_취소하는_경우() {
        // given
        String queue = "cancelQueue";
        Long userId = 1L;

        // when
        registerMatchQueueUseCase.registerMatchQueue(queue, userId);

        Mono<Void> task1 = cancleMatchQueueUseCase.cancelMatchQueue(userId);
        Mono<Void> task2 = cancleMatchQueueUseCase.cancelMatchQueue(userId);
        Flux<Void> concurrentTasks = Flux.merge(task1, task2);

        // then
        StepVerifier.create(concurrentTasks)
                .expectNextCount(1)
                .verifyComplete();
    }
}
