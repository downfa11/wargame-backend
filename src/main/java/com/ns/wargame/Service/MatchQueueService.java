package com.ns.wargame.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.wargame.Domain.dto.MatchResponse;
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
    private final String MATCH_WAIT_KEY ="users:queue:%s:match:wait";
    private final String MATCH_WAIT_KEY_FOR_SCAN ="users:queue:*:match:wait";
    private final String MATCH_PROCEED_KEY ="users:queue:%s:match:proceed";

    private static final Long MAX_ALLOW_USER_COUNT = 10L;

    @Value("${scheduler.enabled}")
    private Boolean scheduling = false;

    @Value("${spring.var.matchExpireTime}")
    private int exireTime;

    private final UserService userService;

    private final GameService gameService;
    private final ObjectMapper mapper = new ObjectMapper();

    public Mono<String> registerMatchQueue(final String queue,final Long userId){
        //membership의 게임코드가 null이 아니라면?(매칭 성공 시나리오)
        return userService.findById(userId).flatMap(user -> {
            Long elo = user.getElo();
            String name = user.getName();
            String member = userId+":"+name;
            return reactiveRedisTemplate.opsForZSet().add(MATCH_WAIT_KEY.formatted(queue), member, elo.doubleValue())
                    .flatMap(result -> reactiveRedisTemplate.expire(MATCH_WAIT_KEY.formatted(queue), Duration.ofSeconds(exireTime)))
                    .thenReturn("{\"userId\":\"" + userId + "\", \"name\":\"" + name + "\", \"elo\":\"" + elo + "\"}");
        }); //  {"userId":"1", "name":"John", "elo":"2000"}
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
        // member "userId:name:elo"
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

    public Mono<MatchResponse> getMatchResponse(Long memberId){
        String key = "matchInfo:" + memberId;
        return reactiveRedisTemplate.opsForValue().get(key)
                .flatMap(matchResponseStr -> {
                    if(matchResponseStr==null) {
                        log.info("No match response found for memberId: {}", memberId);
                        return Mono.empty();
                    }

                    try {
                        MatchResponse matchResponse = mapper.readValue(matchResponseStr, MatchResponse.class);
                        return reactiveRedisTemplate.unlink(key)
                                .then(Mono.just(matchResponse));
                    } catch (JsonProcessingException e) {
                        log.error("JsonProcessingException : ", e);
                        return Mono.error(e);
                    }
                });
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
                                try {
                                    String json = mapper.writeValueAsString(matchResponse);
                                    log.info(json);
                                    reactiveRedisTemplate.opsForValue().set("matchInfo:" + memberId, json).subscribe();
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

