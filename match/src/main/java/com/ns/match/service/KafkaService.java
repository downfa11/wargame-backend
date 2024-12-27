package com.ns.match.service;

import static com.ns.match.service.MatchQueueService.mapper;

import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.match.dto.MatchUserResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService implements ApplicationRunner {
    private final ReactiveKafkaConsumerTemplate<String, Task> TaskRequestConsumerTemplate;
    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;

    private ConcurrentHashMap<String, Task> taskResults = new ConcurrentHashMap<>();
    private final int MAX_TASK_RESULT_SIZE = 5000;


    public Mono<Void> sendTask(String topic, Task task){
        log.info("send ["+topic+"]: "+task.toString());
        String key = task.getTaskID();
        return taskProducerTemplate.send(topic, key, task).then();
    }

    @Override
    public void run(ApplicationArguments args){
        doTaskRequestConsumerTemplate();
    }

    private void doTaskRequestConsumerTemplate(){
        this.TaskRequestConsumerTemplate
                .receiveAutoAck()
                .doOnNext(r -> {
                    Task task = r.value();
                    taskResults.put(task.getTaskID(),task);
                    log.info("TaskRequestConsumerTemplate: "+task);

                    if(taskResults.size() > MAX_TASK_RESULT_SIZE){
                        taskResults.clear();
                        log.info("taskResults clear.");
                    }

                })
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();
    }

    public Mono<MatchUserResponse> waitForUserResponseTaskResult(String taskId) {
        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> taskResults.get(taskId))
                .filter(Objects::nonNull)
                .take(1)
                .map(this::handleMatchUserResponseTask)
                .next()
                .timeout(Duration.ofSeconds(3))
                .switchIfEmpty(Mono.error(new RuntimeException("Timeout waitForUserResponseTaskResult for taskId " + taskId)));
    }

    private MatchUserResponse handleMatchUserResponseTask(Task resultTask){
        return resultTask.getSubTaskList()
                .stream().
                filter(subTaskItem ->
                        subTaskItem.getStatus()
                                .equals(SubTask.TaskStatus.success))
                .map(subTaskItem ->
                        mapper.convertValue(subTaskItem.getData(), MatchUserResponse.class))
                .toList()
                .get(0);
    }

    public Mono<Boolean> waitForUserHasCodeTaskResult(String taskId) {
        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> taskResults.get(taskId))
                .filter(Objects::nonNull)
                .take(1)
                .map(this::handleUserHasCodeTask)
                .next()
                .timeout(Duration.ofSeconds(3))
                .switchIfEmpty(Mono.error(new RuntimeException("Timeout waitForUserHasCodeTaskResult for taskId " + taskId)));
    }

    private Boolean handleUserHasCodeTask(Task resultTask){
        return resultTask.getSubTaskList()
                .stream().
                filter(subTaskItem ->
                        subTaskItem.getStatus()
                                .equals(SubTask.TaskStatus.success))
                .map(subTaskItem -> mapper.convertValue(subTaskItem.getData(), Boolean.class))
                .toList()
                .get(0);
    }
}
