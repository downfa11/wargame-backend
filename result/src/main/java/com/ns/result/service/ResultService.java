package com.ns.result.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.*;
import com.ns.result.domain.ResultRequest;
import com.ns.result.domain.entity.Result;
import com.ns.result.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService implements ApplicationRunner {


    private final ResultRepository resultRepository;

    private final ReactiveKafkaConsumerTemplate<String, Task> TaskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> TaskResponseConsumerTemplate;
    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;

    private final ReactiveKafkaProducerTemplate<String, ResultRequestEvent> eventProducerTemplate;

    private final TaskUseCase taskUseCase;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Task> taskResults = new ConcurrentHashMap<>();
    private final int MAX_TASK_RESULT_SIZE = 5000;

    @Value("${event.produce.topic}")
    private String eventTopic;

    public Mono<Void> sendTask(String topic, Task task){
        log.info("send ["+topic+"]: "+task.toString());
        String key = task.getTaskID();
        return taskProducerTemplate.send(topic, key, task).then();
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

    public Mono<Void> updateElo(ResultRequest resultRequest) {
        List<ClientRequest> blueTeams = resultRequest.getBlueTeams();
        List<ClientRequest> redTeams = resultRequest.getRedTeams();

        // requestMemberElo의 리스트를 통해 membershipId,elo 형태로 membership-service로부터 요청을 받는다.
        return Mono.zip(requestMembershipElos(blueTeams),  requestMembershipElos(redTeams))
                .flatMap(tuple -> {
                    List<MembershipEloRequest> blueEloRequests = tuple.getT1();
                    List<MembershipEloRequest> redEloRequests = tuple.getT2();
                    log.info("[test] blue MembershipEloRequest : "+ blueEloRequests);
                    log.info("[test] red MembershipEloRequest : "+ redEloRequests);

                    // 받은 내용을 가지고, 각 팀 별로 TeamElo를 계산한다.
                    Long blueTeamEloSum = blueEloRequests.stream().mapToLong(MembershipEloRequest::getElo).sum();
                    Long redTeamEloSum = redEloRequests.stream().mapToLong(MembershipEloRequest::getElo).sum();

                    log.info("[test] 각 팀별 EloSum(blue, red) : "+ blueTeamEloSum+", "+redTeamEloSum);
                    boolean isWin = resultRequest.getWinTeam().equalsIgnoreCase("blue"); //todo. blue 문자열로 받았었나? 치맨가;;;

                    // TeamElo를 통해 승패에 따른 Elo 점수 변동값을 연산한다.
                    Mono<List<MembershipEloRequest>> blueTeamNewEloRequests = updateTeamElo(blueEloRequests, redTeamEloSum, isWin);
                    Mono<List<MembershipEloRequest>> redTeamNewEloRequests = updateTeamElo(redEloRequests, blueTeamEloSum, !isWin);

                    // 각 membershipId,newElo 를 requestMemberElo 형태로 membership-service에 다시 전달한다.
                    return Mono.zip(blueTeamNewEloRequests, redTeamNewEloRequests)
                            .flatMap(newEloTuple -> {
                                List<MembershipEloRequest> combinedNewEloRequests = new ArrayList<>();
                                combinedNewEloRequests.addAll(newEloTuple.getT1());
                                combinedNewEloRequests.addAll(newEloTuple.getT2());

                                log.info("[test] 최종 combinedNewEloRequests : "+ combinedNewEloRequests);

                                Task task = sendMembershipNewEloRequests(combinedNewEloRequests);
                                return sendTask("task.membership.response", task);
                            });
                });
    }


    // requestMemberElo의 리스트를 통해 membershipId,elo 형태로 membership-service로부터 요청을 받는다.
    private Mono<List<MembershipEloRequest>> requestMembershipElos(List<ClientRequest> teams){
        List<SubTask> subTasks = teams.stream()
                .map(ClientRequest::getMembershipId)
                .map(membershipId -> taskUseCase.createSubTask(
                        "RequestMembershipElo",
                        String.valueOf(membershipId),
                        SubTask.TaskType.result,
                        SubTask.TaskStatus.ready,
                        membershipId)
                )
                .toList();

        Task task = taskUseCase.createTask(
                "Result Response",
                null,
                subTasks
        );

        return sendTask("task.membership.response", task)
                .then(waitForMembershipEloTaskResult(task.getTaskID()));
    }

    private Mono<List<MembershipEloRequest>> waitForMembershipEloTaskResult(String taskId) {
        return Mono.defer(() -> {
            return Mono.fromCallable(() -> {
                        while (true) {
                            Task resultTask = taskResults.get(taskId);
                            if (resultTask != null) {
                                List<MembershipEloRequest> membershipEloRequests = convertToMembershipEloRequest(resultTask);
                                return membershipEloRequests;
                            }
                            Thread.sleep(500);
                        }
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .timeout(Duration.ofSeconds(3))
                    .onErrorResume(e -> Mono.error(new RuntimeException("waitForMembershipEloTaskResult error : ", e)));
        });
    }

    private List<MembershipEloRequest> convertToMembershipEloRequest(Task task){
        return task.getSubTaskList().stream()
                .filter(subTaskItem -> subTaskItem.getStatus().equals(SubTask.TaskStatus.success))
                .map(subTaskItem -> objectMapper.convertValue(subTaskItem.getData(), MembershipEloRequest.class))
                .toList();
    }

    // TeamElo를 통해 승패에 따른 Elo 점수 변동값을 연산한다.
    private Mono<List<MembershipEloRequest>> updateTeamElo(List<MembershipEloRequest> team, Long opposingTeamEloSum, boolean isWinner) {
        // OpposeTeam의 EloSum으로 승패 여부에 따라 연산해야함
        return Mono.fromCallable(() -> {
            return team.stream()
                    .map(membershipEloRequest -> {
                        long currentElo = membershipEloRequest.getElo();
                        long newElo = calculateElo(currentElo, opposingTeamEloSum, isWinner);
                        membershipEloRequest.setElo(newElo);
                        return membershipEloRequest;
                    })
                    .toList();
        });
    }

    private long calculateElo(long currentElo, long opposingTeamElo, boolean isWinner) {
        final int K = 16;
        final double EA = 1.0 / (1.0 + Math.pow(10, (opposingTeamElo - currentElo) / 400.0));
        int SA = isWinner ? 1 : 0;
        return (long) (currentElo + K * (SA - EA));
    }

    private Task sendMembershipNewEloRequests(List<MembershipEloRequest> membershipEloRequests) {
        List<SubTask> subTasks = membershipEloRequests.stream()
                .map(membershipEloRequest -> {
                    return  taskUseCase.createSubTask("Elo Update",
                            String.valueOf(membershipEloRequest.getMembershipId()),
                            SubTask.TaskType.membership,
                            SubTask.TaskStatus.ready,
                            membershipEloRequest.getElo());

                })
                .toList();

        return taskUseCase.createTask(
                "Result NewElo Request",
                null,
                subTasks);
    }


    public Mono<Result> saveResult(ResultRequest request) {
        Result document = mapToResultDocument(request);
        return resultRepository.save(document)
                .doOnSuccess(saveResult -> {
                    ResultRequestEvent event = new ResultRequestEvent()
                            .builder()
                            .spaceId(saveResult.getSpaceId())
                            .blueTeams(saveResult.getBlueTeams())
                            .redTeams(saveResult.getRedTeams())
                            .winTeam(saveResult.getWinTeam())
                            .loseTeam(saveResult.getLoseTeam())
                            .dateTime(saveResult.getDateTime())
                            .gameDuration(saveResult.getGameDuration()).build();

                    eventProducerTemplate.send(eventTopic, "result-query", event)
                            .doOnSuccess(senderResult -> log.info("ResultRequestEvent 발행됨. " + eventTopic))
                            .doOnError(e -> log.error("메시지 전송 중 오류 발생: ", e)).subscribe();
                });
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


    public Flux<Result> getGameResultsByName(String name, int offset) {
        int size = 30;
        return resultRepository.searchByUserName(name, size, offset);
    }

    public Flux<Result> getGameResultsByMembershipId(Long membershipId, int offset) {
        int size = 30;
        return resultRepository.searchByMembershipId(membershipId, size, offset);
    }

    public Flux<Result> getResultList(){
        return resultRepository.findAll();
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

                                    if ("success".equals(result.getState())) {
                                        updateElo(result)
                                                .doOnSuccess(resultId -> log.info("Updated Elo : "+ resultId));
                                        saveResult(result)
                                                .doOnSuccess(savedResult -> log.info("Result saved: " + savedResult))
                                                .subscribe();

                                    } else if ("dodge".equals(result.getState())) {
                                        dodge(result)
                                                .doOnSuccess(dodgedResult -> log.info("Result dodged: " + dodgedResult))
                                                .subscribe();
                                    } else {
                                        log.warn("Unknown state: " + result.getState());
                                    }



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

    //=============== test ================//
    public Mono<Result> createResultTemp() {
        Random random = new Random();

        ClientRequest bluePlayer1 = createRandomClientRequest(1L, 1L, "Blue", "BluePlayer1");
        ClientRequest bluePlayer2 = createRandomClientRequest(2L, 2L, "Blue", "BluePlayer2");

        ClientRequest redPlayer1 = createRandomClientRequest(3L, 3L, "Red", "RedPlayer1");
        ClientRequest redPlayer2 = createRandomClientRequest(4L, 4L, "Red", "RedPlayer2");

        String winningTeam = random.nextBoolean() ? "Blue" : "Red";
        String losingTeam = winningTeam.equals("Blue") ? "Red" : "Blue";

        ResultRequest dummyResult = ResultRequest.builder()
                .spaceId("dummy-space-id")
                .state("success")
                .channel(random.nextInt(10))
                .room(random.nextInt(10))
                .winTeam(winningTeam)
                .loseTeam(losingTeam)
                .blueTeams(List.of(bluePlayer1, bluePlayer2))
                .redTeams(List.of(redPlayer1, redPlayer2))
                .dateTime(String.valueOf(LocalDateTime.now()))
                .gameDuration(random.nextInt(7200))
                .build();

        return saveResult(dummyResult);
    }

    private ClientRequest createRandomClientRequest(Long membershipId, Long champIndex, String team, String userName) {
        Random random = new Random();

        return ClientRequest.builder()
                .membershipId(membershipId)
                .socket(random.nextInt(100))
                .champindex(champIndex)
                .user_name(userName)
                .team(team)
                .channel(random.nextInt(5))
                .room(random.nextInt(10))
                .kill(random.nextInt(10))
                .death(random.nextInt(10))
                .assist(random.nextInt(10))
                .gold(random.nextInt(10000))
                .level(random.nextInt(18) + 1)
                .maxhp(random.nextInt(100))
                .maxmana(random.nextInt(100))
                .attack(random.nextInt(30))
                .critical(random.nextInt(100))
                .criProbability(random.nextInt(100))
                .attrange(random.nextInt(1000))
                .attspeed(random.nextFloat() * 2)
                .movespeed(random.nextInt(100))
                .itemList(List.of(random.nextInt(100), random.nextInt(100), random.nextInt(100))) // 3개의 랜덤 아이템
                .build();
    }

}


