package com.ns.result.adapter.out;


import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.Task;
import com.ns.result.application.port.out.task.TaskProducerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Mono;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class KafkaTaskProducerAdapter implements TaskProducerPort {
    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;

    @Override
    public Mono<Void> sendTask(String topic, Task task) {
        log.info("send [" + topic + "]: " + task.toString());
        String key = task.getTaskID();
        return taskProducerTemplate.send(topic, key, task).then();
    }
}

