package com.ns.result.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.common.TaskUseCase;
import com.ns.result.domain.ClientRequest;
import com.ns.result.domain.ResultRequest;
import com.ns.result.domain.entity.Result;
import com.ns.result.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService implements ApplicationRunner {


    private final ResultRepository resultRepository;

    private final ReactiveKafkaConsumerTemplate<String, Task> TaskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> TaskResponseConsumerTemplate;
    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;

    private final TaskUseCase taskUseCase;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Task> taskResults = new ConcurrentHashMap<>();
    private final int MAX_TASK_RESULT_SIZE = 5000;

    public Mono<Void> sendTask(String topic, Task task){
        log.info("send ["+topic+"]: "+task.toString());
        String key = task.getTaskID();
        return taskProducerTemplate.send(topic, key, task).then();
    }

    private Map<Long ,Long> requestTeamElo(List<Long> membershipIds){
        return new HashMap<>();
    }

//    public Mono<Void> receiveTeamElo(ResultRequest resultRequest) {
//        List<ClientRequest> blueTeams = resultRequest.getBlueTeams();
//        List<ClientRequest> redTeams = resultRequest.getRedTeams();
//
//        List<Long> blueMembershipIds = blueTeams.stream()
//                .map(ClientRequest::getMembershipId).toList();
//
//        List<Long> redMembershipIds = redTeams.stream()
//                .map(ClientRequest::getMembershipId).toList();
//
//        return Mono.zip(
//                requestTeamElo(blueMembershipIds),
//                requestTeamElo(redMembershipIds),
//                (blueEloMap, redEloMap) -> {
//                    boolean blueTeamWins = resultRequest.getWinTeam().equalsIgnoreCase("Blue");
//
//                    Task blueTeamTask = createTaskForTeam("Blue Team ELO Update", blueTeams, blueEloMap, blueTeamWins);
//                    Task redTeamTask = createTaskForTeam("Red Team ELO Update", redTeams, redEloMap, !blueTeamWins);
//
//                    return Flux.concat(
//                            sendTask(blueTeamTask),
//                            sendTask(redTeamTask)
//                    ).then();
//                }
//        );
//    }



    private long calculateTotalElo(Map<Long,Long> team) {
        return team.values().stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    private Mono<Void> updateTeamElo(List<ClientRequest> team, boolean isWinner) {
        return Flux.fromIterable(team)
                .collectMap(
                        ClientRequest::getMembershipId,
                        client -> {
                            Long currentElo = client.getMembershipId();
                            // 상대 팀의 평균 ELO 계산
                            Long opposingTeamElo = (long) team.stream().mapToLong(ClientRequest::getMembershipId).average().orElse(0);
                            // 새로운 ELO 계산
                            return calculateElo(currentElo, opposingTeamElo, isWinner);
                        }
                )
                .flatMap(updatedEloMap -> {
                    List<SubTask> subTasks = createSubTasksUpdateElo(updatedEloMap);

                    return sendTask("task.membership.response", taskUseCase.createTask(
                            "Elo Update",
                            null,
                            subTasks));
                });
    }

    private long calculateElo(long currentElo, long opposingTeamElo, boolean isWinner) {
        final int K = 16;
        final double EA = 1.0 / (1.0 + Math.pow(10, (opposingTeamElo - currentElo) / 400.0));
        int SA = isWinner ? 1 : 0;
        return (long) (currentElo + K * (SA - EA));
    }

    private Task createTaskForTeam(String taskName, List<ClientRequest> team, Map<Long, Long> teamEloMap, boolean isWinner) {
        List<SubTask> subTasks = team.stream()
                .map(client -> {
                    Long currentElo = teamEloMap.getOrDefault(client.getMembershipId(), 0L);
                    Long opposingTeamElo = calculateTotalElo(teamEloMap) / team.size();
                    Long newElo = calculateElo(currentElo, opposingTeamElo, isWinner);

                    return  taskUseCase.createSubTask("Elo Update",
                            String.valueOf(client.getMembershipId()),
                            SubTask.TaskType.membership,
                            SubTask.TaskStatus.ready,
                            newElo);

                })
                .toList();

        return taskUseCase.createTask(
                taskName,
                null,
                subTasks);
    }


    private List<SubTask> createSubTasksUpdateElo(Map<Long, Long> updatedEloMap) {
        List<SubTask> subTasks = new ArrayList<>();
        updatedEloMap.forEach((membershipId, newElo) -> {
            subTasks.add(
                    taskUseCase.createSubTask("Elo Update",
                    String.valueOf(membershipId),
                    SubTask.TaskType.membership,
                    SubTask.TaskStatus.ready,
                    newElo));
        });
        return subTasks;
    }


    public Mono<Result> saveResult(ResultRequest request) {
        Result document = mapToResultDocument(request);
        return resultRepository.save(document);
    }

    private Result mapToResultDocument(ResultRequest request) {
        return Result.builder()
                .spaceId(request.getSpaceId())
                .state("success")
                .channel(request.getChannel())
                .room(request.getRoom())
                .winTeam(request.getWinTeam())
                .loseTeam(request.getLoseTeam())
                .blueTeams(request.getBlueTeams())
                .redTeams(request.getRedTeams())
                .dateTime(request.getDateTime())
                .gameDuration(request.getGameDuration())
                .build();
    }

    public Mono<Void> dodge(ResultRequest result) {

        List<ClientRequest> allTeams = new ArrayList<>();
        allTeams.addAll(result.getBlueTeams());
        allTeams.addAll(result.getRedTeams());

        if (allTeams.isEmpty()) {
            log.warn("Win과 lose 팀이 비어 있습니다!");
            return Mono.empty();
        }

        return Flux.fromIterable(allTeams)
                .flatMap(client -> {
                    Long index = client.getMembershipId();

                    return Mono.just(
                            taskUseCase.createSubTask("Dodge",
                            String.valueOf(index),
                            SubTask.TaskType.result,
                            SubTask.TaskStatus.ready,
                            index));

                })
                .collectList()
                .flatMap(subTasks -> sendTask("task.membership.response", taskUseCase.createTask(
                            "Dodge Request",
                            null,
                            subTasks))
                );
    }


    public Flux<Result> getGameResultsByName(String name) {
        return resultRepository.searchByUserName(name);
    }

    @Override
    public void run(ApplicationArguments args){

        this.TaskResponseConsumerTemplate
                .receiveAutoAck()
                .doOnNext(r -> {
                    Task task = r.value();

                    List<SubTask> subTasks = task.getSubTaskList();

                    for(var subtask : subTasks){

                        log.info("TaskResponseConsumerTemplate received : "+subtask.toString());
                        try {
                            switch (subtask.getSubTaskName()) {
                                case "ReceivedResult":
                                    ResultRequest result = objectMapper.convertValue(subtask.getData(),ResultRequest.class);
                                    saveResult(result);
                                    log.info("save result : "+result);
                                    break;
                                default:
                                    log.warn("Unknown subtask: {}", subtask.getSubTaskName());
                                    break;
                            }
                        } catch (Exception e) {
                            log.error("Error processing subtask {}: {}", subtask.getSubTaskName(), e.getMessage());
                        }
                    }
                })
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();


        this.TaskRequestConsumerTemplate
                .receiveAutoAck()
                .doOnNext(r -> {
                    Task task = r.value();
                    taskResults.put(task.getTaskID(),task);

                    if(taskResults.size() > MAX_TASK_RESULT_SIZE){
                        taskResults.clear();
                        log.info("taskResults clear.");
                    }

                })
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();
    }
}

