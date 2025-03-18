package com.ns.common.logging;

import com.ns.common.task.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingProducer {
    @Value("${logging.topic}")
    private String topic;

    private final ReactiveKafkaProducerTemplate<String, String> loggingProducerTemplate;

    public Mono<Void> sendMessage(String key, String value) {
        return loggingProducerTemplate.send(topic, key, value)
                .doOnSuccess(result -> log.info("logging success: key={}, value={}", key, value))
                .doOnError(error -> log.error("logging failed: key={}, value={}, error={}", key, value, error.getMessage()))
                .then();
    }
}
