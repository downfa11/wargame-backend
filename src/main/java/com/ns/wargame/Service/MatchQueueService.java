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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final String MATCH_PROCEED_KEY ="users:queue:%s:proceed";

    private static final Long MAX_ALLOW_USER_COUNT = 2L;

    @Value("${scheduler.enabled}")
    private Boolean scheduling = false;

    @Value("${spring.var.matchExpireTime}")
    private int exireTime;

    private final UserService userService;
    private final UserR2dbcRepository userRepository;

    private final GameService gameService;
    private final ObjectMapper mapper = new ObjectMapper();

    public Mono<String> registerMatchQueue(final String queue,final Long userId){
        return userService.findById(userId).flatMap(user -> {
            if(user.getCurGameSpaceCode()!="")
                return Mono.just("fail");

            Long elo = user.getElo();
            String name = user.getName();
            String member = userId+":"+name;
            return reactiveRedisTemplate.opsForZSet().add(MATCH_WAIT_KEY.formatted(queue), member, elo.doubleValue())
                    .flatMap(result -> reactiveRedisTemplate.expire(MATCH_WAIT_KEY.formatted(queue), Duration.ofSeconds(exireTime)))
                    .thenReturn("{\"userId\":\"" + userId + "\", \"name\":\"" + name + "\", \"elo\":\"" + elo + "\"}");
        });
    }

    public Mono<Void> cancelMatchQueue(Long userId) {
        return userService.findById(userId).flatMap(user -> {

            String name = user.getName();
            String member = userId+":"+name;
            return Flux.concat(
                    reactiveRedisTemplate.keys(MATCH_WAIT_KEY.formatted("*"))
                            .flatMap(key -> reactiveRedisTemplate.opsForZSet().remove(key, member)),
                    reactiveRedisTemplate.keys(MATCH_WAIT_KEY_FOR_SCAN.formatted("*"))
                            .flatMap(key -> reactiveRedisTemplate.opsForZSet().remove(key, member)),
                    reactiveRedisTemplate.keys(MATCH_PROCEED_KEY.formatted("*"))
                            .flatMap(key -> reactiveRedisTemplate.opsForZSet().remove(key, member))
            ).then();
        });

    }


    //count만큼 의 사용자를 추출해서 PROCEED 큐로 이동시킵니다.
    public Mono<Long> allowUser(final String queue, final Long count) {
        return reactiveRedisTemplate.opsForZSet().popMin(MATCH_WAIT_KEY.formatted(queue), count)
                .flatMap(member -> {
                    Long elo = extractEloFromMember(member);
                    return reactiveRedisTemplate.opsForZSet().add(MATCH_PROCEED_KEY.formatted(queue), member.getValue(), elo.doubleValue())
                            .flatMap(result -> reactiveRedisTemplate.expire(MATCH_PROCEED_KEY.formatted(queue), Duration.ofSeconds(exireTime)));
                })
                .count();
    }

    private Long extractEloFromMember(ZSetOperations.TypedTuple<String> member) {
        String[] parts = member.getValue().split(":");
        return Long.parseLong(parts[2]);
    }



    //특정 큐에서 사용자가 허용되었는지 확인합니다. 허용되었으면 rank 반환
    public Mono<Boolean> isAllowed(final String queue,final Long userId){
        return userService.findById(userId).flatMap(user -> {
            String name = user.getName();
            String member = userId+":"+name;
            return reactiveRedisTemplate.opsForZSet().rank(MATCH_PROCEED_KEY.formatted(queue),member)
                .defaultIfEmpty(-1L).map(rank -> rank >=0);});
        // empty이면 -1, rank가 -1보다 작으면 false
    }

    public Mono<Boolean> isAllowedByToken(final String queue,final Long userId,final String token){
        return this.generateToken(queue,userId)
                .doOnSuccess(gen -> log.info("isAllowedByToken {} eqeuals {} token",gen,token))
                .filter(gen -> gen.equalsIgnoreCase(token))
                .map(i -> true)
                .defaultIfEmpty(false);
    }

    //몇번째 순위로 대기중인지 반환합니다.
    public Mono<Long> getRank(final String queue, final Long userId) {
        return userService.findById(userId).flatMap(user -> {
            String name = user.getName();
            String member = userId+":"+name;
            return reactiveRedisTemplate.opsForZSet().rank(MATCH_WAIT_KEY.formatted(queue), member)
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0 ? rank + 1 : rank)
                .doOnSuccess(rank-> log.info("getRank : {}",rank));});
    }

    public enum MatchStatus {
        MATCH_FOUND,
        MATCHING,
        NO_MATCH
    }

    public Mono<Tuple2<MatchStatus, MatchResponse>> getMatchResponse(Long memberId) {
        String key = "matchInfo:" + memberId;
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
                        log.error("JsonProcessingException : ", e);
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
                );

    }


    public Mono<String> generateToken(final String queue,final Long userId){
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");

            var input = "user-queue-%s-%d".formatted(queue,userId);

            byte[] encodeHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for(byte b : encodeHash){
                hexString.append(String.format("%02x",b));
            }
            return Mono.just(hexString.toString());

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 10000)
    public void scheduleMatchUser() {
        if (!scheduling) {
            log.info("passed scheduling..");
            return;
        }

        reactiveRedisTemplate.scan(ScanOptions.scanOptions()
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

                            members.forEach(memberId -> {
                                //memberId =
                                try {
                                    String membershipId = memberId.split(":")[0];
                                    String json = mapper.writeValueAsString(matchResponse);
                                    log.info("matchInfo:"+membershipId+" : "+json);
                                    reactiveRedisTemplate.opsForValue().set("matchInfo:" + membershipId, json).subscribe();
                                } catch (JsonProcessingException e) {
                                    log.error("JsonProcessingException : ", e);
                                }
                            });


                            return gameService.MatchSendMessage("match", "key", matchResponse.toString())
                                    .then(RemoveMembersFromQueue(queue,members))
                                    .doOnSuccess(result -> log.info("Kafka message sent and members removed from Redis successfully."))
                                    .doOnError(error -> log.error("Error during Kafka send or Redis operation: " + error.getMessage()))
                                    .subscribeOn(Schedulers.boundedElastic());
                        } else {
            return Mono.empty();
        }
    }))
            .subscribe();
}


    private Mono<Void> RemoveMembersFromQueue(String queue,List<String> members) {
        return Flux.fromIterable(members)
                .flatMap(member -> reactiveRedisTemplate.opsForZSet().remove(String.format(MATCH_WAIT_KEY, queue), member))
                .then();
    }

}

