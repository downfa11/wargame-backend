package com.ns.match.adapter.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.match.application.port.out.task.TaskConsumerPort;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class KafkaTaskConsumerAdapter implements ApplicationRunner, TaskConsumerPort {

    private final ReactiveKafkaConsumerTemplate<String, Task> taskRequestConsumerTemplate;
    private final ObjectMapper mapper;
    private ConcurrentHashMap<String, Task> taskResults = new ConcurrentHashMap<>();

    private final int MAX_TASK_RESULT_SIZE = 5000;

    @Override
    public void run(ApplicationArguments args){
        doTaskRequestConsumerTemplate();
    }

    private void doTaskRequestConsumerTemplate(){
        this.taskRequestConsumerTemplate
                .receive()
                .doOnNext(record -> {
                    log.info("received: "+record.value());
                    handleTaskRequest(record.value());
                    record.receiverOffset().acknowledge();
                })
                .doOnError(e -> log.error("Error doTaskRequestConsumerTemplate: " + e))
                .subscribe();
    }

    public Mono<Long> waitForUserResponseTaskResult(String taskId) {
        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> getTaskResults(taskId))
                .filter(Objects::nonNull)
                .take(1)
                .map(this::handleMatchUserEloTask)
                .next()
                .timeout(Duration.ofSeconds(3))
                .switchIfEmpty(Mono.error(new RuntimeException("Timeout waitForUserResponseTaskResult for taskId " + taskId)));
    }

    private Long handleMatchUserEloTask(Task resultTask){
        return resultTask.getSubTaskList()
                .stream().filter(subTaskItem ->
                        subTaskItem.getStatus().equals(SubTask.TaskStatus.success))
                .map(subTaskItem -> {
                    Object data = subTaskItem.getData();

                    if (data instanceof Integer)
                        return ((Integer) data).longValue();
                    return (Long) data;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public Mono<Boolean> waitForUserHasCodeTaskResult(String taskId) {
        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> getTaskResults(taskId))
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
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public Mono<String> waitForUserNameTaskResult(String taskId){
        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> getTaskResults(taskId))
                .filter(Objects::nonNull)
                .take(1)
                .map(this::handleUserNameTask)
                .next()
                .timeout(Duration.ofSeconds(3))
                .switchIfEmpty(Mono.error(new RuntimeException("Timeout waitForUserNameTaskResult for taskId " + taskId)));
    }

    private String handleUserNameTask(Task resultTask){
        return resultTask.getSubTaskList()
                .stream().
                filter(subTaskItem ->
                        subTaskItem.getStatus()
                                .equals(SubTask.TaskStatus.success))
                .map(subTaskItem -> mapper.convertValue(subTaskItem.getData(), String.class))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Task getTaskResults(String taskId){
        log.info("getTaskResults " + taskId);
        return taskResults.get(taskId);
    }

    public void handleTaskRequest(Task task){
        taskResults.put(task.getTaskID(), task);

        if (taskResults.size() > MAX_TASK_RESULT_SIZE) {
            taskResults.clear();
        }
    }

}
