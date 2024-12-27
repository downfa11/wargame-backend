package com.ns.match.service;

import static com.ns.common.TaskUseCase.createSubTask;
import static com.ns.common.TaskUseCase.createTask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.match.dto.MatchResponse;
import com.ns.match.dto.MatchUserResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.KafkaSender;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchQueueService {

    @Value("${scheduler.enabled}")
    private Boolean scheduling = false;

    @Value("${spring.var.matchExpireTime:3}")
    private int expireTime;

    public static final ObjectMapper mapper = new ObjectMapper();
    private static final Long MAX_ALLOW_USER_COUNT = 2L;


    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private final KafkaService kafkaService;

    private final String MATCH_WAIT_KEY ="users:queue:%s:wait";
    private final String MATCH_WAIT_KEY_FOR_SCAN ="users:queue:*:wait";


    public enum MatchStatus {
        MATCH_FOUND, MATCHING, NO_MATCH
    }


    public Mono<String> registerMatchQueue(final String queue, final Long userId) {
        String lockKey = "lock:" + queue;
        String lockValue = UUID.randomUUID().toString();

        return acquireLock(lockKey,lockValue)
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.just("fail");
                    }

                    return getUserHasCode(userId)
                            .flatMap(hasCode -> {
                                if (hasCode) {
                                    log.info("게임중이 아니라서 매칭 큐에 진입합니다.");
                                    return getUserResponse(userId)
                                            .flatMap(userResponse -> handleMatchResponse(userResponse, queue, userId));
                                }
                                else return Mono.just("fail");

                            })
                            .doFinally(signal -> releaseLock(lockKey, lockValue).subscribe());
                });
    }

    public Mono<MatchUserResponse> getUserResponse(Long membershipId) {
        List<SubTask> subTasks = createSubTaskListMatchUserResponseByMembershipId(membershipId);
        Task task = createTaskMatchUserResponseByMembershipId(membershipId, subTasks);

        return kafkaService.sendTask("task.membership.response",task)
                .then(kafkaService.waitForUserResponseTaskResult(task.getTaskID())
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Task createTaskMatchUserResponseByMembershipId(Long membershipId, List<SubTask> subTasks){
        return createTask("Match Response", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListMatchUserResponseByMembershipId(Long membershipId) {
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskMatchUserResponseByMembershipId(membershipId));
        return subTasks;
    }

    private SubTask createSubTaskMatchUserResponseByMembershipId(Long membershipId){
        return createSubTask("MatchUserResponseByMembershipId",
                String.valueOf(membershipId),
                SubTask.TaskType.match,
                SubTask.TaskStatus.ready,
                membershipId);
    }


    public Mono<Boolean> getUserHasCode(Long membershipId) {
        List<SubTask> subTasks = createSubTaskListMatchUserHasCodeByMembershipId(membershipId);
        Task task = createTaskMatchUserHasCodeByMembershipId(membershipId, subTasks);

        return kafkaService.sendTask("task.membership.response",task)
                .then(kafkaService.waitForUserHasCodeTaskResult(task.getTaskID())
                        .subscribeOn(Schedulers.boundedElastic()));
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

    private Mono<String> handleMatchResponse(MatchUserResponse user, String queue, Long userId) {
        if (!user.getSpaceCode().isEmpty()) {
            return Mono.just("fail");
        }

        Long elo = user.getElo();
        String name = user.getName();
        String member = userId + ":" + name;

        log.info("register member :"+member);
        return reactiveRedisTemplate.opsForZSet().add(MATCH_WAIT_KEY.formatted(queue), member, elo.doubleValue())
                .flatMap(result -> reactiveRedisTemplate.expire(MATCH_WAIT_KEY.formatted(queue), Duration.ofSeconds(expireTime)))
                .thenReturn("{\"userId\":\"" + userId + "\", \"name\":\"" + name + "\", \"elo\":\"" + elo + "\"}");
    }

    private Mono<Boolean> releaseLock(String lockKey, String lockValue) {
        return reactiveRedisTemplate.opsForValue().get(lockKey)
                .flatMap(currentValue -> {
                    if (lockValue.equals(currentValue)) {
                        return reactiveRedisTemplate.opsForValue().delete(lockKey).map(deleted -> deleted != null && deleted);
                    }
                    return Mono.just(false);
                });
    }


    public Mono<Void> cancelMatchQueue(Long userId) {
        String lockKey = "lock:cancelMatchQueue:" + userId;
        String lockValue = UUID.randomUUID().toString();

        return reactiveRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10))
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.error(new RuntimeException("Unable to acquire lock"));
                    }

                    return getUserResponse(userId)
                            .flatMap(userResponse -> handleCancelMatchResponse(userResponse, userId))
                            .doFinally(signal -> releaseLock(lockKey, lockValue).subscribe())
                            .onErrorResume(e -> Mono.error(new RuntimeException("waitForMatchTaskResult error : ", e)));
                });
    }

    private Mono<Void> handleCancelMatchResponse(MatchUserResponse user, Long userId) {
        String name = user.getName();
        String member = userId + ":" + name;

        log.info("cancle match: "+member);
        return Flux.concat(
                reactiveRedisTemplate.keys(MATCH_WAIT_KEY.formatted("*"))
                        .flatMap(key -> reactiveRedisTemplate.opsForZSet().remove(key, member)),

                reactiveRedisTemplate.keys(MATCH_WAIT_KEY_FOR_SCAN.formatted("*"))
                        .flatMap(key -> reactiveRedisTemplate.opsForZSet().remove(key, member))
        ).then();
    }



    // 반복해서 매칭중인지 호출하는 로직
    public Mono<Long> getRank(final String queue, final Long userId) {
        String lockKey = "lock:getRank:" + userId;
        String lockValue = UUID.randomUUID().toString();

        return acquireLock(lockKey, lockValue)
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.just(-1L); // 잠금 획득 실패 시 -1 반환
                    }

                    return getUserNickname(userId)
                            .flatMap(nickname -> {
                                if (nickname.equals("null")) {
                                    return Mono.just(-1L);
                                }
                                return calculateRank(queue, nickname, userId);
                            })
                            .doFinally(signal -> releaseLock(lockKey, lockValue).subscribe());
                })
                .doOnError(error -> log.error("Error during lock acquisition: ", error));
    }

    private Mono<String> getUserNickname(Long userId) {
        String userKey = "*:" + userId;
        return reactiveRedisTemplate.opsForValue().get(userKey)
                .defaultIfEmpty("null")
                .doOnNext(nickname -> log.info("Nickname found: " + nickname))
                .doOnError(error -> log.error("Error during nickname retrieval: ", error))
                .flatMap(nickname -> {
                    if (!nickname.equals("null")) {
                        return reactiveRedisTemplate.expire(userKey, Duration.ofSeconds(expireTime))
                                .thenReturn(nickname);
                    }
                    return Mono.just(nickname);
                });
    }

    private Mono<Long> calculateRank(String queue, String nickname, Long userId) {
        String member = nickname + ":" + userId;
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
                .doOnError(error -> log.error("Error during rank retrieval: ", error));
    }

    public Mono<Tuple2<MatchStatus, MatchResponse>> getMatchResponse(Long memberId) {
        String key = "matchInfo:" + memberId;
        String lockKey = "lock:getMatchResponse:" + memberId;
        String lockValue = UUID.randomUUID().toString();

        return acquireLock(lockKey, lockValue)
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.error(new RuntimeException("Unable to acquire lock"));
                    }

                    return handleMatchResponseFromQueue(key, memberId)
                            .switchIfEmpty(handleIfEmptyInQueue(memberId))
                            .doFinally(signal -> releaseLock(lockKey, lockValue)
                                    .subscribe());
                });
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

                        return kafkaService.sendTask("task.membership.response", task)
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

    @Scheduled(initialDelay = 5000, fixedDelay = 1000)
    public void scheduleMatchUser() {
        if (!scheduling) {
            log.info("passed scheduling..");
            return;
        }

        String lockKey = "lock:scheduleMatchUser";
        String lockValue = UUID.randomUUID().toString();

        acquireLock(lockKey, lockValue)
                .flatMap(locked -> {
                    if (!locked) {
                        log.info("Another instance is already running");
                        return Mono.empty();
                    }

                    return processMatching();
                })
                .doFinally(signal -> releaseLock(lockKey, lockValue).subscribe())
                .subscribe();
    }

    private Task createTaskMatchResponse(List<SubTask> subTasks){
        return createTask("Match response", null, subTasks);
    }
    private List<SubTask> createSubTaskListMatchResponse(MatchResponse matchResponse){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskMatchResponse(matchResponse));
        return subTasks;
    }

    private SubTask createSubTaskMatchResponse(MatchResponse matchResponse){
        return createSubTask("MatchResponse",
                null,
                SubTask.TaskType.match,
                SubTask.TaskStatus.ready,
                matchResponse);
    }

    private String createMatchResponseJson(MatchResponse response) {
        try {
            return mapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("createMatchResponseJson JsonProcessingException : ", e);
            return "";
        }
    }

    private Mono<Boolean> acquireLock(String lockKey, String lockValue) {
        return reactiveRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10));
    }

    private Mono<Void> processMatching() {
        return reactiveRedisTemplate.scan(ScanOptions.scanOptions()
                        .match(MATCH_WAIT_KEY_FOR_SCAN)
                        .count(100).build())
                .map(this::extractQueueName)
                .flatMap(this::processQueue)
                .then();
    }

    private String extractQueueName(String key) {
        return key.split(":")[2];
    }

    private Mono<Void> processQueue(String queue) {
        return reactiveRedisTemplate.opsForZSet()
                .range(MATCH_WAIT_KEY.formatted(queue), Range.closed(0L, MAX_ALLOW_USER_COUNT - 1))
                .collectList()
                .flatMap(members -> {
                    if (members.size() == MAX_ALLOW_USER_COUNT) {
                        return handleMatchFound(queue, members);
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> handleMatchFound(String queue, List<String> members) {
        String spaceId = UUID.randomUUID().toString();
        MatchResponse matchResponse = MatchResponse.fromMembers(spaceId, members);

        log.info("TEST: " + createMatchResponseJson(matchResponse));

        members.forEach(memberId -> saveMatchInfo(memberId, matchResponse));

        List<SubTask> subTasks = createSubTaskListMatchResponse(matchResponse);
        Task task = createTaskMatchResponse(subTasks);

        return kafkaService.sendTask("task.match.response", task)
                .then(removeMembersFromQueue(queue, members))
                .doOnSuccess(result -> log.info("Kafka message sent and members removed from Redis successfully."))
                .doOnError(error -> log.error("Error during Kafka send or Redis operation: " + error.getMessage()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void saveMatchInfo(String memberId, MatchResponse matchResponse) {
        try {
            String membershipId = memberId.split(":")[0];
            String json = mapper.writeValueAsString(matchResponse);
            reactiveRedisTemplate.opsForValue().set("matchInfo:" + membershipId, json).subscribe();
        } catch (JsonProcessingException e) {
            log.error("saveMatchInfo JsonProcessingException: ", e);
        }
    }

    private Mono<Void> removeMembersFromQueue(String queue, List<String> members) {
        return reactiveRedisTemplate.opsForZSet()
                .remove(MATCH_WAIT_KEY.formatted(queue), members.toArray())
                .then();
    }
}