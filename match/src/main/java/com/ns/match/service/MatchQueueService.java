package com.ns.match.service;

import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.match.dto.MatchResponse;
import com.ns.match.dto.MatchUserResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchQueueService {

    @Value("${scheduler.enabled}")
    private Boolean scheduling = false;

    @Value("${spring.var.matchexpiretime:3}")
    private int expireTime;

    @Value("${spring.var.nicknameexpiretime:300}")
    private int nickNameExpireTime;

    private final String MATCH_WAIT_KEY ="users:queue:%s:wait";
    private final String MATCH_WAIT_KEY_FOR_SCAN ="users:queue:*:wait";
    public static final Long MAX_ALLOW_USER_COUNT = 10L;


    public static final ObjectMapper mapper = new ObjectMapper();

    private final RedissonReactiveClient redissonReactiveClient;

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private final KafkaService kafkaService;
    private final TaskService taskService;


    public enum MatchStatus {
        MATCH_FOUND, MATCHING, NO_MATCH
    }


    public Mono<String> registerMatchQueue(final String queue, final Long userId) {
        String lockKey = "lock:" + queue;
        RLockReactive lock = redissonReactiveClient.getLock(lockKey);

        return acquireLock(lock)
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.just("fail");
                    }

                    return getUserHasCode(userId)
                            .flatMap(hasCode -> {
                                if (hasCode) {
                                    log.info("게임중이 아니라서 매칭 큐에 진입합니다.");
                                    return getUserResponse(userId)
                                            .flatMap(userResponse -> addMatchUserResponse(userResponse, queue, userId));
                                }
                                else return Mono.just("fail");

                            })
                            .doFinally(signal -> releaseLock(lock, locked).subscribe());
                });
    }

    public Mono<MatchUserResponse> getUserResponse(Long membershipId) {
        List<SubTask> subTasks = createSubTaskListMatchUserResponseByMembershipId(membershipId);
        Task task = createTaskMatchUserResponseByMembershipId(membershipId, subTasks);

        return taskService.sendTask("task.membership.response",task)
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

        return taskService.sendTask("task.membership.response",task)
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

    private Mono<String> addMatchUserResponse(MatchUserResponse user, String queue, Long userId) {
        if (!user.getSpaceCode().isEmpty()) {
            return Mono.just("fail");
        }

        Long elo = user.getElo();
        String name = user.getName();
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

    private Mono<Boolean> acquireLock(RLockReactive lock) {
        return lock.tryLock(10, 30, TimeUnit.SECONDS);
    }

    private Mono<Boolean> releaseLock(RLockReactive lock, boolean locked) {
        if (locked) {
            return Mono.fromRunnable(() -> lock.unlock())
                    .thenReturn(true);
        }

        return Mono.just(false);
    }


    public Mono<Void> cancelMatchQueue(Long userId) {
        String lockKey = "lock:cancelMatchQueue:" + userId;
        RLockReactive lock = redissonReactiveClient.getLock(lockKey);

        return acquireLock(lock)
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.error(new RuntimeException("Unable to acquire lock"));
                    }

                    return getUserResponse(userId)
                            .flatMap(userResponse -> handleCancelMatchResponse(userResponse, userId))
                            .doFinally(signal -> releaseLock(lock, locked).subscribe())
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
        RLockReactive lock = redissonReactiveClient.getLock(lockKey);

        return acquireLock(lock)
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.just(-1L); // 잠금 획득 실패 시 -1 반환
                    }

                    return getUserNickname(userId)
                            .flatMap(nickname -> {
                                if (nickname.equals("None")) {
                                    return Mono.just(-1L);
                                }
                                return calculateRank(queue, nickname, userId);
                            })
                            .doFinally(signal -> releaseLock(lock, locked).subscribe());
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

    public Mono<Tuple2<MatchStatus, MatchResponse>> getMatchResponse(Long memberId) {
        String key = "matchInfo:" + memberId;
        String lockKey = "lock:getMatchResponse:" + memberId;
        RLockReactive lock = redissonReactiveClient.getLock(lockKey);

        return acquireLock(lock)
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.error(new RuntimeException("Unable to acquire lock"));
                    }

                    return handleMatchResponseFromQueue(key, memberId)
                            .switchIfEmpty(handleIfEmptyInQueue(memberId))
                            .doFinally(signal -> releaseLock(lock, locked).subscribe());
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

                        return taskService.sendTask("task.membership.response", task)
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

    @Scheduled(initialDelay = 3000, fixedDelay = 3000)
    public void scheduleMatchUser() {
        if (!scheduling) {
            log.info("passed scheduling..");
            return;
        }

        String lockKey = "lock:scheduleMatchUser";
        RLockReactive lock = redissonReactiveClient.getLock(lockKey);

        acquireLock(lock)
                .flatMap(locked -> {
                    if (!locked) {
                        log.info("already running");
                        return Mono.empty();
                    }

                    return reactiveRedisTemplate.
                            scan(ScanOptions.scanOptions()
                                    .match(MATCH_WAIT_KEY_FOR_SCAN)
                                    .count(3) // 매칭 큐의 종류
                                    .build())
                            .map(this::extractQueueName)
                            .collectList()
                            .flatMap(this::processAllQueue)
                            .thenReturn(locked);
                })
                .flatMap(locked -> releaseLock(lock, locked))
                .subscribe();
    }

    private Mono<Void> processAllQueue(List<String> queues){
        return Flux.fromIterable(queues)
                .flatMap(this::processQueue)
                .then();
    }

    private Mono<Void> processQueue(String queue) {
        return reactiveRedisTemplate.executeInSession(session ->
                Flux.defer(() -> processQueueInRange(queue, 100)).then())
                .then();
    }

    private Mono<Void> processQueueInRange(String queue, int maxProcessCount) {
        AtomicBoolean stopProcessing = new AtomicBoolean(false);

        return Flux.range(0, maxProcessCount)
                .takeWhile(i -> !stopProcessing.get())
                .flatMap(i -> reactiveRedisTemplate.opsForZSet()
                        .popMin(MATCH_WAIT_KEY.formatted(queue), MAX_ALLOW_USER_COUNT)
                        .collectList()
                        .flatMap(members -> {
                            if (members.isEmpty()) {
                                log.info("빈집입니다. {}", queue);
                                stopProcessing.set(true);
                                return Mono.empty();
                            }

                            List<String> memberValues = members.stream()
                                    .map(TypedTuple::getValue)
                                    .collect(Collectors.toList());

                            if (memberValues.size() < MAX_ALLOW_USER_COUNT) {
                                log.info("{} 매칭 큐는 MAX_ALLOW_USER_COUNT를 충족시키지 못하는 찌꺼기 남았음: {}", queue, memberValues.size());
                                return handleMatchError(queue, memberValues)
                                        .doOnTerminate(() -> stopProcessing.set(true)); // 실제 Mono 성공시 호출
                            }

                            return handleMatchFound(queue, memberValues)
                                    .onErrorResume(e -> {
                                        log.error("Error handleMatchFound {}: {}", queue, e.getMessage());
                                        return handleMatchError(queue, memberValues);
                                    });
                        }), 1)
                .then();
    }

    private Mono<Void> handleMatchError(String queue, List<String> members){
        return Flux.fromIterable(members)
                .flatMap(member -> reactiveRedisTemplate.opsForZSet()
                .add(MATCH_WAIT_KEY.formatted(queue), member, 0).then())
                .then();
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

    private String extractQueueName(String key) {
        return key.split(":")[2];
    }

    private Mono<Void> handleMatchFound(String queue, List<String> members) {
        String spaceId = UUID.randomUUID().toString();
        MatchResponse matchResponse = MatchResponse.fromMembers(spaceId, members);
        members.forEach(memberId -> saveMatchInfo(memberId, matchResponse));

        List<SubTask> subTasks = createSubTaskListMatchResponse(matchResponse);
        Task task = createTaskMatchResponse(subTasks);

        return taskService.sendTask("task.match.response", task)
                .then(removeMembersFromQueue(queue, members))
                .doOnError(error -> log.error("Error sendTask: " + error.getMessage()))
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

    // =============================Integration Test API==================================== //
    public Mono<Void> requestIntegrationTest(Long memberId, String nickName, Long elo){
        String queue = "integrationTestQueue";
        String userKey = memberId+ ":" + nickName;

        return reactiveRedisTemplate.opsForZSet()
                        .add(MATCH_WAIT_KEY.formatted(queue), userKey, elo.doubleValue())
                .then();
    }

    public Mono<Long> getRequestCount(){
        String queue = "integrationTestQueue";

        return reactiveRedisTemplate.opsForZSet()
                .size(MATCH_WAIT_KEY.formatted(queue))
                .doOnNext(size -> log.info("ZSet size: {}", queue, size));
    }

    // ========================================================================================== //
}