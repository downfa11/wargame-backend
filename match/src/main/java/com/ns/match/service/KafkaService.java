package com.ns.match.service;

import static com.ns.match.service.MatchQueueService.mapper;

import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.match.dto.MatchUserResponse;
import java.time.Duration;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService implements ApplicationRunner {
    private final ReactiveKafkaConsumerTemplate<String, Task> TaskRequestConsumerTemplate;
    private final TaskService taskService;

    @Override
    public void run(ApplicationArguments args){
        doTaskRequestConsumerTemplate();
    }

    private void doTaskRequestConsumerTemplate(){
        this.TaskRequestConsumerTemplate
                .receiveAutoAck()
                .doOnNext(record -> {
                    log.info("received: "+record.value());
                    taskService.handleTaskRequest(record.value());
                })
                .doOnError(e -> log.error("Error doTaskRequestConsumerTemplate: " + e))
                .subscribe();
    }

    public Mono<MatchUserResponse> waitForUserResponseTaskResult(String taskId) {
        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> taskService.getTaskResults(taskId))
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
                .map(tick -> taskService.getTaskResults(taskId))
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
