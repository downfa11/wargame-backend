#pragma once
#include "base.h"
#include "PacketManager.h"
#include "kafkaIPC.h"
#include "Resource.h"

#define MAX_CLIENT_PER_ROOM 1
#define MAX_TEAM_PER_ROOM 2 // MAX_CLIENT_PER_ROOM/2
#define MAX_CLIENT 10

using ChatEntry = pair<string, pair<chrono::system_clock::time_point, string>>;
using ChatLog = vector<ChatEntry>;

class Room {
public:
	static mutex mutex_client;
	static mutex mutex_structure;

	static ChatLog chat_log;

	static list<Client*> client_list_room;
	static list<structure*> structure_list_room;

	static Client* clients_room_info[MAX_CLIENT];

	static chrono::time_point<chrono::system_clock> startTime;
	static chrono::high_resolution_clock::time_point lastUpdateTime;

	static void ClientChampInit(int client_socket);
	static void ClientClose(int client_socket);

	static bool AllClientsReady(int channel, int room);
	static void ClientChat(int client_socket, int size, void* data);
	static void ClientMove(int client_socket, void* data);
	static void ClientMoveStart(int client_socket, void* data);
	static void ClientMoveStop(int client_socket, void* data);
	static void SendVictory(int client_socket, int winTeam, int channel, int room);
	static void ClientStat(int client_socket);

	static void MouseSearch(int client_socket, void* data);
	static void AttackClient(int client_socket, void* data);
	static void AttackStruct(int index, void* data);
	static void NewStructure(int index, int chan, int room, int team, float x, float y, float z);
	static void StructureDie(int index, int chan, int room);
	static void StructureStat(int index, int chan, int room);
	static void GotoLobby(int client_socket);
	static void TurretSearch(int index, int chan, int room);
	static void TurretShot(int index, bullet* newBullet, int attacked_, int chan, int room);
	static void ClientDie(int client_socket, int killer);
	static void WaitAndRespawn(int respawnTime, const chrono::time_point<chrono::system_clock>& diedtTime, int client_socket);
	static void TurretSearchWorker(int index, int chan, int room);
	static void StopTurretSearch(int index);
	static void SaveMatchResult(const MatchResult& result);
	static void ClientRespawn(int client_socket);


	static void ItemStat(int client_socket, void* data);

	static void Well(int client_socket, void* data);
	static void CharLevelUp(Client* client);
	static void ChampPickTimeOut(int timemin, int channel, int room);

};