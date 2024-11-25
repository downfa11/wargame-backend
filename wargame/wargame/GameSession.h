#pragma once

#include "base.h"
#include "StructureManager.h"

#include <iostream>
#include <vector>
#include <list>
#include <shared_mutex>

constexpr int MAX_MOUSE_SEARCH = 50;
constexpr int MAX_MOVE_DISTANCE = 10;


using ChatEntry = std::pair<std::string, std::pair<std::chrono::system_clock::time_point, std::string>>;

#pragma pack(push,1) // 구조체 정렬을 1바이트로 맞추기 위한 지시문
struct MouseInfo
{
	float x;
	float y;
	float z;
	int kind = -1; /// 0: Player, 1:Structure, 2:Unit

};

struct AttInfo {
	int attacker;
	int attacked;
	int kind;
	int object_kind;
	int assist1 = -1;
	int assist2 = -1;
	int assist3 = -1;
	int assist4 = -1;

};

struct Bullet {
	double x;
	double y;
	double z;
	int dmg;
};

struct ItemSlots {
	int socket;
	int id_0;
	int id_1;
	int id_2;
	int id_3;
	int id_4;
	int id_5;
};

struct Unit {
	int index;
	int unit_kind = -1; // 병종 종류
	int team = -1; // red, blue
	int curhp = 0;
	int maxhp = 0;
	float x = 0;
	float y = 0;
	float z = 0;
	float maxdelay = 0;
	float curdelay = 0;
	int attrange = 0;
	int bulletdmg = 0;
	float bulletspeed = 0;

};

struct UnitInfo {
	int index;
	int kind = -1; // 병종 종류
	int team = -1; // red, blue
	int curhp = 0;
	int maxhp = 0;
	float x = 0;
	float y = 0;
	float z = 0;
	int attrange = 0;
	int bulletdmg = 0;
	float bulletspeed = 0;

};
#pragma pack(pop)


class GameSession {

public:
	GameSession(int channel, int room) : channelId(channel), roomId(room){
		chat_log = std::vector<ChatEntry>();
		std::cout << "GameSession created: Channel " << channel << ", Room " << room << std::endl;
	};

	static std::vector<ChatEntry> chat_log;

	static std::list<Client*> client_list_room;
	static std::list<Structure*> structure_list_room;
	static std::list<Unit*> unit_list_room;

	static std::shared_mutex chat_mutex;
	static std::shared_mutex room_mutex;
	static std::shared_mutex structure_mutex;
	static std::shared_mutex unit_mutex;

	static std::chrono::time_point <std::chrono::system_clock> startTime;

	static asio::io_context io_context;

	static void ClientChat(int client_socket, int size, void* data);
	static bool IsPositionValid(const Client& currentPos, const ClientInfo& newPos);
	static void ClientMove(int client_socket, void* data);
	static void ClientMoveStart(int client_socket, void* data);
	static void ClientMoveStop(int client_socket, void* data);

	static std::vector<ChatEntry> GetChatLog(int channelIndex, int roomIndex);
	static void ClientReady(int client_socket, int size, void* data);
	static bool AllClientsReady(int chan, int room);
	static void SendVictory(int winTeam, int channel, int room);
	static void ClientStat(int client_socket);
	static void ClientChampInit(int client_socket);
	static void ClientChampInit(Client* client);
	static void MouseSearch(int client_socket, void* data);
	static void AttackClient(int client_socket, void* data);
	static void AttackStructure(int index, void* data);
	static void UpdateClientDelay(Client* client);
	static int CalculateDamage(Client* attacker);

	static void NotifyAttackResulttoClient(int client_socket, int chan, int room, int attacked_socket);
	static void NotifyAttackResulttoStructure(int client_socket, int chan, int room, int attacked_index);

	static void ClientDie(int client_socket, int killer, int kind);
	static void WaitAndRespawn(int client_socket, int respawnTime);

	static void ClientRespawn(int client_socket);
	static void ItemStat(int client_socket, void* data);
	static void Well(int client_socket, void* data);
	static void CharLevelUp(Client* client);

	static void champ1Passive(void* data);

private:
	int channelId;
	int roomId;
};