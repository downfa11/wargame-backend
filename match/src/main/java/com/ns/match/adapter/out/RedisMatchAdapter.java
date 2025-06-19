package com.ns.match.adapter.out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.PlayerQuery;
import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.match.application.port.out.CancelMatchQueuePort;
import com.ns.match.application.port.out.GetMatchQueuePort;
import com.ns.match.application.port.out.IntegrationTestMatchPort;
import com.ns.match.application.port.out.RegisterMatchQueuePort;
import com.ns.match.application.port.out.task.TaskConsumerPort;
import com.ns.match.application.port.out.task.TaskProducerPort;
import com.ns.match.dto.MatchResponse;
import com.ns.match.dto.UserMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class RedisMatchAdapter implements RegisterMatchQueuePort, CancelMatchQueuePort, GetMatchQueuePort, IntegrationTestMatchPort {

    @Value("${spring.var.matchexpiretime:3}")
    private int expireTime;

    private final String MATCH_WAIT_KEY ="users:queue:wait";
    private final String MATCH_WAIT_KEY_FOR_SCAN ="users:queue:wait";


    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final TaskProducerPort taskProducerPort;
    private final TaskConsumerPort taskConsumerPort;
    public final ObjectMapper mapper;

    @Override
    public Mono<PlayerQuery> getPlayerQuery(Long membershipId) {
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskPlayerQuery(membershipId));
        Task task = createTask("MatchToPlayerQuery", String.valueOf(membershipId), subTasks);

        return taskProducerPort.sendTask("task.player.response", task)
                .then(taskConsumerPort.waitForPlayerQuery(task.getTaskID())
                        .doOnNext(result -> log.info("[{}] waitForPlayerQuery 결과 수신: {}", task.getTaskID(), result))
                        .doOnSuccess(res -> log.info("[{}] waitForPlayerQuery 완료", task.getTaskID()))
                )
                .filter(result -> {
                    boolean valid = result != null;
                    if (!valid) {
                        log.warn("[{}] 결과가 null임 (filter에서 제거됨)", task.getTaskID());
                    }
                    return valid;
                })
                .onErrorResume(error -> {
                    log.error("[{}] 에러 발생: {}", task.getTaskID(), error.toString());
                    return Mono.empty();
                })
                .doOnTerminate(() -> log.info("[{}] 전체 흐름 종료", task.getTaskID()))
                .subscribeOn(Schedulers.boundedElastic());

    }


    private SubTask createSubTaskPlayerQuery(Long membershipId){
        return createSubTask("MatchToPlayerQuery",
                String.valueOf(membershipId),
                SubTask.TaskType.match,
                SubTask.TaskStatus.ready,
                membershipId);
    }

    @Override
    public Mono<Boolean> registerMatchQueue(Long userId) {
        // 애초에 맨 처음에 MatchData(membershipId, elo, hasCode, nickname)으로 구성된 VO를 전달받기
        return getPlayerQuery(userId)
                .flatMap(playerQuery -> {
                    if (playerQuery.getCode().isBlank()) {
                        log.info("게임중이 아니라서 매칭 큐에 진입합니다.");

                        String memberKey = playerQuery.getMembershipId() + ":" + playerQuery.getNickname();
                        Double elo = playerQuery.getElo().doubleValue();
                        return handleRegisterMatch(memberKey, elo);
                    }
                    else return Mono.just(false);
                });
    }

    @Override
    public Mono<Void> cancelMatchQueue(Long userId) {
        return getPlayerQuery(userId)
                .flatMap(playerQuery -> {
                    String memberKey = playerQuery.getMembershipId() + ":" + playerQuery.getNickname();
                    return handleCancelMatch(memberKey);
                })
                .onErrorResume(e -> Mono.error(new RuntimeException("cancelMatchQueue error : ", e)));
    }

    @Override
    public Mono<MatchResponse> getMatchResponse(Long memberId) {
        String key = "matchInfo:" + memberId;

        return handleMatchResponseFromQueue(key, memberId)
                .switchIfEmpty(Mono.just(new MatchResponse()));
    }


    private Mono<Boolean> handleRegisterMatch(String memberKey, Double elo) {
        return reactiveRedisTemplate.opsForZSet().add(MATCH_WAIT_KEY, memberKey, elo)
                .flatMap(zset -> reactiveRedisTemplate.expire(MATCH_WAIT_KEY, Duration.ofSeconds(expireTime)));
    }

    private Mono<Void> handleCancelMatch(String memberKey) {
        return Flux.concat(
                reactiveRedisTemplate.keys(MATCH_WAIT_KEY.formatted("*"))
                        .flatMap(key -> reactiveRedisTemplate.opsForZSet().remove(key, memberKey)),

                reactiveRedisTemplate.keys(MATCH_WAIT_KEY_FOR_SCAN.formatted("*"))
                        .flatMap(key -> reactiveRedisTemplate.opsForZSet().remove(key, memberKey))
        ).then();
    }

    private Mono<MatchResponse> handleMatchResponseFromQueue(String key, Long memberId){
        return reactiveRedisTemplate.opsForValue().get(key)
                .flatMap(matchResponseStr -> {
                    try {
                        MatchResponse matchResponse = mapper.readValue(matchResponseStr, MatchResponse.class);

                        List<SubTask> subTasks = new ArrayList<>();
                        subTasks.add(createSubTaskMatchCodeUpdate(memberId, matchResponse.getSpaceId()));
                        Task task = createTask("Match Request", null, subTasks);

                        return taskProducerPort.sendTask("task.membership.response", task)
                                .then(reactiveRedisTemplate.unlink(key))
                                .then(Mono.just(matchResponse));

                    } catch (JsonProcessingException e) {
                        log.error("getMatchResponse JsonProcessingException: ", e);
                        return Mono.error(e);
                    }
                });
    }

    private SubTask createSubTaskMatchCodeUpdate(Long memberId, String spaceId){
        return createSubTask("MatchCodeUpdate", String.valueOf(memberId), SubTask.TaskType.match, SubTask.TaskStatus.ready, spaceId);
    }

    @Override
    public Mono<Long> calculateRank(Long userId) {
        log.info("Member: " + userId);

        return reactiveRedisTemplate.opsForZSet().rank(MATCH_WAIT_KEY, String.valueOf(userId))
                .defaultIfEmpty(-1L)
                .map(rank -> {
                    log.info("Rank: " + rank);

                    if (rank >= 0) {
                        reactiveRedisTemplate.expire(MATCH_WAIT_KEY, Duration.ofSeconds(expireTime)).subscribe();
                        return rank + 1; // Redis rank는 0 기반이므로 1을 더함
                    }
                    return rank;
                })
                .doOnError(error -> log.error("Error calculateRank: ", error));
    }


    //============Integration Test===========================

    @Override
    public Mono<Void> requestIntegrationTest(Long memberId, String nickName, Long elo) {
        String queue = "integrationTestQueue";
        String userKey = memberId+ ":" + nickName;

        return reactiveRedisTemplate.opsForZSet()
                .add(MATCH_WAIT_KEY.formatted(queue), userKey, elo.doubleValue())
                .then();
    }

    @Override
    public Mono<Long> getRequestCount() {

        return reactiveRedisTemplate.opsForZSet()
                .size(MATCH_WAIT_KEY)
                .doOnNext(size -> log.info("ZSet size: {}", size));
    }
}
