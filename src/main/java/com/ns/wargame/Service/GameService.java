package com.ns.wargame.Service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.wargame.Domain.dto.GameResultRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.SenderResult;


@Service
@Slf4j
@RequiredArgsConstructor
public class GameService implements ApplicationRunner {

    private final GameResultService gameResultService;
    private final ReactiveKafkaProducerTemplate<String, String> reactiveCommonProducerTemplate;
    private final ReactiveKafkaProducerTemplate<String, String> reactiveMatchProducerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, String> reactiveCommonConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, String> reactiveResultConsumerTemplate;


    public Mono<Void> CommonSendMessage(String topic,String key, String message){
        return reactiveCommonProducerTemplate.send(topic, key, message)
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(result -> {
                    RecordMetadata meta = result.recordMetadata();
                    log.info("kafka send success : topic {} / {}", meta.topic(), meta.offset());
                })
                .doOnError(error -> {
                    log.info("kafka send error : {}",error.toString());
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
                    log.info("received message : "+r.value());

                    ObjectMapper mapper = new ObjectMapper();
                    GameResultRequest result;
                    try {
                        result = mapper.readValue(r.value(), GameResultRequest.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    String state = result.getState();

                    if(state.equals("success")){
                        log.info("enroll result!");
                        gameResultService.enroll(result).subscribe();}

                    else if(state.equals("dodge")){
                        log.info("dodge start!");
                        gameResultService.dodge(result).subscribe();}

                })
                .doOnError(e -> {System.out.println("Error receiving: " + e);})
                .subscribe();
    }
}
