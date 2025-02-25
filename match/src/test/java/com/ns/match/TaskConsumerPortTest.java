package com.ns.match;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.match.application.port.out.task.TaskConsumerPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class TaskConsumerPortTest {

    @Mock
    private TaskConsumerPort taskConsumerPort;

    private String taskName = "taskId";

    @Test
    void 회원_서비스로부터_사용자의_응답을_받아오는_메서드() {
        // given
        Long expectedResponse = 100L;

        // when
        when(taskConsumerPort.waitForUserResponseTaskResult(taskName)).thenReturn(Mono.just(expectedResponse));

        // then
        Mono<Long> result = taskConsumerPort.waitForUserResponseTaskResult(taskName);

        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(taskConsumerPort, times(1)).waitForUserResponseTaskResult(taskName);
    }

    @Test
    void 회원_서비스로부터_사용자의_정보를_받아올때_대기시간을_초과한_경우() {
        // given
        when(taskConsumerPort.waitForUserResponseTaskResult(taskName)).thenReturn(Mono.error(new RuntimeException("Timeout")));

        // when
        Mono<Long> result = taskConsumerPort.waitForUserResponseTaskResult(taskName);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().contains("Timeout"))
                .verify();

        verify(taskConsumerPort, atLeast(1)).waitForUserResponseTaskResult(taskName);
    }

    @Test
    void 회원_서비스로부터_사용자의_코드_존재여부를_받아오는_메서드() {
        // given
        Boolean expectedResponse = true;

        // when
        when(taskConsumerPort.waitForUserHasCodeTaskResult(taskName)).thenReturn(Mono.just(expectedResponse));

        // then
        Mono<Boolean> result = taskConsumerPort.waitForUserHasCodeTaskResult(taskName);

        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(taskConsumerPort, times(1)).waitForUserHasCodeTaskResult(taskName);
    }

    @Test
    void 전적_서비스로부터_사용자의_코드_존재여부를_받아올때_대기시간을_초과한_경우() {
        // given
        when(taskConsumerPort.waitForUserHasCodeTaskResult(taskName)).thenReturn(Mono.error(new RuntimeException("Timeout")));

        // when
        Mono<Boolean> result = taskConsumerPort.waitForUserHasCodeTaskResult(taskName);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().contains("Timeout"))
                .verify();

        verify(taskConsumerPort, atLeast(1)).waitForUserHasCodeTaskResult(taskName);
    }

    @Test
    void 회원_서비스로부터_사용자의_이름을_받아오는_메서드() {
        // given
        String expectedResponse = "JohnDoe";

        // when
        when(taskConsumerPort.waitForUserNameTaskResult(taskName)).thenReturn(Mono.just(expectedResponse));

        // then
        Mono<String> result = taskConsumerPort.waitForUserNameTaskResult(taskName);

        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(taskConsumerPort, times(1)).waitForUserNameTaskResult(taskName);
    }

    @Test
    void 회원_서비스로부터_사용자의_이름을_받아올때_대기시간을_초과한_경우() {
        // given
        when(taskConsumerPort.waitForUserNameTaskResult(taskName)).thenReturn(Mono.error(new RuntimeException("Timeout")));

        // when
        Mono<String> result = taskConsumerPort.waitForUserNameTaskResult(taskName);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().contains("Timeout"))
                .verify();

        verify(taskConsumerPort, atLeast(1)).waitForUserNameTaskResult(taskName);
    }
}
