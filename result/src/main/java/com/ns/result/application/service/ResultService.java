package com.ns.result.application.service;


import static com.ns.common.task.TaskUseCase.createTask;
import static com.ns.result.adapter.out.persistence.elasticsearch.ResultMapper.mapToResultDocument;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.ClientRequest;
import com.ns.common.GameFinishedEvent;
import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import com.ns.result.adapter.out.persistence.elasticsearch.ResultRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ResultRepository resultRepository;
    private final int RESULT_SEARCH_SIZE = 30;


    public Flux<Result> getResultList() {
        return resultRepository.findAll();
    }

    public Mono<Result> saveResult(GameFinishedEvent gameFinishedEvent) {
        return resultRepository.save(mapToResultDocument(gameFinishedEvent)); // todo. OpenSearchService.saveResult(document)
    }

    public Flux<Result> getGameResultsByName(String name, int offset) {
        String key = "results:name:" + name + "offset:" + offset;

        return reactiveRedisTemplate.opsForList().range(key, 0, -1)
                .doOnNext(results -> log.info("캐시에서 가져옴"))
                .map(this::convertJsonToResult)
                .switchIfEmpty(Flux.defer(() -> resultRepository.searchByUserName(name, RESULT_SEARCH_SIZE, offset)
                        .flatMap(result -> {
                            log.info("캐시가 없어");
                            String resultJson = convertResultToJson(result);
                            return reactiveRedisTemplate.opsForList().rightPush(key, resultJson)
                                    .thenReturn(result);
                        })
                        .doOnTerminate(() -> reactiveRedisTemplate.expire(key, Duration.ofHours(1)).subscribe())));
    }

    public Flux<Result> getGameResultsByMembershipId(Long membershipId, int offset) {
        String key = "results:membershipId:" + membershipId + "offset:" + offset;

        return reactiveRedisTemplate.opsForList().range(key, 0, -1)
                .doOnNext(results -> log.info("캐시에서 가져옴"))
                .map(this::convertJsonToResult)
                .switchIfEmpty(Flux.defer(() -> resultRepository.searchByMembershipId(membershipId, RESULT_SEARCH_SIZE, offset)
                        .flatMap(result -> {
                            log.info("캐시가 없어");
                            String resultJson = convertResultToJson(result);
                            return reactiveRedisTemplate.opsForList().rightPush(key, resultJson)
                                    .thenReturn(result);
                        })
                        .doOnTerminate(() -> reactiveRedisTemplate.expire(key, Duration.ofHours(1)).subscribe())));
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


    //=============== test ================//
    public Mono<Result> createResultTemp() {
        Random random = new Random();

        ClientRequest bluePlayer1 = createRandomClientRequest(1L, 1L, "Blue", "BluePlayer1");
        ClientRequest bluePlayer2 = createRandomClientRequest(2L, 2L, "Blue", "BluePlayer2");

        ClientRequest redPlayer1 = createRandomClientRequest(3L, 3L, "Red", "RedPlayer1");
        ClientRequest redPlayer2 = createRandomClientRequest(4L, 4L, "Red", "RedPlayer2");

        String winningTeam = random.nextBoolean() ? "Blue" : "Red";
        String losingTeam = winningTeam.equals("Blue") ? "Red" : "Blue";

        GameFinishedEvent dummyResult = GameFinishedEvent.builder()
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


