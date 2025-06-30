package com.ns.result.config;

import com.ns.common.task.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.SenderOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${kafka.clusters.bootstrapservers}")
    String bootstrapServers;

    @Value("${task.request.consumer.group}")
    String requestConsumerGroup;

    @Value("${task.response.consumer.group}")
    String responseConsumerGroup;

    @Value("${task.request.topic}")
    String taskRequestTopic;

    @Value("${task.response.topic}")
    String taskResponseTopic;

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
    public ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate() {
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

        return new ReactiveKafkaProducerTemplate<>(
                SenderOptions.create(producerProps)
        );
    }

    @Bean
    public ReactiveKafkaConsumerTemplate<String, Task> taskRequestConsumerTemplate() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Task.class.getName());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, requestConsumerGroup);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        consumerProps.put("security.protocol", securityProtocol);
        consumerProps.put("sasl.mechanism", saslMechanism);
        consumerProps.put("sasl.jaas.config", saslJaasConfig);
        consumerProps.put("session.timeout.ms", sessionTimeoutMs);
        consumerProps.put("client.id", clientId+ "-request-consumer");

        ReceiverOptions<String, Task> receiverOptions = ReceiverOptions.<String, Task>create(consumerProps)
                .subscription(Collections.singleton(taskRequestTopic));

        return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
    }

    @Bean
    public ReactiveKafkaConsumerTemplate<String, Task> taskResponseConsumerTemplate() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Task.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, responseConsumerGroup);

        consumerProps.put("security.protocol", securityProtocol);
        consumerProps.put("sasl.mechanism", saslMechanism);
        consumerProps.put("sasl.jaas.config", saslJaasConfig);
        consumerProps.put("session.timeout.ms", sessionTimeoutMs);
        consumerProps.put("client.id", clientId+ "-response-consumer");

        ReceiverOptions<String, Task> receiverOptions = ReceiverOptions.<String, Task>create(consumerProps)
                .subscription(Collections.singleton(taskResponseTopic));

        return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
    }
}