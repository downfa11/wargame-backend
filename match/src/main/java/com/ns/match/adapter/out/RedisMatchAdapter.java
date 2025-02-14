package com.ns.match.adapter.out;

import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.match.application.port.out.CancelMatchQueuePort;
import com.ns.match.application.port.out.GetMatchQueuePort;
import com.ns.match.application.port.out.IntegrationTestMatchPort;
import com.ns.match.application.port.out.RegisterMatchQueuePort;
import com.ns.match.application.port.out.task.TaskConsumerPort;
import com.ns.match.application.port.out.task.TaskProducerPort;
import com.ns.match.application.service.MatchResponse;
import com.ns.match.application.service.UserMatch;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class RedisMatchAdapter implements RegisterMatchQueuePort, CancelMatchQueuePort, GetMatchQueuePort,
        IntegrationTestMatchPort {


    @Value("${spring.var.matchexpiretime:3}")
    private int expireTime;

    @Value("${spring.var.nicknameexpiretime:300}")
    private int nickNameExpireTime;


    private final String MATCH_WAIT_KEY ="users:queue:%s:wait";
    private final String MATCH_WAIT_KEY_FOR_SCAN ="users:queue:*:wait";

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final TaskProducerPort taskProducerPort;
    private final TaskConsumerPort taskConsumerPort;
    public final ObjectMapper mapper = new ObjectMapper();


    public enum MatchStatus {
        MATCH_FOUND, MATCHING, NO_MATCH
    }

    @Override
    public Mono<String> registerMatchQueue(String queue, Long userId) {
        return getUserHasCode(userId)
                .flatMap(hasCode -> {
                    if (!hasCode) {
                        log.info("게임중이 아니라서 매칭 큐에 진입합니다.");
                        return getUserMatch(userId)
                                .flatMap(match -> addUserMatch(queue, userId, match))
                                .switchIfEmpty(Mono.just("fail"));
                    }
                    else return Mono.just("fail");

                });
    }

    public Mono<UserMatch> getUserMatch(Long membershipId){
        return getUserElo(membershipId)
                .flatMap(elo -> getUserName(membershipId)
                        .map(name -> {
                            log.info("data: " + elo + " " + name);
                            return UserMatch.builder()
                                    .membershipId(String.valueOf(membershipId))
                                    .elo(elo)
                                    .name(name)
                                    .build();
                        })
                )
                .switchIfEmpty(Mono.empty());
    }

    public Mono<Long> getUserElo(Long membershipId) {
        List<SubTask> subTasks = createSubTaskListMatchPlayerEloByMembershipId(membershipId);
        Task task = createTaskMatchPlayerEloByMembershipId(membershipId, subTasks);

        return taskProducerPort.sendTask("task.result.response",task)
                .then(taskConsumerPort.waitForUserResponseTaskResult(task.getTaskID())
                        .doOnSuccess(result -> log.info("Success user Elo: " + result))
                        .subscribeOn(Schedulers.boundedElastic()))
                .onErrorResume(error -> {
                    log.error("getUserElo error : "+error);
                    return Mono.empty();
                });
    }

    private Task createTaskMatchPlayerEloByMembershipId(Long membershipId, List<SubTask> subTasks){
        return createTask("Match Response", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListMatchPlayerEloByMembershipId(Long membershipId) {
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskMatchPlayerEloByMembershipId(membershipId));
        return subTasks;
    }

    private SubTask createSubTaskMatchPlayerEloByMembershipId(Long membershipId){
        return createSubTask("MatchPlayerEloByMembershipId",
                String.valueOf(membershipId),
                SubTask.TaskType.match,
                SubTask.TaskStatus.ready,
                membershipId);
    }

    public Mono<Boolean> getUserHasCode(Long membershipId) {
        List<SubTask> subTasks = createSubTaskListMatchUserHasCodeByMembershipId(membershipId);
        Task task = createTaskMatchUserHasCodeByMembershipId(membershipId, subTasks);

        return taskProducerPort.sendTask("task.result.response",task)
                .then(taskConsumerPort.waitForUserHasCodeTaskResult(task.getTaskID())
                        .doOnSuccess(result -> log.info("Success user hascode: {}", result))
                        .subscribeOn(Schedulers.boundedElastic()))
                .onErrorResume(error -> {
                    log.error("getUserHasCode error : "+error);
                    return Mono.just(false);
                });
    }

    private Task createTaskMatchUserHasCodeByMembershipId(Long membershipId, List<SubTask> subTasks){
        return createTask("Match Response", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListMatchUserHasCodeByMembershipId(Long membershipId){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskMatchUserHasCodeByMembershipId(membershipId));
        return subTasks;
    }

    private SubTask createSubTaskMatchUserHasCodeByMembershipId(Long membershipId){
        return createSubTask("MatchUserHasCodeByMembershipId",
                String.valueOf(membershipId),
                SubTask.TaskType.match,
                SubTask.TaskStatus.ready,
                membershipId);
    }

    public Mono<String> getUserName(Long membershipId) {
        List<SubTask> subTasks = createSubTaskListMatchUserName(membershipId);
        Task task = createTaskMatchUserName(membershipId, subTasks);

        return taskProducerPort.sendTask("task.membership.response",task)
                .then(taskConsumerPort.waitForUserNameTaskResult(task.getTaskID())
                        .subscribeOn(Schedulers.boundedElastic()))
                .onErrorResume(error -> {
                    log.error("getUserName error : "+error);
                    return Mono.empty();
                });
    }

    private Task createTaskMatchUserName(Long membershipId, List<SubTask> subTasks){
        return createTask("Match Response", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListMatchUserName(Long membershipId){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskMatchUserName(membershipId));
        return subTasks;
    }

    private SubTask createSubTaskMatchUserName(Long membershipId){
        return createSubTask("MatchUserName",
                String.valueOf(membershipId),
                SubTask.TaskType.match,
                SubTask.TaskStatus.ready,
                membershipId);
    }

    private Mono<String> addUserMatch(String queue, Long userId, UserMatch match) {
        Long elo = match.getElo();
        String name = match.getName();
        String member = userId + ":" + name;

        log.info("register member: "+member);
        return addUserNickName(userId, name) // 먼저 사용자의 nickname을 HSet 타입으로 캐싱
                .flatMap(result -> reactiveRedisTemplate.opsForZSet().add(MATCH_WAIT_KEY.formatted(queue), member, elo.doubleValue())
                        .flatMap(zset -> reactiveRedisTemplate.expire(MATCH_WAIT_KEY.formatted(queue), Duration.ofSeconds(expireTime))))
                .thenReturn("{\"userId\":\"" + userId + "\", \"name\":\"" + name + "\", \"elo\":\"" + elo + "\"}");

    }

    private Mono<Boolean> addUserNickName(Long userId, String nickname){
        String memberKey = "user:" + userId;
        String memberNameField = "nickname";

        return reactiveRedisTemplate.opsForHash()
                .put(memberKey, memberNameField, nickname)
                .flatMap(result -> reactiveRedisTemplate.expire(memberKey, Duration.ofSeconds(nickNameExpireTime)));
    }

    @Override
    public Mono<Void> cancelMatchQueue(Long userId) {
        return getUserMatch(userId)
                .flatMap(userResponse -> handleCancelMatchResponse(userResponse.getName(), userId))
                .onErrorResume(e -> Mono.error(new RuntimeException("cancelMatchQueue error : ", e)));
    }

    private Mono<Void> handleCancelMatchResponse(String name, Long userId) {
        String memberKey = userId + ":" + name;

        log.info("cancle match: "+memberKey);
        return Flux.concat(
                reactiveRedisTemplate.keys(MATCH_WAIT_KEY.formatted("*"))
                        .flatMap(key -> reactiveRedisTemplate.opsForZSet().remove(key, memberKey)),

                reactiveRedisTemplate.keys(MATCH_WAIT_KEY_FOR_SCAN.formatted("*"))
                        .flatMap(key -> reactiveRedisTemplate.opsForZSet().remove(key, memberKey))
        ).then();
    }



    @Override
    public Mono<Tuple2<MatchStatus, MatchResponse>> getMatchResponse(Long memberId) {
        String key = "matchInfo:" + memberId;

        return handleMatchResponseFromQueue(key, memberId)
                .switchIfEmpty(handleIfEmptyInQueue(memberId));
    }

    private List<SubTask> createSubTaskListMatchCodeUpdate(Long memberId, String spaceId){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskMatchCodeUpdate(memberId, spaceId));
        return subTasks;
    }

    private SubTask createSubTaskMatchCodeUpdate(Long memberId, String spaceId){
        return createSubTask(
                "MatchCodeUpdate",
                String.valueOf(memberId),
                SubTask.TaskType.membership,
                SubTask.TaskStatus.ready,
                spaceId
        );
    }

    private Mono<Tuple2<MatchStatus, MatchResponse>> handleMatchResponseFromQueue(String key, Long memberId){
        return reactiveRedisTemplate.opsForValue().get(key)
                .flatMap(matchResponseStr -> {
                    try {
                        MatchResponse matchResponse = mapper.readValue(matchResponseStr, MatchResponse.class);
                        List<SubTask> subTasks = createSubTaskListMatchCodeUpdate(memberId, matchResponse.getSpaceId());
                        Task task = createTask("Match Request", null, subTasks);

                        return taskProducerPort.sendTask("task.membership.response", task)
                                .then(reactiveRedisTemplate.unlink(key))
                                .then(Mono.just(Tuples.of(MatchStatus.MATCH_FOUND, matchResponse)));

                    } catch (JsonProcessingException e) {
                        log.error("getMatchResponse JsonProcessingException: ", e);
                        return Mono.error(e);
                    }
                });
    }

    private Mono<Tuple2<MatchStatus, MatchResponse>> handleIfEmptyInQueue(Long memberId){
        return getRank("match", memberId)
                .flatMap(rank -> {
                    if (rank > -1) {
                        return Mono.just(Tuples.of(MatchStatus.MATCHING, new MatchResponse()));
                    } else {
                        return Mono.just(Tuples.of(MatchStatus.NO_MATCH, new MatchResponse()));
                    }
                });
    }

    @Override
    public Mono<Long> getRank(String queue, Long userId) {
        return getUserNickname(userId)
                .flatMap(nickname -> {
                    if (nickname.equals("None")) {
                        return Mono.just(-1L);
                    }
                    return calculateRank(queue, nickname, userId);
                })
                .doOnError(error -> log.error("Error getRank: ", error));
    }

    private Mono<String> getUserNickname(Long userId) {
        String memberKey = "user:" + userId;
        String memberNameField = "nickname";

        return reactiveRedisTemplate.opsForHash().get(memberKey, memberNameField)
                .map(nickname -> (String) nickname)
                .defaultIfEmpty("None")
                .doOnNext(nickname -> log.info("getUserNickname: " + nickname))
                .doOnError(error -> log.error("Error getUserNickname: ", error))
                .flatMap(nickname -> {
                    if (!nickname.equals("None")) {
                        String userKey = userId + ":" + nickname;
                        return reactiveRedisTemplate.expire(userKey, Duration.ofSeconds(expireTime))
                                .thenReturn(nickname);
                    }
                    return Mono.just(nickname);
                });
    }

    private Mono<Long> calculateRank(String queue, String nickname, Long userId) {
        String member = userId + ":" + nickname;
        log.info("Member: " + member);

        return reactiveRedisTemplate.opsForZSet().rank(MATCH_WAIT_KEY.formatted(queue), member)
                .defaultIfEmpty(-1L)
                .map(rank -> {
                    log.info("Rank: " + rank);

                    if (rank >= 0) {
                        reactiveRedisTemplate.expire(MATCH_WAIT_KEY.formatted(queue), Duration.ofSeconds(expireTime)).subscribe();
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
        String queue = "integrationTestQueue";

        return reactiveRedisTemplate.opsForZSet()
                .size(MATCH_WAIT_KEY.formatted(queue))
                .doOnNext(size -> log.info("ZSet size: {}", queue, size));
    }

    //=======================================================
}
