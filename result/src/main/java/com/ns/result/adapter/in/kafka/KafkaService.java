package com.ns.result.adapter.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.events.ResultRequestEvent;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.result.adapter.axon.GameFinishedCommand;
import com.ns.result.adapter.out.kafka.TaskService;
import com.ns.result.application.service.DodgeService;
import com.ns.result.application.service.ResultService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService implements ApplicationRunner {

    private final ReactiveKafkaConsumerTemplate<String, Task> TaskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> TaskResponseConsumerTemplate;
    private final ResultService resultService;
    private final TaskService taskService;
    private final DodgeService dodgeService;
    private final ObjectMapper objectMapper;

    private final CommandGateway commandGateway;

    @Override
    public void run(ApplicationArguments args){
        doTaskResponseConsumerTemplate();
        doTaskRequestConsumerTemplate();
    }

    private void doTaskResponseConsumerTemplate(){
        this.TaskResponseConsumerTemplate
                .receive()
                .doOnNext(r -> {
                    Task task = r.value();
                    List<SubTask> subTasks = task.getSubTaskList();

                    for(var subtask : subTasks){
                        mapSubTaskToResult(subtask);
                        log.info("TaskResponseConsumerTemplate received : "+subtask);
                    }
                    r.receiverOffset().acknowledge();
                })
                .doOnError(e -> log.error("Error doTaskResponseConsumerTemplate: " + e))
                .subscribe();
    }

    private void mapSubTaskToResult(SubTask subtask){
        switch (subtask.getSubTaskName()) {
            case "ReceivedResult":
                mapReceivedResultSubTask(subtask);
                break;

            default:
                log.warn("Unknown subtask: {}", subtask.getSubTaskName());
                break;
        }
    }

    public void mapReceivedResultSubTask(SubTask subtask){
            ResultRequestEvent result = objectMapper.convertValue(subtask.getData(), ResultRequestEvent.class);

            GameFinishedCommand axonCommand = new GameFinishedCommand(result);

            Mono.fromFuture(() -> commandGateway.send(axonCommand))
                    .doOnSuccess(success -> log.info("GameFinishedCommand sent successfully: " + success))
                    .doOnError(throwable -> log.error("Failed to send GameFinishedCommand: " + throwable))
                    .then().subscribe();

    }

    private void doTaskRequestConsumerTemplate(){
        this.TaskRequestConsumerTemplate
                .receive()
                .doOnNext(record -> {
                    taskService.handleTaskRequest(record.value());
                    record.receiverOffset().acknowledge();
                })
                .doOnError(e -> log.error("Error doTaskRequestConsumerTemplate: " + e))
                .subscribe();
    }

}
