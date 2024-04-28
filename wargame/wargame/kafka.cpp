#define _CRT_SECURE_NO_WARNINGS

#include "kafka.h"

const string kafkaMessage::brokers = "localhost:9092";
const Topic kafkaMessage::resultTopic = "result";
const Topic kafkaMessage::matchTopic = "match";

Properties kafkaMessage::props({ {"bootstrap.servers", kafkaMessage::brokers} });
KafkaProducer kafkaMessage::producer(kafkaMessage::props);

void kafkaMessage::KafkaSend(const Topic& topic, const string& message) {
	ProducerRecord record(topic, NullKey, Value(message.c_str(), message.size()));

	auto deliveryCb = [](const RecordMetadata& metadata, const Error& error) {
		if (!error)
			std::cout << "Message delivered: " << metadata.toString() << std::endl;
		else
			std::cerr << "Message failed to be delivered: " << error.message() << std::endl;
		};

	producer.send(record, deliveryCb);
}