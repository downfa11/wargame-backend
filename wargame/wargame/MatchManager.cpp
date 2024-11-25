#define _CRT_SECURE_NO_WARNINGS

#include "MatchManager.h"
#include "PacketManager.h"
#include "StructureManager.h"
#include "GameManager.h"
#include "Utility.h"

#include <iostream>
#include <string>
#include <chrono>
#include <vector>
#include <mutex>
#include <memory>
#include <algorithm>
#include <shared_mutex>
#include <thread>



const std::string MatchManager::brokers = "localhost:9094";
const Topic MatchManager::resultTopic = "task.result.response";
const Topic MatchManager::matchTopic = "task.match.response";

std::mutex kafka_mutex;

Properties MatchManager::props({ {"bootstrap.servers", MatchManager::brokers} });


void MatchManager::KafkaSend(const Topic& topic, const std::string& message) {

	KafkaProducer producer(MatchManager::props);

	ProducerRecord record(topic, NullKey, Value(message.c_str(), message.size()));
	std::cout << "Message (size:"<< message.size()<<")  : " << message.c_str() << std::endl;
	auto deliveryCb = [](const RecordMetadata& metadata, const Error& error) {
		if (!error)
			std::cout << "Message delivered" << std::endl;
		else
			std::cerr << "Message failed to be delivered: "<<error.value()<<" " << error.message() << std::endl;
		};

	producer.send(record, deliveryCb);
	producer.close();
}

void MatchManager::SaveMatchResult(const MatchResult& result) {
	{
		std::unique_lock<std::mutex> lock(kafka_mutex);
		std::string json = UtilityManager::matchResultToJson(result);
		MatchManager::KafkaSend(MatchManager::resultTopic, json);
	}
}

void MatchManager::handleBattleStart(int channel, int room) {
	std::cout << "모든 사용자들이 Ready 상태입니다." << std::endl;

	RoomStateUpdate(channel, room, 0);

	std::vector<Client*> clientList;

	{
		std::shared_lock<std::shared_mutex> lock(session.room_mutex);
		auto& client_room = session.client_list_room;
		clientList.resize(client_room.size());
		std::copy(client_room.begin(), client_room.end(), clientList.begin());
	}

	BYTE* packetData = new BYTE[sizeof(int) * 3 * clientList.size()];
	int packetSize = sizeof(int) * 3 * clientList.size();

	for (int i = 0; i < clientList.size(); i++) {
		memcpy(packetData + sizeof(int) * (3 * i), &clientList[i]->socket, sizeof(int));
		memcpy(packetData + sizeof(int) * (3 * i + 1), &clientList[i]->clientindex, sizeof(int)); // user_index
		memcpy(packetData + sizeof(int) * (3 * i + 2), &clientList[i]->team, sizeof(int)); // user_team
		std::cout << clientList[i]->clientindex << "님(" << clientList[i]->socket << ")은 " << (clientList[i]->team == 0 ? "blue" : "red") << "팀입니다." << std::endl;
	}

	for (auto currentClient : clientList) {
		GameSession::ClientChampInit(currentClient->socket);
		PacketManger::Send(currentClient->socket, H_BATTLE_START, packetData, packetSize);
	}

	for (auto currentClient : clientList)
		GameSession::ClientStat(currentClient->socket);

	delete[] packetData;


	session.startTime = std::chrono::system_clock::now();

	// [kind] nexus: 0, turret: 1, gate: 2
	StructureManager::NewStructure(0, 0, 0, channel, room, 30, 0, 30); // nexus
	StructureManager::NewStructure(1, 1, 1, channel, room, 30, 0, -30); // turret
	StructureManager::NewStructure(2, 1, 0, channel, room, 60, 0, -60); // nexus
}

void MatchManager::handleDodgeResult(int channel, int room) {

	std::cout << " 1분이 경과할 동안 전체 유저들의 픽이 이뤄지지 않았습니다. 종료합니다." << std::endl;

	MatchResult result;
	result.state = "dodge";
	result.channel = 0;
	result.room = 0;
	result.gameDuration = 0;
	result.dateTime = "";

	for (auto& inst : GameManager::auth_data) {
		if (inst.channel == channel && inst.room == room) {
			result.spaceId = inst.spaceId;

			for (auto& user : inst.blueTeam) {
				Client* dummy = new Client();
				dummy->clientindex = std::stoi(user.user_index);
				dummy->user_name = user.user_name;
				dummy->champindex = -1;
				dummy->socket = -1;
				result.blueTeams.push_back(std::move(dummy));
			}
			for (auto& user : inst.redTeam) {
				Client* dummy = new Client();
				dummy->clientindex = std::stoi(user.user_index);
				dummy->user_name = user.user_name;
				dummy->champindex = -1;
				dummy->socket = -1;
				result.redTeams.push_back(std::move(dummy));
			}
		}
	}

	std::string json = UtilityManager::matchResultToJson(result);

	auto condition = [channel, room](RoomData& roomInfo) {
		return roomInfo.channel == channel && roomInfo.room == room; };
	GameManager::auth_data.erase(std::remove_if(GameManager::auth_data.begin(), GameManager::auth_data.end(), condition), GameManager::auth_data.end());

	session.startTime = std::chrono::time_point<std::chrono::system_clock>();

	MatchManager::KafkaSend(MatchManager::resultTopic, json);


	{
		std::unique_lock<std::shared_mutex> lock(session.structure_mutex);
		session.structure_list_room.clear();
	}

	{
		std::unique_lock<std::shared_mutex> lock(session.room_mutex);
		for (auto& client : session.client_list_room)
		{
			if (client == nullptr)
				continue;

			GameManager::ClientClose(client->socket);
		}

		session.client_list_room.clear();
	}

}

