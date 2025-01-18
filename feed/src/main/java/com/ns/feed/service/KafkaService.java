package com.ns.feed.service;

import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService implements ApplicationRunner {

    private final ReactiveKafkaConsumerTemplate<String, Task> TaskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> TaskResponseConsumerTemplate;

    private final TaskService taskService;
    private final PostService postService;


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
                .doOnError(e -> log.error("Error doTaskResponseConsumerTemplate: " + e))
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
                .doOnNext(record -> taskService.handleTaskRequest(record.value()))
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();
    }


}
