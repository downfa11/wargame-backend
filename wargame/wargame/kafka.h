#pragma once

#include <cstdlib>
#include <iostream>
#include <string>
#include <kafka/KafkaProducer.h>

using namespace std;
using namespace kafka;
using namespace kafka::clients::producer;

class kafkaMessage {


public:
	const static string brokers;
	const static Topic resultTopic;
	const static Topic matchTopic;

	static Properties props;
	static KafkaProducer producer;


	~kafkaMessage() {
		producer.close();
	}
	static void KafkaSend(const Topic& topic, const string& message);
};