void MatchManager::waitForPickTime(int channel, int room) {

	std::chrono::time_point<std::chrono::system_clock> startTime = std::chrono::system_clock::now();
	std::chrono::time_point<std::chrono::system_clock> currentTime;
	std::chrono::duration<double> elapsedTime;

	while (elapsedTime.count() < PICK_TIME) {
		std::this_thread::sleep_for(std::chrono::milliseconds(1000));
		currentTime = std::chrono::system_clock::now();
		elapsedTime = currentTime - startTime;

		int remainingTime = PICK_TIME - elapsedTime.count();

		{
			std::shared_lock<std::shared_mutex> lock(session.room_mutex);
			for (auto currentClient : session.client_list_room)
				PacketManger::Send(currentClient->socket, H_PICK_TIME, &remainingTime, sizeof(int));
		}
	}
}

void MatchManager::sendTeamPackets(int channel, int room) {
	std::vector<Client*> clientList;

	{
		std::shared_lock<std::shared_mutex> lock(session.room_mutex);
		auto& client_room = session.client_list_room;
		clientList.resize(client_room.size());
		std::copy(client_room.begin(), client_room.end(), clientList.begin());
	}


	BYTE* packetData = new BYTE[sizeof(int) * 3 * clientList.size()];
	int packetSize = sizeof(int) * 3 * clientList.size();

	for (int i = 0; i < clientList.size(); i++) {
		memcpy(packetData + sizeof(int) * (3 * i), &clientList[i]->socket, sizeof(int));
		memcpy(packetData + sizeof(int) * (3 * i + 1), &clientList[i]->clientindex, sizeof(int)); // user_index
		memcpy(packetData + sizeof(int) * (3 * i + 2), &clientList[i]->team, sizeof(int)); // user_team
		std::cout << clientList[i]->clientindex << "님(" << clientList[i]->socket << ")은 " << (clientList[i]->team == 0 ? "blue" : "red") << "팀입니다." << std::endl;
	}

	for (auto currentClient : clientList)
		PacketManger::Send(currentClient->socket, H_TEAM, packetData, packetSize);

	delete[] packetData;



}

//reconnect
void MatchManager::sendTeamPackets(int client_socket) {

	int chan = -1, room = -1;
	{
		std::shared_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			std::cout << "reconnect teamPackets error" << std::endl;
			return;
		}
		chan = client_info->channel;
		room = client_info->room;
	}


	if (chan == -1 || room == -1) {
		std::cout << "lock error" << std::endl;
		return;
	}

	{
		std::shared_lock<std::shared_mutex> lock(session.room_mutex);
		auto& client_list_room = GameSession::client_list_room;

		std::vector<Client*> clientList(client_list_room.begin(), client_list_room.end());

		BYTE* packetData = new BYTE[sizeof(int) * 3 * clientList.size()];
		int packetSize = sizeof(int) * 3 * clientList.size();

		for (int i = 0; i < clientList.size(); i++) {
			memcpy(packetData + sizeof(int) * (3 * i), &clientList[i]->socket, sizeof(int));
			memcpy(packetData + sizeof(int) * (3 * i + 1), &clientList[i]->clientindex, sizeof(int)); // user_index
			memcpy(packetData + sizeof(int) * (3 * i + 2), &clientList[i]->team, sizeof(int)); // user_team
			std::cout << clientList[i]->clientindex << "님(" << clientList[i]->socket << ")은 " << (clientList[i]->team == 0 ? "blue" : "red") << "팀입니다." << std::endl;
		}

		PacketManger::Send(client_socket, H_TEAM, packetData, packetSize);

		delete[] packetData;
	}
}

void MatchManager::ChampPickTimeOut(int channel, int room) {

	std::chrono::time_point<std::chrono::system_clock> fTime = std::chrono::system_clock::now(), cTime;
	std::chrono::duration<double> delay;

	while (delay.count() < 3) {
		std::this_thread::sleep_for(std::chrono::milliseconds(1000));
		cTime = std::chrono::system_clock::now();
		delay = cTime - fTime;
	}

	/*
		cout << "timer start" << endl;
		Timer timer;
		timer.StartTimer(3000, [channel, room]() {
			cout << "d?" << endl;
	*/

	RoomStateUpdate(channel, room, -1);

	std::cout << "set game space." << std::endl;
	{
		std::shared_lock<std::shared_mutex> lock(session.room_mutex);
		auto client_list = session.client_list_room;
		for (auto inst : client_list)
		{
			for (auto inst2 : client_list)
			{
				if (inst->socket == inst2->socket)
					continue;

				ClientInfo info;
				info.socket = inst->socket;
				info.champindex = inst->champindex;
				PacketManger::Send(inst2->socket, H_NEWBI, &info, sizeof(ClientInfo));
			}
		}
	}

	sendTeamPackets(channel, room);
	waitForPickTime(channel, room);

	if (AllClientsReady(channel, room))
		handleBattleStart(channel, room);
	else
		handleDodgeResult(channel, room);

	//});
}


void MatchManager::RoomStateUpdate(int channel, int room, int curState) {
	for (auto& roomData : GameManager::auth_data) {
		if (roomData.channel == channel && roomData.room == room) {
			if (roomData.isGame == curState)
				roomData.isGame++;

			else std::cout << "isGame 오류" << std::endl;
		}
	}
}

bool MatchManager::AllClientsReady(int chan, int room) {
	{
		std::shared_lock<std::shared_mutex> lock(GameSession::room_mutex);

		if (GameSession::client_list_room.size() != MAX_CLIENT_PER_ROOM)
			return false;

		for (auto inst : GameSession::client_list_room) {
			if (!inst->ready)
				return false;
		}
	}

	return true;
}