package com.ns.feed.service;

import com.ns.common.SubTask;
import com.ns.common.Task;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService implements ApplicationRunner {
    private static ConcurrentHashMap<String, Task> taskResults = new ConcurrentHashMap<>();

    private final ReactiveKafkaConsumerTemplate<String, Task> TaskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> TaskResponseConsumerTemplate;

    private final PostService postService;
    private final int MAX_TASK_RESULT_SIZE = 5000;

    @Override
    public void run(ApplicationArguments args){
        doTaskResponseConsumerTemplate();
        doTaskRequestConsumerTemplate();
    }

    private void doTaskResponseConsumerTemplate(){
        this.TaskResponseConsumerTemplate
                .receiveAutoAck()
                .doOnNext(r -> {
                    Task task = r.value();

                    for(var subtask : task.getSubTaskList()){
                        mapSubTaskToFeed(task.getTaskID(), subtask);
                        log.info("TaskResponseConsumerTemplate received : "+subtask);
                    }
                })
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();
    }

    private void mapSubTaskToFeed(String taskId, SubTask subtask){
        String taskName = subtask.getSubTaskName().trim();

        switch (taskName) {
            case "PostByMembershipId":
                String membershipId = String.valueOf(subtask.getData());
                handlePostByMembershipId(taskId, membershipId);
                break;
            default:
                log.warn("Unknown subtask: {}", taskName);
                break;
        }
    }

    private void handlePostByMembershipId(String taskId, String membershipId){
        if (membershipId.isEmpty() || membershipId.isBlank())
            return;

        postService.postByMembershipId(taskId,membershipId)
                    .doOnError(e -> log.error("PostByMembershipId 처리 중 오류 발생", e))
                    .subscribe();
    }


    private void doTaskRequestConsumerTemplate(){
        this.TaskRequestConsumerTemplate
                .receiveAutoAck()
                .doOnNext(record -> handleTaskRequest(record.value()))
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();
    }

    private void handleTaskRequest(Task task){
            taskResults.put(task.getTaskID(), task);

            if (taskResults.size() > MAX_TASK_RESULT_SIZE) {
                taskResults.clear();
                log.info("taskResults cleared due to exceeding the maximum size.");
            }
    }


    public static Mono<String> waitForGetUserNameTaskComment(String taskId) {
        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> taskResults.get(taskId))
                .filter(Objects::nonNull)
                .take(1)
                .map(task -> {
                    SubTask subTask = task.getSubTaskList().get(0);
                    return String.valueOf(subTask.getData());
                })
                .next()
                .timeout(Duration.ofSeconds(3))
                .switchIfEmpty(Mono.error(new RuntimeException("Timeout waitForUserPostsTaskComment for taskId " + taskId)));
    }


    public static Mono<String> waitForGetUserNameTaskFeed(String taskId) {
        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> taskResults.get(taskId))
                .filter(Objects::nonNull)
                .take(1)
                .map(task -> {
                    SubTask subTask = task.getSubTaskList().get(0);
                    return String.valueOf(subTask.getData());
                })
                .next()
                .timeout(Duration.ofSeconds(3))
                .switchIfEmpty(Mono.error(new RuntimeException("Timeout waitForGetUserNameTaskFeed for taskId " + taskId)));
    }

}
