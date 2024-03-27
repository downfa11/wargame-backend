package com.ns.wargame.Service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.wargame.Domain.GameResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.SenderResult;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameService implements ApplicationRunner {

    private final ReactiveRedisTemplate<String, Long> reactiveRedisTemplate;
    private final UserService userService;
    private final GameResultService gameResultService;
    private final ReactiveKafkaProducerTemplate<String, String> reactiveCommonProducerTemplate;
    private final ReactiveKafkaProducerTemplate<String, String> reactiveMatchProducerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, String> reactiveCommonConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, String> reactiveResultConsumerTemplate;

    public Mono<Void> updateRanking(Long user_id) {
        return userService.findById(user_id)
                .flatMap(user -> reactiveRedisTemplate.opsForZSet().remove("leaderboard", user_id)
                        .then(reactiveRedisTemplate.opsForZSet().add("leaderboard", user_id, user.getElo())))
                .then();
    }

    public Flux<Map<String, Object>> getLeaderboard() {
        ReactiveZSetOperations<String, Long> zSetOps = reactiveRedisTemplate.opsForZSet();
        return zSetOps.rangeWithScores("leaderboard", Range.closed(0L, 99L))
                .flatMapSequential(typedTuple -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("사용자 ID", typedTuple.getValue());
                    data.put("점수", typedTuple.getScore());
                    return Flux.just(data);
                });
    }


    public Mono<Void> CommonSendMessage(String topic,String key, String message){
        return reactiveCommonProducerTemplate.send(topic, key, message)
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(result -> {
                    RecordMetadata meta = result.recordMetadata();
                    log.info("kafka send success : topic {} / {}", meta.topic(), meta.offset());
                })
                .doOnError(error -> {
                    log.info("kafka send error");
                    log.info(error.toString());
                }).then();
    }

    public Mono<SenderResult<Void>> MatchSendMessage(String topic, String key, String message){
        return reactiveMatchProducerTemplate.send(topic, key, message);
    }

    @Override
    public void run(ApplicationArguments args){
        this.reactiveCommonConsumerTemplate
                .receiveAutoAck()
                .doOnNext(r -> {log.info("Common test success : {} {} {} {}",r.key(),r.value(),r.topic(),r.offset());})
                // Common test success : key "hi im namsoek" test 1
                .doOnError(e -> {System.out.println("Error receiving: " + e);})
                .subscribe();

        this.reactiveResultConsumerTemplate
                .receiveAutoAck()
                .doOnNext(r -> {

                    ObjectMapper mapper = new ObjectMapper();
                    GameResult result;
                    try {
                        result = mapper.readValue(r.value(), GameResult.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    gameResultService.enroll(result);

                    log.info("Result test success : {} {} {} {}",r.key(),r.value(),r.topic(),r.offset());})
                .doOnError(e -> {System.out.println("Error receiving: " + e);})
                .subscribe();
    }
}
