#pragma once

#include "base.h"
#include "Client.h"
#include "Structure.h"
#include "StructureManager.h"
#include "Resource.h"
#include "UnitManager.h"
#include "PacketManager.h"

#include <iostream>
#include <vector>
#include <list>
#include <unordered_map>
#include <shared_mutex>
#include <memory>
#include <functional>

constexpr int MAX_MOUSE_SEARCH = 50;
constexpr int MAX_MOVE_DISTANCE = 10;


using ChatEntry = std::pair<std::string, std::pair<std::chrono::system_clock::time_point, std::string>>;

enum Target { CLIENT, STRUCTURE, UNIT };

const std::unordered_map<Target, int> TargetHeader = {
	{CLIENT, H_ATTACK_CLIENT},
	{STRUCTURE, H_ATTACK_STRUCT},
	{UNIT, H_ATTACK_UNIT}
};

struct PassiveStack {
	int passiveId = -1;
	int stackTime;
	int stackCount;
	int maxStackCount;
};

class StructureManager;
class UtilityManager;

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
	bool AllClientsReady();
	void SendVictory(int winTeam);

	void ClientStat(int client_socket);
	void ClientChampInit(Client* client, int champIndex);
	void ClientChampInit(Client* client);

	void MouseSearch(Client* client, MouseInfo info);
	
	template <typename T>
	void FindClosestTarget(const std::list<T*>& list, int teamClient, MouseInfo& info, double& minDistance, void*& closestTarget) {
		for (auto& inst : list) {
			if (teamClient != inst->team) {
				float distance = UtilityManager::DistancePosition(info.x, info.y, info.z, inst->x, inst->y, inst->z);
				if (distance < MAX_MOUSE_SEARCH && distance < minDistance) {
					closestTarget = static_cast<void*>(inst);
					minDistance = distance;
				}
			}
		}
	}

	void AttackClient(Client* client, AttInfo info);
	void AttackStructure(Client* client, AttInfo info);
	void AttackUnit(Client* client, AttInfo info);
	void UpdateClientDelay(Client* client);
	int CalculateDamage(Client* attacker);

	bool IsValidAttackRange(Client* attacker, const Client* target);
	bool IsValidAttackRange(Client* attacker, const Structure* target);
	bool IsValidAttackRange(Client* attacker, const Unit* target);

	void MouseSearchToTarget(Client* attacker, Client* attacked);
	void MouseSearchToTarget(Client* attacker, Structure* attacked);
	void MouseSearchToTarget(Client* attacker, Unit* attacked);

	void ApplyAbsorption(Client* attacker, int damage);
	void ApplyDamage(Client* target, int damage);
	bool IsReadyToAttack(Client* attacker);

	void NotifyAttackResult(int client_socket, int attacked, Target targetType);

	void ClientDie(int client_socket, int killer, Target kind);
	void WaitAndRespawn(int client_socket, int respawnTime);

	void ClientRespawn(int client_socket);
	void ItemStat(Client* client, Item info);
	void CharLevelUp(Client* client);

	void Champ1Passive(Client* attacker, AttInfo info);
	void ChampStatusEffect(int client_socket, std::string field, int value, int delayTime);
	void BulletStat(int client_socket, int bullet_index);

	void GetChampInfo(int client_socket);
	void GetItemInfo(int client_socket);

	void HandleClient1PassiveToClient(Client* attacker, int attacked_socket, PassiveStack passive);
	void HandleClient1PassiveToStructure(Client* attacker, int structure_id, PassiveStack passive);
	void HandleClient1PassivetoUnit(Client* attacker, int unit_id, PassiveStack passive);

	void NotifyPassiveStack(int client_socket, int target_id, int maxStackCount, int stackCount, Target targetType, int stackTime);

private:
	int channelId;
	int roomId;
};