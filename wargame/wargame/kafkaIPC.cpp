#define _CRT_SECURE_NO_WARNINGS

#include "kafkaIPC.h"

const string kafkaIPC::brokers = "localhost:9092";
const Topic kafkaIPC::resultTopic = "game.result.topic";
const Topic kafkaIPC::matchTopic = "game.match.topic";

const Properties props({ {"bootstrap.servers", kafkaIPC::brokers} });
KafkaProducer kafkaIPC::producer(props);

void kafkaIPC::KafkaSend(const Topic& topic, const string& message) {
	ProducerRecord record(topic, NullKey, Value(message.c_str(), message.size()));

	auto deliveryCb = [](const RecordMetadata& metadata, const Error& error) {
		if (!error)
			std::cout << "Message delivered: " << metadata.toString() << std::endl;
		else 
			std::cerr << "Message failed to be delivered: " << error.message() << std::endl;
		};

	producer.send(record, deliveryCb);
}

void kafkaIPC::KafkaConsumerThread() {

	// Prepare the configuration
	const Properties props({ {"bootstrap.servers", {kafkaIPC::brokers}} });
	KafkaConsumer consumer(props);

	consumer.subscribe({ kafkaIPC::matchTopic });

	while (true) {
		auto records = consumer.poll(chrono::milliseconds(100));

		for (const auto& record : records) {
			if (!record.error()) {
				cout << "Got a new message..." << endl;
				cout << "    Topic    : " << record.topic() << endl;
				cout << "    Partition: " << record.partition() << endl;
				cout << "    Offset   : " << record.offset() << endl;
				cout << "    Timestamp: " << record.timestamp().toString() << endl;
				cout << "    Headers  : " << toString(record.headers()) << endl;
				cout << "    Key   [" << record.key().toString() << "]" << endl;
				cout << "    Value [" << record.value().toString() << "]" << endl;

				/*
				{
					"id": "example_id",
					"spaceId": 123,
					"teams": {
						"team1": ["player1", "player2", "player3"],
						"team2": ["player4", "player5", "player6"]
					}
				}  */

				kafkaIPC::json_receive(record.value().toString());
			}
			else cerr << record.toString() << endl;

		}
	}

	// No explicit close is needed, RAII will take care of it
	consumer.close();
}


void kafkaIPC::json_receive(string request) {

	size_t jsonStart = request.find("{");
	size_t jsonEnd = request.find_last_of("}");

	if (jsonStart != string::npos && jsonEnd != string::npos) {
		string jsonData = request.substr(jsonStart, jsonEnd - jsonStart + 1);

		try {
			size_t idStart = jsonData.find("\"id\":") + 6;
			size_t idEnd = jsonData.find("\"", idStart);
			string id = jsonData.substr(idStart, idEnd - idStart);

			size_t spaceIdStart = jsonData.find("\"spaceId\":") + 10;
			size_t spaceIdEnd = jsonData.find(",", spaceIdStart);
			int spaceId = stoi(jsonData.substr(spaceIdStart, spaceIdEnd - spaceIdStart));

			// roomData roominfo; roominfo.channel, room
			size_t teamsStart = jsonData.find("\"teams\":") + 8;
			size_t teamsEnd = jsonData.find_last_of("}");
			// team1, team2 애들의 UserData userData
			string teamsData = jsonData.substr(teamsStart, teamsEnd - teamsStart + 1);

			// roominfo.user_data.push_back(userData);
			// GameManager::auth_data.push_back(roominfo);
		}
		catch (const exception& e) {
			cerr << "Failed to parse JSON data: " << e.what() << endl;
		}
	}
	else cerr << "JSON data not found in POST request." << endl;

}
