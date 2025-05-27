package com.ns.result.adapter.in.kafka;


import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.result.application.service.TaskConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class KafkaTaskConsumerAdapter implements ApplicationRunner {

    private final ReactiveKafkaConsumerTemplate<String, Task> taskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> taskResponseConsumerTemplate;

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
                        mapSubTaskToMembership(task.getTaskID(), subtask);
                        log.info("TaskResponseConsumerTemplate received : "+subtask);
                    }
                    r.receiverOffset().acknowledge();
                })
                .doOnError(e -> log.error("Error doTaskResponseConsumerTemplate: " + e))
                .subscribe();
    }

    private void mapSubTaskToMembership(String taskId, SubTask subtask){
        switch (subtask.getSubTaskName()) {
            default:
                log.warn("Unknown subtask: {}", subtask.getSubTaskName());
                break;
        }
    }


    private void doTaskRequestConsumerTemplate(){
        this.taskRequestConsumerTemplate
                .receive()
                .doOnNext(record -> {
                    try {
                        log.info("received: " + record.value());
                        taskConsumerService.handleTaskResponse(record.value());
                        record.receiverOffset().acknowledge();
                    } catch (Exception e) {
                        log.error("Exception in handleTaskRequest", e);
                        record.receiverOffset().acknowledge();
                    }
                })
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();
    }

}

