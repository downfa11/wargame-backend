package com.ns.match.adapter;

import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.match.adapter.out.KafkaTaskProducerAdapter;
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


    @Mock private ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;
    @InjectMocks private KafkaTaskProducerAdapter kafkaTaskProducerAdapter;


    @Test
    void Task를_발행하는_메서드() {
        // given
        Task task = createTask("Match Response", "123",
                List.of(createSubTask("match", "123", SubTask.TaskType.post, SubTask.TaskStatus.ready, 123L)));
        String topic = "task.match.response";
        String taskId = task.getTaskID();

        // when
        when(taskProducerTemplate.send(any(String.class), any(String.class), any(Task.class))).thenReturn(Mono.empty());

        Mono<Void> result = kafkaTaskProducerAdapter.sendTask(topic, task);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(taskProducerTemplate, times(1)).send(topic, taskId, task);
    }
}