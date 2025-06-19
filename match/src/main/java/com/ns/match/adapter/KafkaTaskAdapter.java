package com.ns.match.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.PlayerQuery;
import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.match.application.port.out.task.TaskConsumerPort;
import com.ns.match.application.port.out.task.TaskProducerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class KafkaTaskAdapter implements ApplicationRunner, TaskConsumerPort, TaskProducerPort {

    private final ReactiveKafkaConsumerTemplate<String, Task> taskRequestConsumerTemplate;
    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;

    private final ObjectMapper mapper;
    private ConcurrentHashMap<String, Task> taskResults = new ConcurrentHashMap<>();
    private final int MAX_TASK_RESULT_SIZE = 5000;


    @Override
    public void run(ApplicationArguments args) {
        this.taskRequestConsumerTemplate
                .receive()
                .doOnNext(record -> {
                    try {
                        log.info("received: " + record.value());
                        handleTaskRequest(record.value());
                        record.receiverOffset().acknowledge();
                    } catch (Exception e) {
                        log.error("Exception in handleTaskRequest", e);
                        record.receiverOffset().acknowledge();
                    }
                })
                .onErrorContinue((e, o) -> log.error("Stream error occurred: {}", e.toString()))
                .subscribe();
    }

    private void handleTaskRequest(Task task){
        taskResults.put(task.getTaskID(), task);

        if (taskResults.size() > MAX_TASK_RESULT_SIZE) {
            taskResults.clear();
        }
    }

    @Override
    public Task getTaskResults(String taskId){
        log.info("getTaskResults " + taskId + ":" + taskResults.get(taskId).getSubTaskList());
        return taskResults.get(taskId);
    }

    @Override
    public Mono<Void> sendTask(String topic, Task task){
        log.info("send ["+topic+"]: "+task.toString());
        String key = task.getTaskID();
        return taskProducerTemplate.send(topic, key, task).then();
    }

    public Mono<PlayerQuery> waitForPlayerQuery(String taskId) {
        return Flux.interval(Duration.ofMillis(1000))
                .map(tick -> getTaskResults(taskId))
                .filter(Objects::nonNull)
                .take(1)
                .map(this::handlePlayerQuery)
                .filter(Objects::nonNull)
                .next()
                .timeout(Duration.ofSeconds(1))
                .switchIfEmpty(Mono.error(new RuntimeException("Timeout waitForUserResponseTaskResult for taskId " + taskId)));
    }

    private PlayerQuery handlePlayerQuery(Task resultTask){
        log.info("handleMatchUserEloTask" + resultTask.toString());

        return resultTask.getSubTaskList()
                .stream().filter(subTaskItem ->
                        subTaskItem.getStatus().equals(SubTask.TaskStatus.success))
                .map(subTaskItem -> {
                    Object data = subTaskItem.getData();
                    PlayerQuery query = mapper.convertValue(data, PlayerQuery.class);
                    return query;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
