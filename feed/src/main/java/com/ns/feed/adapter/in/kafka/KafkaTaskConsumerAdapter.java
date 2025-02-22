package com.ns.feed.adapter.in.kafka;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.feed.adapter.out.kafka.KafkaTaskProducerAdapter;
import com.ns.feed.application.port.in.post.FindPostUseCase;
import com.ns.feed.application.port.out.TaskConsumerPort;
import com.ns.feed.application.port.out.TaskProducerPort;
import com.ns.feed.application.port.out.post.FindPostPort;
import com.ns.feed.application.service.PostService;
import com.ns.feed.application.service.TaskConsumerService;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class KafkaTaskConsumerAdapter implements ApplicationRunner {

    private final ReactiveKafkaConsumerTemplate<String, Task> taskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> taskResponseConsumerTemplate;

    private final FindPostUseCase findPostUseCase;
    private final TaskConsumerService taskConsumerService;

    @Override
    public void run(ApplicationArguments args){
        doTaskResponseConsumerTemplate();
        doTaskRequestConsumerTemplate();
    }

    private void doTaskResponseConsumerTemplate(){
        this.taskResponseConsumerTemplate
                .receive()
                .doOnNext(r -> {
                    Task task = r.value();

                    for(var subtask : task.getSubTaskList()){
                        mapSubTaskToFeed(task.getTaskID(), subtask);
                        log.info("taskProducerTemplate received : "+subtask);
                    }
                    r.receiverOffset().acknowledge();
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

        findPostUseCase.postByMembershipId(taskId,membershipId)
                .doOnError(e -> log.error("PostByMembershipId 처리 중 오류 발생", e))
                .subscribe();
    }


    private void doTaskRequestConsumerTemplate(){
        this.taskRequestConsumerTemplate
                .receive()
                .doOnNext(record -> {
                    taskConsumerService.handleTaskResponse(record.value());
                    record.receiverOffset().acknowledge();
                })
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();
    }


}
