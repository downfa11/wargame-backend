package com.ns.resultquery.config;

import com.ns.common.events.ResultRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${kafka.clusters.bootstrapservers}")
    String bootstrapServers;

    @Value("${event.consumer.group}")
    String eventConsumerGroup;

    @Value("${event.consumer.topic}")
    String eventTopic;


    @Bean
    public ReactiveKafkaConsumerTemplate<String, ResultRequestEvent> ReactiveKafkaConsumerTemplate() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ResultRequestEvent.class.getName());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, eventConsumerGroup);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        ReceiverOptions<String, ResultRequestEvent> receiverOptions = ReceiverOptions.<String, ResultRequestEvent>create(consumerProps)
                .subscription(Collections.singleton(eventTopic));

        return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
    }
}
