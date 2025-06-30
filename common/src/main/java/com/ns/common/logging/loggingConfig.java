package com.ns.common.logging;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class LoggingConfig {
    @Value("${kafka.clusters.bootstrapservers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.sasl.mechanism}")
    String saslMechanism;

    @Value("${spring.kafka.properties.sasl.jaas.config}")
    String saslJaasConfig;

    @Value("${spring.kafka.properties.security.protocol}")
    String securityProtocol;

    @Value("${spring.kafka.properties.session.timeout.ms}")
    String sessionTimeoutMs;

    @Value("${spring.kafka.client.id}")
    String clientId;

    @Bean
    public ReactiveKafkaProducerTemplate<String, LogMessage> loggingProducerTemplate() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        producerProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        producerProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);

        producerProps.put("security.protocol", securityProtocol);
        producerProps.put("sasl.mechanism", saslMechanism);
        producerProps.put("sasl.jaas.config", saslJaasConfig);
        producerProps.put("client.id", clientId+ "-producer");

        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(producerProps));
    }
}
