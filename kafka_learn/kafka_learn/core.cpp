#include <cstdlib>
#include <iostream>
#include <vector>
#include <string>
#include <kafka/KafkaProducer.h>
#include <kafka/KafkaConsumer.h>

using namespace std;

using namespace kafka;
using namespace kafka::clients::producer;
using namespace kafka::clients::consumer;

#define MAILSLOT_RESULT_ADDRESS TEXT("\\\\.\\mailslot\\result")
#define MAILSLOT_MATCH_ADDRESS TEXT("\\\\.\\mailslot\\match")

const string brokers = "localhost:9092";
const Topic resultTopic = "result";
const Topic matchTopic = "match";

const Properties props({ {"bootstrap.servers", {brokers}} });


void KafkaSend(const Topic& topic, string message) {
	KafkaProducer producer(props);

	ProducerRecord record(topic, NullKey, Value(message.c_str(), message.size()));

	auto deliveryCb = [](const RecordMetadata& metadata, const Error& error) {
		if (!error)
			std::cout << "Message delivered: " << metadata.toString() << std::endl;
		else
			std::cerr << "Message failed to be delivered: " << error.message() << std::endl;
		};

	producer.send(record, deliveryCb);
	producer.close();
}

bool SendToMailslot(const std::string& message) {
	HANDLE hMailslot;
	DWORD bytesWritten;

	hMailslot = CreateFile(MAILSLOT_MATCH_ADDRESS, GENERIC_WRITE, FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
	if (hMailslot == INVALID_HANDLE_VALUE) {
		std::cerr << "Failed to open mailslot" << std::endl;
		return false;
	}

	BOOL result = WriteFile(hMailslot, message.c_str(), message.length(), &bytesWritten, NULL);
	CloseHandle(hMailslot);

	return result != 0;
}

void KafkaConsumerThread() {
	KafkaConsumer consumer(props);
	consumer.subscribe({ matchTopic });

	while (1) {
		auto records = consumer.poll(chrono::milliseconds(3000));
		for (const auto& record : records) {
			if (!record.error()) {
				cout << endl;
				cout << "    Topic    : " << record.topic() << endl;
				//cout << "    Partition: " << record.partition() << endl;
				cout << "    Offset   : " << record.offset() << endl;
				cout << "    Timestamp: " << record.timestamp().toString() << endl;
				//cout << "    Headers  : " << toString(record.headers()) << endl;
				cout << "    Key   :" << record.key().toString() << endl;
				cout << "    Value :" << record.value().toString() << endl;

				/*
				{
					"id": "example_id",
					"spaceId": 123,
					"teams": {
						"team1": ["player1", "player2", "player3"],
						"team2": ["player4", "player5", "player6"]
					}
				}  */
				cout << endl;
				string message = record.value().toString();

				if (!SendToMailslot(message))
					cerr << "Failed to send message through mailslot" << endl;
				else cout << "Send successfully." << endl;
			}
			else cerr << record.toString() << endl;

		}
	}
	consumer.close();
}

void MailslotServerThread() {
    HANDLE hMailslot;
    DWORD bytesRead;
	vector<char> buffer(8192);

    hMailslot = CreateMailslot(MAILSLOT_RESULT_ADDRESS, 0, MAILSLOT_WAIT_FOREVER, NULL);
    if (hMailslot == INVALID_HANDLE_VALUE) {
        cerr << "Failed to create mailslot : "<< GetLastError() << endl;
        return;
    }

    while (true) {
        BOOL result = ReadFile(hMailslot, buffer.data(), buffer.size(), &bytesRead, NULL);
        if (!result || bytesRead == 0) {
            cerr << "Failed to read from mailslot " <<result<<" or "<<bytesRead << endl;
            continue;
        }

		if (bytesRead == buffer.size()) {
			cout << "size double." << endl;
			buffer.resize(buffer.size() * 2);
		}

		string message(buffer.begin(), buffer.begin() + bytesRead);
		cout << "received data : " << message << endl;
		KafkaSend(resultTopic, message);
    }

    CloseHandle(hMailslot);
}

int main()
{
    thread mailslotServer(MailslotServerThread);
	thread kafka_consumer_thread(KafkaConsumerThread);
	cout << "ver 1.2" << endl;
	cout << endl;
	while (1) {
		string line;
		std::getline(std::cin, line);

		cout << "message : " << line << endl;
		//KafkaSend(resultTopic, line);
		SendToMailslot(line);
	}

    mailslotServer.join();
	kafka_consumer_thread.join();

	return 0;
}