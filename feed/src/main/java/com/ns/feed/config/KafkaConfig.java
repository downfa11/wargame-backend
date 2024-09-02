package com.ns.feed.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.Task;
import lombok.RequiredArgsConstructor;
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


@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${kafka.clusters.bootstrapservers}")
    String bootstrapServers;

    private final ObjectMapper objectMapper;

    @Bean
    public ReactiveKafkaProducerTemplate<String, Task> MembershipProducerTemplate() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        SenderOptions<String, Task> senderOptions = SenderOptions.<String, Task>create(producerProps)
                .withValueSerializer(new JsonSerializer<>(objectMapper));

        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }

    @Bean
    public ReactiveKafkaConsumerTemplate<String, Task> MembershipConsumerTemplate() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Task.class.getName());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");

        ReceiverOptions<String, Task> receiverOptions = ReceiverOptions.<String, Task>create(consumerProps)
                .subscription(Collections.singleton("membership"));

        return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
    }
}

