#pragma once

#include "base.h"
#include "Client.h"
#include "Structure.h"
#include "Resource.h"
#include "StructureManager.h"
#include "UnitManager.h"

#include <iostream>
#include <vector>
#include <list>
#include <shared_mutex>
#include <memory>

constexpr int MAX_MOUSE_SEARCH = 50;
constexpr int MAX_MOVE_DISTANCE = 10;


using ChatEntry = std::pair<std::string, std::pair<std::chrono::system_clock::time_point, std::string>>;

class GameSession {

public:
	GameSession(int channel, int room) : channelId(channel), roomId(room) {
		chat_log = std::vector<ChatEntry>();
		std::cout << "GameSession created: Channel " << channel << ", Room " << room << std::endl;

	};

	std::vector<ChatEntry> chat_log;
	std::list<Client*> client_list_room;
	std::list<Structure*> structure_list_room;
	std::list<Bullet*> bullet_list_room;
	std::list<Unit*> unit_list_room;

	std::shared_mutex chat_mutex;
	std::shared_mutex room_mutex;
	std::shared_mutex bullet_mutex;
	std::shared_mutex structure_mutex;
	std::shared_mutex unit_mutex;

	std::chrono::time_point<std::chrono::system_clock> startTime;
	std::unique_ptr<StructureManager> structureManager;
	std::unique_ptr<UnitManager> unitManager;

	void ClientChat(std::string& name, int size, void* data);
	bool IsPositionValid(const Client& currentPos, const ClientInfo& newPos);
	void ClientMove(int client_socket, ClientInfo info);
	void ClientMoveStart(int client_socket, ClientMovestart* info);
	void ClientMoveStop(int client_socket, ClientInfo info);

	std::vector<ChatEntry> GetChatLog();
	void ClientReady(int client_socket, int champindex);
	bool AllClientsReady(int chan, int room);
	void SendVictory(int winTeam, int channel, int room);
	void ClientStat(int client_socket);
	void ClientChampInit(Client* client, int champIndex);
	void ClientChampInit(Client* client);
	void MouseSearch(Client* client, MouseInfo info);
	void AttackClient(Client* client, AttInfo info);
	void AttackStructure(Client* client, AttInfo info);
	void UpdateClientDelay(Client* client);
	int CalculateDamage(Client* attacker);

	void NotifyAttackResulttoClient(int client_socket, int chan, int room, int attacked_socket);
	void NotifyAttackResulttoStructure(int client_socket, int chan, int room, int attacked_index);

	void ClientDie(int client_socket, int killer, int kind);
	void WaitAndRespawn(int client_socket, int respawnTime);

	void ClientRespawn(int client_socket);
	void ItemStat(Client* client, Item info);
	void CharLevelUp(Client* client);

	void champ1Passive(int client_socket, AttInfo info, int chan, int room);
	void champStatusEffect(int client_socket, std::string field, int value, int delayTime);
	void BulletStat(int client_socket, int bullet_index);
private:
	int channelId;
	int roomId;
};