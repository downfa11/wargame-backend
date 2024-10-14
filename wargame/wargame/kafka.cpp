#define _CRT_SECURE_NO_WARNINGS

#include "kafka.h"

const string kafkaMessage::brokers = "localhost:9094";
const Topic kafkaMessage::resultTopic = "task.result.response";
const Topic kafkaMessage::matchTopic = "task.match.response";

Properties kafkaMessage::props({ {"bootstrap.servers", kafkaMessage::brokers} });

void kafkaMessage::KafkaSend(const Topic& topic, const string& message) {

	KafkaProducer producer(kafkaMessage::props);

	ProducerRecord record(topic, NullKey, Value(message.c_str(), message.size()));
	cout << "Message (size:"<< message.size()<<")  : " << message.c_str() << endl;
	auto deliveryCb = [](const RecordMetadata& metadata, const Error& error) {
		if (!error)
			cout << "Message delivered" << endl;
		else
			cerr << "Message failed to be delivered: "<<error.value()<<" " << error.message() << endl;
		};

	producer.send(record, deliveryCb);
	producer.close();
}