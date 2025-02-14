package com.ns.result.adapter.in.kafka;

import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.result.adapter.axon.command.GameFinishedCommand;
import com.ns.result.adapter.out.kafka.TaskService;
import com.ns.result.application.service.PlayerService;
import java.util.ArrayList;
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

    private final ReactiveKafkaConsumerTemplate<String, Task> taskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> taskResponseConsumerTemplate;
    private final TaskService taskService;
    private final PlayerService playerService;
    private final ObjectMapper objectMapper;

    private final CommandGateway commandGateway;

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
                    List<SubTask> subTasks = task.getSubTaskList();

                    for(var subtask : subTasks){
                        mapSubTaskToResult(task.getTaskID(), subtask);
                        log.info("taskProducerTemplate received : "+subtask);
                    }
                    r.receiverOffset().acknowledge();
                })
                .doOnError(e -> log.error("Error doTaskResponseConsumerTemplate: " + e))
                .subscribe();
    }

    private void mapSubTaskToResult(String taskId, SubTask subtask){
        log.info(subtask.getSubTaskName());

        switch (subtask.getSubTaskName()) {
            case "ReceivedResult":
                mapReceivedResultSubTask(subtask);
                break;
            case "MatchUserHasCodeByMembershipId":
                handleMatchUserHasCode(taskId, subtask);
                break;
            case "MatchPlayerEloByMembershipId":
                handleMatchPlayerElo(taskId, subtask);
                break;
            default:
                log.warn("Unknown subtask: {}", subtask.getSubTaskName());
                break;
        }
    }

    public void mapReceivedResultSubTask(SubTask subtask){
        GameFinishedCommand axonCommand = objectMapper.convertValue(subtask.getData(), GameFinishedCommand.class);

        Mono.fromFuture(() -> commandGateway.send(axonCommand))
                    .doOnSuccess(success -> log.info("GameFinishedCommand sent successfully: " + success))
                    .doOnError(throwable -> log.error("Failed to send GameFinishedCommand: " + throwable))
                    .then().subscribe();
    }

    private void doTaskRequestConsumerTemplate(){
        this.taskRequestConsumerTemplate
                .receive()
                .doOnNext(record -> {
                    taskService.handleTaskRequest(record.value());
                    record.receiverOffset().acknowledge();
                })
                .doOnError(e -> log.error("Error dotaskProducerTemplate: " + e))
                .subscribe();
    }


    private void handleMatchPlayerElo(String taskId, SubTask subtask) {
        String membershipId = subtask.getMembershipId();
        playerService.findByMembershipId(membershipId)
                .flatMap(player -> {
                    List<SubTask> subTasks = createSubTaskListMatchPlayerEloByMembershipId(membershipId, player.getElo());
                    return taskService.sendTask("task.match.request",createTaskMatcPlayerEloByMembershipId(taskId,membershipId,subTasks));
                })
                .doOnError(e -> log.error("Error handleMatchPlayerElo {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private Task createTaskMatcPlayerEloByMembershipId(String taskId, String membershipId, List<SubTask> subTasks){
        return createTask(taskId, "Match Request - UserResponse", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListMatchPlayerEloByMembershipId(String membershipId, Long elo) {
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskMatchPlayerEloByMembershipId(membershipId, elo));
        return subTasks;
    }

    private SubTask createSubTaskMatchPlayerEloByMembershipId(String membershipId, Long elo){
        return createSubTask("MatchPlayerEloByMembershipId",
                String.valueOf(membershipId),
                SubTask.TaskType.match,
                SubTask.TaskStatus.success,
                elo);
    }



    // 해당 사용자가 현재 게임중인지(HasCode) 판별하는 Boolean 값을 전달하는 메소드
    private void handleMatchUserHasCode(String taskId, SubTask subTask){
        String membershipId = subTask.getMembershipId();

        playerService.findByMembershipId(membershipId)
                .flatMap(user -> {
                    log.info(user.getCode() + user.getCode().isBlank());
                    Boolean hasCode = !user.getCode().isBlank();
                    List<SubTask> subTasks = createSubTaskListMatchUserHasCodeByMembershipId(membershipId, hasCode);
                    return taskService.sendTask("task.match.request", createTaskMatchUserHasCodeByMembershipId(taskId, membershipId, subTasks));
                }).subscribe();
    }

    private List<SubTask> createSubTaskListMatchUserHasCodeByMembershipId(String membershipId, Boolean hasCode) {
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskMatchUserHasCodeByMembershipId(membershipId, hasCode));
        return subTasks;
    }

    private Task createTaskMatchUserHasCodeByMembershipId(String taskId, String membershipId, List<SubTask> subTasks){
        return createTask(taskId, "Match Request - UserHasCode", String.valueOf(membershipId), subTasks);
    }

    private SubTask createSubTaskMatchUserHasCodeByMembershipId(String membershipId, Boolean hasCode) {
        return createSubTask("MatchUserHasCodeByMembershipId",
                String.valueOf(membershipId),
                SubTask.TaskType.match,
                SubTask.TaskStatus.success,
                hasCode);
    }

}
