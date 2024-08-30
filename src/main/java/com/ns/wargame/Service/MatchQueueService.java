package com.ns.wargame.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.wargame.Domain.dto.MatchResponse;
import com.ns.wargame.Repository.UserR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchQueueService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final String MATCH_WAIT_KEY ="users:queue:%s:wait";
    private final String MATCH_WAIT_KEY_FOR_SCAN ="users:queue:*:wait";

    private static final Long MAX_ALLOW_USER_COUNT = 2L;

    @Value("${scheduler.enabled}")
    private Boolean scheduling = false;

    @Value("${spring.var.matchExpireTime}")
    private int expireTime;

    private final UserService userService;
    private final UserR2dbcRepository userRepository;

    private final GameService gameService;
    private final ObjectMapper mapper = new ObjectMapper();

    public Mono<String> registerMatchQueue(final String queue, final Long userId) {
        String lockKey = "lock:" + queue;
        String lockValue = UUID.randomUUID().toString();

        return reactiveRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10))
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.just("fail");
                    }

                    return userService.findById(userId).flatMap(user -> {
                        if (!user.getCurGameSpaceCode().isEmpty()) {
                            return releaseLock(lockKey, lockValue)
                                    .thenReturn("fail");
                        }

                        Long elo = user.getElo();
                        String name = user.getName();
                        String member = userId + ":" + name;

                        return reactiveRedisTemplate.opsForZSet().add(MATCH_WAIT_KEY.formatted(queue), member, elo.doubleValue())
                                .flatMap(result -> reactiveRedisTemplate.expire(MATCH_WAIT_KEY.formatted(queue), Duration.ofSeconds(expireTime)))
                                .thenReturn("{\"userId\":\"" + userId + "\", \"name\":\"" + name + "\", \"elo\":\"" + elo + "\"}")
                                .doFinally(signal -> releaseLock(lockKey, lockValue).subscribe());
                    });
                });
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

                    return userService.findById(userId).flatMap(user -> {
                        String name = user.getName();
                        String member = userId + ":" + name;
                        return Flux.concat(
                                        reactiveRedisTemplate.keys(MATCH_WAIT_KEY.formatted("*"))
                                                .flatMap(key -> reactiveRedisTemplate.opsForZSet().remove(key, member)),
                                        reactiveRedisTemplate.keys(MATCH_WAIT_KEY_FOR_SCAN.formatted("*"))
                                                .flatMap(key -> reactiveRedisTemplate.opsForZSet().remove(key, member))
                                ).then()
                                .doFinally(signal -> releaseLock(lockKey, lockValue).subscribe());
                    });
                });
    }


    //몇번째 순위로 대기중인지 반환합니다.
    public Mono<Long> getRank(final String queue, final Long userId) {
        String lockKey = "lock:getRank:" + userId;
        String lockValue = UUID.randomUUID().toString();

        return reactiveRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10))
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.just(-1L); // 잠금 획득 실패 시 -1 반환
                    }

                    return userService.findById(userId).flatMap(user -> {
                        String name = user.getName();
                        String member = userId + ":" + name;
                        return reactiveRedisTemplate.opsForZSet().rank(MATCH_WAIT_KEY.formatted(queue), member)
                                .defaultIfEmpty(-1L)
                                .map(rank -> rank >= 0 ? rank + 1 : rank)
                                .doFinally(signal -> releaseLock(lockKey, lockValue).subscribe());
                    });
                });
    }


    public enum MatchStatus {
        MATCH_FOUND,
        MATCHING,
        NO_MATCH
    }

    public Mono<Tuple2<MatchStatus, MatchResponse>> getMatchResponse(Long memberId) {
        String key = "matchInfo:" + memberId;
        String lockKey = "lock:getMatchResponse:" + memberId;
        String lockValue = UUID.randomUUID().toString();

        return reactiveRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10))
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.error(new RuntimeException("Unable to acquire lock"));
                    }

                    return reactiveRedisTemplate.opsForValue().get(key)
                            .flatMap(matchResponseStr -> {
                                try {
                                    MatchResponse matchResponse = mapper.readValue(matchResponseStr, MatchResponse.class);
                                    return userService.findById(memberId)
                                            .flatMap(user -> {
                                                user.setCurGameSpaceCode(matchResponse.getSpaceId());
                                                return userRepository.save(user)
                                                        .thenReturn(Tuples.of(MatchStatus.MATCH_FOUND, matchResponse));
                                            })
                                            .doOnSuccess(result -> reactiveRedisTemplate.unlink(key).subscribe());
                                } catch (JsonProcessingException e) {
                                    log.error("getMatchResponse JsonProcessingException : ", e);
                                    return Mono.error(e);
                                }
                            })
                            .switchIfEmpty(
                                    getRank("match", memberId)
                                            .flatMap(rank -> {
                                                if (rank > -1)
                                                    return Mono.just(Tuples.of(MatchStatus.MATCHING, new MatchResponse()));
                                                else
                                                    return Mono.just(Tuples.of(MatchStatus.NO_MATCH, new MatchResponse()));
                                            })
                            )
                            .doFinally(signal -> releaseLock(lockKey, lockValue).subscribe());
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

        reactiveRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10))
                .flatMap(locked -> {
                    if (!locked) {
                        log.info("Another instance is already running");
                        return Mono.empty();
                    }

                    return reactiveRedisTemplate.scan(ScanOptions.scanOptions()
                                    .match(MATCH_WAIT_KEY_FOR_SCAN)
                                    .count(100).build())
                            .map(key -> key.split(":")[2])
                            .flatMap(queue -> reactiveRedisTemplate.opsForZSet()
                                    .range(MATCH_WAIT_KEY.formatted(queue), Range.closed(0L, MAX_ALLOW_USER_COUNT - 1))
                                    .collectList()
                                    .flatMap(members -> {
                                        if (members.size() == MAX_ALLOW_USER_COUNT) {
                                            String spaceId = UUID.randomUUID().toString();
                                            MatchResponse matchResponse = MatchResponse.fromMembers(spaceId, members);
                                            String MatchResponseJson = createMatchResponseJson(matchResponse);
                                            members.forEach(memberId -> {
                                                try {
                                                    String membershipId = memberId.split(":")[0];
                                                    String json = mapper.writeValueAsString(matchResponse);
                                                    reactiveRedisTemplate.opsForValue().set("matchInfo:" + membershipId, json).subscribe();
                                                } catch (JsonProcessingException e) {
                                                    log.error("scheduleMatchUser JsonProcessingException : ", e);
                                                }
                                            });

                                            return gameService.MatchSendMessage("match", "key", matchResponse.toString())
                                                    .then(RemoveMembersFromQueue(queue, members))
                                                    .doOnSuccess(result -> log.info("Kafka message sent and members removed from Redis successfully."))
                                                    .doOnError(error -> log.error("Error during Kafka send or Redis operation: " + error.getMessage()))
                                                    .subscribeOn(Schedulers.boundedElastic());
                                        } else {
                                            return Mono.empty();
                                        }
                                    }))
                            .then()
                            .doFinally(signal -> releaseLock(lockKey, lockValue).subscribe());
                }).subscribe();
    }

    private String createMatchResponseJson(MatchResponse response) {
        try {
            return mapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("createMatchResponseJson JsonProcessingException : ", e);
            return "";
        }
    }


    private Mono<Void> RemoveMembersFromQueue(String queue,List<String> members) {
        return Flux.fromIterable(members)
                .flatMap(member -> reactiveRedisTemplate.opsForZSet().remove(String.format(MATCH_WAIT_KEY, queue), member))
                .then();
    }

}