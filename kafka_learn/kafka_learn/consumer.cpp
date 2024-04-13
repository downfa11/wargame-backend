#include <kafka/KafkaConsumer.h>

#include <cstdlib>
#include <iostream>
#include <signal.h>
#include <string>


using namespace std;

std::atomic_bool running = { true };

void stopRunning(int sig) {
    if (sig != SIGINT) return;

    if (running) {
        running = false;
    }
    else {
        // Restore the signal handler, -- to avoid stuck with this handler
        signal(SIGINT, SIG_IGN); // NOLINT
    }
}

/*
{
    "id": "example_id",
    "spaceId": 123,
    "teams": {
        "team1": ["player1", "player2", "player3"],
        "team2": ["player4", "player5", "player6"]
    }
}  */

void json_receive(string request) {

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
int main()
{
    using namespace kafka;
    using namespace kafka::clients::consumer;

    // Use Ctrl-C to terminate the program
    signal(SIGINT, stopRunning);    // NOLINT

    // E.g. KAFKA_BROKER_LIST: "192.168.0.1:9092,192.168.0.2:9092,192.168.0.3:9092"
    const std::string brokers = "localhost:9092"; // NOLINT
    const Topic topic = "game.result.topic";         // NOLINT

    // Prepare the configuration
    const Properties props({ {"bootstrap.servers", {brokers}} });

    // Create a consumer instance
    KafkaConsumer consumer(props);

    // Subscribe to topics
    consumer.subscribe({ topic });

    while (running) {
        // Poll messages from Kafka brokers
        auto records = consumer.poll(std::chrono::milliseconds(100));

        for (const auto& record : records) {
            if (!record.error()) {
                std::cout << "Got a new message..." << std::endl;
                std::cout << "    Topic    : " << record.topic() << std::endl;
                std::cout << "    Partition: " << record.partition() << std::endl;
                std::cout << "    Offset   : " << record.offset() << std::endl;
                std::cout << "    Timestamp: " << record.timestamp().toString() << std::endl;
                std::cout << "    Headers  : " << toString(record.headers()) << std::endl;
                std::cout << "    Key   [" << record.key().toString() << "]" << std::endl;
                std::cout << "    Value [" << record.value().toString() << "]" << std::endl;

                json_receive(record.value().toString());
            }
            else {
                std::cerr << record.toString() << std::endl;
            }
        }
    }

    // No explicit close is needed, RAII will take care of it
    consumer.close();
}


// 이 녀석은.. 쓰레드 하나 할애해서 계속해서 돌려야한다. 