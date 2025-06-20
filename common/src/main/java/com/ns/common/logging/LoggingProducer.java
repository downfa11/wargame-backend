package com.ns.common.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingProducer {
    @Value("${logging.topic}")
    private String topic;

    private final ReactiveKafkaProducerTemplate<String, LogMessage> loggingProducerTemplate;

    public Mono<Void> sendMessage(String key, LogMessage value) {
            return loggingProducerTemplate.send(topic, key, value)
                    .doOnSuccess(result -> log.info("logging success: key={}, value={}", key, value))
                    .doOnError(error -> log.error("logging failed: key={}, value={}, error={}", key, value, error.getMessage()))
                    .then();
    }
}
