package com.ns.result.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.MembershipEloRequest;
import com.ns.common.ResultRequestEvent;
import com.ns.common.SubTask;
import com.ns.common.Task;
import java.time.Duration;
import java.util.List;
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

    private final ReactiveKafkaConsumerTemplate<String, Task> TaskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> TaskResponseConsumerTemplate;
    private final ResultService resultService;
    private final TaskService taskService;
    private final DodgeService dodgeService;
    private final ObjectMapper objectMapper;



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
                    List<SubTask> subTasks = task.getSubTaskList();

                    for(var subtask : subTasks){
                        mapSubTaskToResult(subtask);
                        log.info("TaskResponseConsumerTemplate received : "+subtask);
                    }
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

            if ("success".equals(result.getState())) {
                resultService.updateElo(result)
                            .doOnSuccess(resultId -> log.info("Updated Elo : " + resultId))
                        .then(resultService.saveResult(result)
                                .doOnSuccess(savedResult -> log.info("Result saved: " + savedResult)))
                        .subscribe();

            } else if ("dodge".equals(result.getState())) {
                dodgeService.dodge(result)
                        .doOnSuccess(dodgedResult -> log.info("Result dodged: " + dodgedResult))
                        .subscribe();
            } else {
                log.warn("Unknown state: " + result.getState());
            }
    }

    private void doTaskRequestConsumerTemplate(){
        this.TaskRequestConsumerTemplate
                .receiveAutoAck()
                .doOnNext(record -> taskService.handleTaskRequest(record.value()))
                .doOnError(e -> log.error("Error doTaskRequestConsumerTemplate: " + e))
                .subscribe();
    }

}
