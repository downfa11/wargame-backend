#pragma once

#include <cstdlib>
#include <iostream>
#include <string>
#include <kafka/KafkaProducer.h>
#include <kafka/KafkaConsumer.h>

using namespace std;
using namespace kafka;
using namespace kafka::clients::producer;
using namespace kafka::clients::consumer;

class kafkaIPC {


public:
	const static string brokers;
	const static Topic resultTopic;
	const static Topic matchTopic;

	static KafkaProducer producer;


	~kafkaIPC() {
		producer.close();
	}
	static void KafkaSend(const Topic& topic, const string& message);
	static void KafkaConsumerThread();
	static void json_receive(string request);
};