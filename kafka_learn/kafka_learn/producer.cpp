#define _CRT_SECURE_NO_WARNINGS

#include <kafka/KafkaProducer.h>

#include <cstdlib>
#include <iostream>
#include <string>

int main()
{
    using namespace kafka;
    using namespace kafka::clients::producer;

    // E.g. KAFKA_BROKER_LIST: "192.168.0.1:9092,192.168.0.2:9092,192.168.0.3:9092"
    const std::string brokers = "localhost:9092"; // NOLINT
    const Topic topic = "game.result.topic";            // NOLINT

    // Prepare the configuration
    const Properties props({ {"bootstrap.servers", brokers} });

    // Create a producer
    KafkaProducer producer(props);

    // Prepare a message
        std::cout << "Type message value and hit enter to produce message..." << std::endl;
        std::string line;
        std::getline(std::cin, line);
        /*line = R"({
      "id": 1,
      "gameSpaceCode": "exampleGameSpace",
      "winTeamString": "Winning Team",
      "loseTeamString": "Losing Team",
      "winTeams": ["TeamA_User1", "TeamA_User2", "TeamA_User3", "TeamA_User4", "TeamA_User5"],
      "loseTeams": ["TeamB_User1", "TeamB_User2", "TeamB_User3", "TeamB_User4", "TeamB_User5"],
      "createdAt": "2024-03-19T12:30:45",
      "updatedAt": "2024-03-19T13:45:22"
    })";*/
    
    ProducerRecord record(topic, NullKey, Value(line.c_str(), line.size()));

    // Prepare delivery callback
    auto deliveryCb = [](const RecordMetadata& metadata, const Error& error) {
        if (!error) {
            std::cout << "Message delivered: " << metadata.toString() << std::endl;
        }
        else {
            std::cerr << "Message failed to be delivered: " << error.message() << std::endl;
        }


        };

    // Send a message
    producer.send(record, deliveryCb);

    // Close the producer explicitly(or not, since RAII will take care of it)
    producer.close();
}