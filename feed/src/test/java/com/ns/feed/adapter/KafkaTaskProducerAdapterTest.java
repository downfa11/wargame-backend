package com.ns.feed.adapter;

import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.feed.adapter.out.kafka.KafkaTaskProducerAdapter;
import com.ns.feed.application.port.out.TaskConsumerPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class KafkaTaskProducerAdapterTest {

    @Mock private TaskConsumerPort taskConsumerPort;
    @Mock private ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;

    @InjectMocks private KafkaTaskProducerAdapter kafkaTaskProducerAdapter;


    @Test
    void Task를_발행하는_메서드() {
        // given
        Task task = createTask("Post Response", "123", List.of(createSubTask("PostUserName", "123", SubTask.TaskType.post, SubTask.TaskStatus.ready, 123L)));
        String topic = "task.membership.response";
        String taskId = task.getTaskID();

        // when
        when(taskProducerTemplate.send(any(String.class), any(String.class), any(Task.class))).thenReturn(Mono.empty());

        Mono<Void> result = kafkaTaskProducerAdapter.sendTask(topic, task);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(taskProducerTemplate, times(1)).send(topic, taskId, task);
    }

    @Test
    void 게시글의_작성자_이름을_조회하는_메서드() {
        // given
        Long membershipId = 1001L;
        Task task = createTask("Post Response", String.valueOf(membershipId), List.of(createSubTask("PostUserName", String.valueOf(membershipId), SubTask.TaskType.post, SubTask.TaskStatus.ready, membershipId)));

        when(taskConsumerPort.waitForGetUserNameTaskFeed(any(String.class))).thenReturn(Mono.just("player1"));
        when(taskProducerTemplate.send(any(String.class), any(String.class), any(Task.class))).thenReturn(Mono.empty());

        // when
        Mono<String> result = kafkaTaskProducerAdapter.getUserNameByPost(membershipId);

        // then
        StepVerifier.create(result)
                .expectNext("player1")
                .verifyComplete();

        verify(taskProducerTemplate, times(1)).send("task.membership.response", task.getTaskID(), task);
        verify(taskConsumerPort, times(1)).waitForGetUserNameTaskFeed(task.getTaskID());
    }

    @Test
    void 댓글의_작성자_이름을_조회하는_메서드() {
        // given
        Long membershipId = 1001L;
        Task task = createTask("Comment Response", String.valueOf(membershipId), List.of(createSubTask("CommentUserName", String.valueOf(membershipId), SubTask.TaskType.post, SubTask.TaskStatus.ready, membershipId)));

        when(taskConsumerPort.waitForGetUserNameTaskComment(eq(task.getTaskID()))).thenReturn(Mono.just("player1"));
        when(taskProducerTemplate.send(eq("task.membership.response"), eq(task.getTaskID()), eq(task))).thenReturn(Mono.empty());

        // when
        Mono<String> result = kafkaTaskProducerAdapter.getUserNameByComment(membershipId);

        // then
        StepVerifier.create(result)
                .expectNext("player1")
                .verifyComplete();

        verify(taskProducerTemplate, times(1)).send(eq("task.membership.response"), eq(task.getTaskID()), eq(task));
        verify(taskConsumerPort, times(1)).waitForGetUserNameTaskComment(eq(task.getTaskID()));
    }

}
