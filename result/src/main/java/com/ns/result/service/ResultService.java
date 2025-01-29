package com.ns.result.service;


import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;
import static com.ns.result.ResultMapper.mapToResultDocument;
import static com.ns.result.ResultMapper.mapToResultReqeustEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.dto.ClientRequest;
import com.ns.common.dto.MembershipEloRequest;
import com.ns.common.events.ResultRequestEvent;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.result.domain.entity.Result;
import com.ns.result.repository.ResultRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {
    @Value("${event.produce.topic}")
    private String eventTopic;

    private final ReactiveKafkaProducerTemplate<String, ResultRequestEvent> eventProducerTemplate;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private final ResultRepository resultRepository;
    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;
    private final ObjectMapper objectMapper;

    private final EloService eloService;
    private final TaskService taskService;
    private final OpenSearchService openSearchService;


    private final int RESULT_SEARCH_SIZE = 30;


    public Mono<Void> sendTask(String topic, Task task) {
        log.info("send [" + topic + "]: " + task.toString());
        String key = task.getTaskID();
        return taskProducerTemplate.send(topic, key, task).then();
    }

    public Mono<Void> updateElo(ResultRequestEvent resultRequest) {
        List<ClientRequest> blueTeams = resultRequest.getBlueTeams();
        List<ClientRequest> redTeams = resultRequest.getRedTeams();

        // requestMemberElo의 리스트를 통해 membershipId,elo 형태로 membership-service로부터 요청을 받는다.
        return Mono.zip(requestMembershipElos(blueTeams), requestMembershipElos(redTeams))
                .flatMap(tuple -> {
                    List<MembershipEloRequest> blueEloRequests = tuple.getT1();
                    List<MembershipEloRequest> redEloRequests = tuple.getT2();

                    log.info("[test] blue MembershipEloRequest : " + blueEloRequests);
                    log.info("[test] red MembershipEloRequest : " + redEloRequests);

                    // 받은 내용을 가지고, 각 팀 별로 TeamElo를 계산한다.
                    Long blueTeamEloSum = eloService.calcTeamEloSum(blueEloRequests);
                    Long redTeamEloSum = eloService.calcTeamEloSum(redEloRequests);

                    log.info("[test] 각 팀별 EloSum(blue, red) : " + blueTeamEloSum + ", " + redTeamEloSum);

                    boolean isWin = resultRequest.getWinTeam().equalsIgnoreCase("blue"); //todo. blue 문자열로 받았었나? 치맨가;;;

                    // TeamElo를 통해 승패에 따른 Elo 점수 변동값을 연산한다.
                    List<MembershipEloRequest> blueTeamNewEloRequests = eloService.updateTeamElo(blueEloRequests,
                            redTeamEloSum, isWin);
                    List<MembershipEloRequest> redTeamNewEloRequests = eloService.updateTeamElo(redEloRequests,
                            blueTeamEloSum, !isWin);

                    // 각 membershipId,newElo 를 requestMemberElo 형태로 membership-service에 다시 전달한다.
                    return updateMembershipEloRequest(blueTeamNewEloRequests, redTeamNewEloRequests);
                });
    }

    private Mono<Void> updateMembershipEloRequest(List<MembershipEloRequest> blueTeamNewEloRequests,
                                                  List<MembershipEloRequest> redTeamNewEloRequests) {
        List<MembershipEloRequest> combinedNewEloRequests = new ArrayList<>();
        combinedNewEloRequests.addAll(blueTeamNewEloRequests);
        combinedNewEloRequests.addAll(redTeamNewEloRequests);

        log.info("[test] 최종 combinedNewEloRequests : " + combinedNewEloRequests);

        return sendTask("task.membership.response",
                sendMembershipNewEloRequests(combinedNewEloRequests));
    }


    // requestMemberElo의 리스트를 통해 membershipId,elo 형태로 membership-service로부터 요청을 받는다.
    private Mono<List<MembershipEloRequest>> requestMembershipElos(List<ClientRequest> teams) {
        Task task = createTask("Result Response",
                null,
                mapTeamMembershipElos(teams));

        return sendTask("task.membership.response", task)
                .then(waitForMembershipEloTaskResult(task.getTaskID())
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<List<MembershipEloRequest>> waitForMembershipEloTaskResult(String taskId) {
        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> taskService.getTaskResults(taskId))
                .filter(Objects::nonNull)
                .take(1)
                .map(task -> convertToMembershipEloRequest(task))
                .next()
                .timeout(Duration.ofSeconds(3))
                .switchIfEmpty(Mono.error(
                        new RuntimeException("Timeout waitForMembershipEloTaskResult for taskId " + taskId)));

    }

    private List<MembershipEloRequest> convertToMembershipEloRequest(Task task) {
        return task.getSubTaskList().stream()
                .filter(subTaskItem -> subTaskItem.getStatus().equals(SubTask.TaskStatus.success))
                .map(subTaskItem -> objectMapper.convertValue(subTaskItem.getData(), MembershipEloRequest.class))
                .toList();
    }

    private List<SubTask> mapTeamMembershipElos(List<ClientRequest> teams) {
        return teams.stream()
                .map(ClientRequest::getMembershipId)
                .map(this::createRequestMembershipEloSubTask)
                .toList();
    }

    private SubTask createRequestMembershipEloSubTask(Long membershipId) {
        return createSubTask(
                "RequestMembershipElo",
                String.valueOf(membershipId),
                SubTask.TaskType.result,
                SubTask.TaskStatus.ready,
                membershipId);
    }


    private Task sendMembershipNewEloRequests(List<MembershipEloRequest> membershipEloRequests) {
        return createTask(
                "Result NewElo Request",
                null,
                membershipEloRequests.stream()
                        .map(this::createSubTaskEloUpdate)
                        .toList());
    }

    private SubTask createSubTaskEloUpdate(MembershipEloRequest membershipEloRequest) {
        return createSubTask("Elo Update",
                String.valueOf(membershipEloRequest.getMembershipId()),
                SubTask.TaskType.membership,
                SubTask.TaskStatus.ready,
                membershipEloRequest.getElo());
    }


    public Mono<Result> saveResult(ResultRequestEvent request) {
        Result document = mapToResultDocument(request);
        return resultRepository.save(document) // todo. OpenSearchService.saveResult(document)
                .doOnTerminate(() -> eventSend(document));
    }

    public Mono<Result> eventSend(Result savedResult) {
        return eventProducerTemplate
                .send(eventTopic, "result-query", mapToResultReqeustEvent(savedResult))
                .doOnSuccess(senderResult -> log.info("ResultRequestEvent 발행됨. " + eventTopic))
                .doOnError(e -> log.error("메시지 전송 중 오류 발생: ", e))
                .thenReturn(savedResult);
    }

    public Flux<Result> getGameResultsByName(String name, int offset) {
        String key = "results:" + name;

        return reactiveRedisTemplate.opsForList().range(key, offset, offset + RESULT_SEARCH_SIZE - 1)
                .map(this::convertJsonToResult)
                .switchIfEmpty(Flux.defer(() -> resultRepository.searchByUserName(name, RESULT_SEARCH_SIZE, offset)
                            .flatMap(result -> {
                                log.info("없어서 조회하고 캐싱한거임.");
                                String resultJson = convertResultToJson(result);
                                return reactiveRedisTemplate.opsForList().rightPush(key, resultJson).thenReturn(result);
                            })
                            .doOnTerminate(() -> {
                                reactiveRedisTemplate.expire(key, Duration.ofHours(1)).subscribe(); // 전적 결과 TTL 1hour
                            })));
    }

    private String convertResultToJson(Result result) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Result convertJsonToResult(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, Result.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public Flux<Result> getGameResultsByMembershipId(Long membershipId, int offset) {
        return resultRepository.searchByMembershipId(membershipId, RESULT_SEARCH_SIZE, offset);
    }

    public Flux<Result> getResultList() {
        return resultRepository.findAll();
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

        ResultRequestEvent dummyResult = ResultRequestEvent.builder()
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


