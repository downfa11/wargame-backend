#pragma once

#include <kafka/KafkaProducer.h>
#include "base.h"
#include "GameSession.h"

#include <string>


#define PICK_TIME 10


using namespace kafka;
using namespace kafka::clients::producer;

#pragma pack(push,1)
struct MatchResult {
	std::string spaceId;
	std::string state; // dodge: 비정상적인 상황, success: 정상적인 상황
	int channel;
	int room;
	std::string winTeam;
	std::string loseTeam;

	std::vector<Client*> blueTeams;
	std::vector<Client*> redTeams;

	std::string dateTime;
	int gameDuration;
};
#pragma pack(pop)

class MatchManager {


public:
	const static std::string brokers;
	const static Topic resultTopic;
	const static Topic matchTopic;

	static Properties props;

	MatchManager() {
		MatchManager::props.put(MESSAGE_MAX_SIZE_FIELD, MESSAGE_MAX_SIZE_VALUE);
	}

	static void KafkaSend(const Topic& topic, const std::string& message);
	static void SaveMatchResult(const MatchResult& result);

	static void handleBattleStart(int channel, int room);
	static void handleDodgeResult(int channel, int room);

	static void waitForPickTime(int channel, int room);
	static void sendTeamPackets(int channel, int room);
	static void sendTeamPackets(int client_socket);
	static void ChampPickTimeOut(int channel, int room);

	static void RoomStateUpdate(int channel, int room, int curState);
	static bool AllClientsReady(int chan, int room);

private:
	static GameSession session;
	const char* MESSAGE_MAX_SIZE_FIELD = "message.max.bytes";
	const std::string MESSAGE_MAX_SIZE_VALUE = std::to_string(8192);

};