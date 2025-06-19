package com.ns.player.adapter.in.kafka;


import com.ns.common.PlayerQuery;
import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;

import com.ns.player.application.port.out.SendCommandPort;
import com.ns.player.application.port.out.SendQueryPort;
import com.ns.player.application.port.out.task.TaskProducerPort;
import com.ns.player.application.service.TaskConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class KafkaTaskConsumerAdapter implements ApplicationRunner {

    private final ReactiveKafkaConsumerTemplate<String, Task> taskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> taskResponseConsumerTemplate;

    private final TaskProducerPort taskProducerPort;
    private final TaskConsumerService taskConsumerService;

    private final SendQueryPort sendQueryPort;
    private final SendCommandPort sendCommandPort;

    @Override
    public void run(ApplicationArguments args){
        doTaskResponseConsumerTemplate();
        doTaskRequestConsumerTemplate();

        Mono.delay(Duration.ofSeconds(2))
                .doOnNext(ti -> sendWarmupMessage())
                .subscribe();
    }

    private void doTaskResponseConsumerTemplate(){
        this.taskResponseConsumerTemplate
                .receive()
                .doOnNext(r -> {
                    Task task = r.value();

                    for(var subtask : task.getSubTaskList()){
                        mapSubTaskToMembership(task.getTaskID(), subtask);
                        log.info("TaskResponseConsumerTemplate received : "+subtask.getSubTaskName());
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
            case "InitSubTask":
                log.info("WarmUp Kafka Response Consumer Successfully.");
                break;
            case "MatchUserHasCodeByMembershipId":
                handleMatchUserHasCode(taskId, subtask);
                break;
            case "MatchPlayerEloByMembershipId":
                handleMatchPlayerElo(taskId, subtask);
                break;
            case "MatchToPlayerQuery":
                handleMatchPlayerQuery(taskId, subtask);
                break;

            default:
                log.warn("Unknown subtask: {}", subtask.getSubTaskName());
                break;
        }
    }

    private void handleMatchPlayerQuery(String taskId, SubTask subTask){
        String membershipId = subTask.getMembershipId();

        sendQueryPort.sendPlayerQuery(membershipId)
                .flatMap(player -> {
                    List<SubTask> subTasks = new ArrayList<>();
                    subTasks.add(createSubTaskMatchPlayerQuery(membershipId,
                            PlayerQuery.builder()
                                    .membershipId(Long.valueOf(player.getMembershipId()))
                                    .code(player.getCode())
                                    .nickname(player.getNickname())
                                    .elo(player.getElo())
                                    .build())
                    );
                    Task task = createTask(taskId, "MatchToPlayerQuery", String.valueOf(membershipId), subTasks);
                    return taskProducerPort.sendTask("task.match.request", task);
                })
                .doOnError(e -> log.error("Error handleMatchPlayerElo {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private SubTask createSubTaskMatchPlayerQuery(String membershipId, PlayerQuery playerQuery){
        return createSubTask("MatchPlayerQuery", String.valueOf(membershipId), SubTask.TaskType.player, SubTask.TaskStatus.success, playerQuery);
    }


    private void handleMatchPlayerElo(String taskId, SubTask subtask) {
        String membershipId = subtask.getMembershipId();
        sendQueryPort.sendPlayerQuery(membershipId)
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
                SubTask.TaskType.player,
                SubTask.TaskStatus.success,
                elo);
    }



    // 해당 사용자가 현재 게임중인지(HasCode) 판별하는 Boolean 값을 전달하는 메소드
    private void handleMatchUserHasCode(String taskId, SubTask subTask){
        String membershipId = subTask.getMembershipId();

        sendQueryPort.sendPlayerQuery(membershipId)
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
                SubTask.TaskType.player,
                SubTask.TaskStatus.success,
                hasCode);
    }

    private void doTaskRequestConsumerTemplate(){
        this.taskRequestConsumerTemplate
                .receive()
                .doOnNext(record -> {
                    try {
                        Task task = record.value();
                        String taskName = task.getTaskName();

                        if (!task.getSubTaskList().isEmpty() &&
                                "InitSubTask".equals(task.getSubTaskList().get(0).getSubTaskName())) {
                            log.info("WarmUp Kafka Request Consumer Successfully.");
                        }
                        else log.info("received: " + taskName);


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

    private Mono<Void> sendWarmupMessage() {
        String dummyTaskId = "init-kafka-consumer-" + System.currentTimeMillis();
        List<SubTask> subTasks = List.of(createSubTask("InitSubTask", null, SubTask.TaskType.player, SubTask.TaskStatus.success, "warmup"));
        Task warmupTask = createTask(dummyTaskId, "InitTask", "0", subTasks);

        return Mono.when(
                taskProducerPort.sendTask("task.player.response", warmupTask),
                taskProducerPort.sendTask("task.player.request", warmupTask)
        );
    }


}

