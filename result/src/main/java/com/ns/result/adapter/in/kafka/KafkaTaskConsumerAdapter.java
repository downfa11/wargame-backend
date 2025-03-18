package com.ns.result.adapter.in.kafka;


import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.result.adapter.axon.command.GameFinishedCommand;
import com.ns.result.application.port.out.SendCommandPort;
import com.ns.result.application.port.out.player.FindPlayerPort;
import com.ns.result.application.port.out.task.TaskProducerPort;
import com.ns.result.application.service.TaskConsumerService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import reactor.core.publisher.Mono;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class KafkaTaskConsumerAdapter implements ApplicationRunner {

    private final ReactiveKafkaConsumerTemplate<String, Task> taskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> taskResponseConsumerTemplate;

    private final TaskProducerPort taskProducerPort;
    private final TaskConsumerService taskConsumerService;

    private final FindPlayerPort findPlayerPort;
    private final SendCommandPort sendCommandPort;

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
            case "ReceivedResult":
                sendCommandPort.sendReceivedGameFinishedEvent(subtask);
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



    private void handleMatchPlayerElo(String taskId, SubTask subtask) {
        String membershipId = subtask.getMembershipId();
        findPlayerPort.findByMembershipId(membershipId)
                .flatMap(player -> {
                    List<SubTask> subTasks = createSubTaskListMatchPlayerEloByMembershipId(membershipId, player.getElo());
                    return taskProducerPort.sendTask("task.match.request",createTaskMatcPlayerEloByMembershipId(taskId,membershipId,subTasks));
                })
                .switchIfEmpty(Mono.fromRunnable(() -> log.warn("handleMatchPlayerElo: Not found membershipId=" + membershipId)))
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

        findPlayerPort.findByMembershipId(membershipId)
                .flatMap(user -> {
                    log.info(user.getCode() + user.getCode().isBlank());
                    Boolean hasCode = !user.getCode().isBlank();
                    List<SubTask> subTasks = createSubTaskListMatchUserHasCodeByMembershipId(membershipId, hasCode);
                    return taskProducerPort.sendTask("task.match.request", createTaskMatchUserHasCodeByMembershipId(taskId, membershipId, subTasks));
                })
                .switchIfEmpty(Mono.fromRunnable(() -> log.warn("handleMatchUserHasCode: Not found membershipId=" + membershipId)))
                .subscribe();
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

