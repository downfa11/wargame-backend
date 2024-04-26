#pragma once
#include "base.h"
#include "PacketManager.h"
#include "Resource.h"


#define MAX_CLIENT 5000
#define MAX_CHANNEL_COUNT 2
#define MAX_ROOM_COUNT_PER_CHANNEL 100
#define MAX_CLIENT_PER_ROOM 2
#define MAX_TEAM_PER_ROOM 5 // MAX_CLIENT_PER_ROOM/2


using ChatEntry = pair<string, pair<chrono::system_clock::time_point, string>>;
using ChatLog = vector<vector<ChatEntry>>;

struct ClientChannel
{
	list<Client*> client_list_room[MAX_ROOM_COUNT_PER_CHANNEL];
	list<structure*> structure_list_room[MAX_ROOM_COUNT_PER_CHANNEL];

	chrono::time_point<chrono::system_clock> startTime[MAX_ROOM_COUNT_PER_CHANNEL];
	ChatLog chat_log;
	ClientChannel() {
		chat_log = vector<vector<ChatEntry>>(MAX_ROOM_COUNT_PER_CHANNEL, vector<ChatEntry>());
	}
};


class GameManager
{
public:
	static list<Client*> client_list_all;
	static vector<roomData> auth_data;
	static ClientChannel client_channel[MAX_CHANNEL_COUNT];
	static Client* clients_info[MAX_CLIENT];

	static bool exit_connect;
	static int timeout_check_time;

	static void TimeOutCheck();
	static void NewClient(SOCKET client_socket, LPPER_HANDLE_DATA handle, LPPER_IO_DATA ioinfo);
	static void ClientClose(int client_socket);
	static void ClientChat(int client_socket, int size, void* data);
	static void ClientChanMove(int client_socket, void* data);
	static void ClientRoomMove(int client_socket, void* data);
	static void ClientMove(int client_socket, void* data);
	static void ClientMoveStart(int client_socket, void* data);
	static void ClientMoveStop(int client_socket, void* data);
	static void ClientTimeOutSet(int client_socket);
	static vector<Client*> GetClientListInRoom(int channelIndex, int roomIndex);
	static vector<ChatEntry> GetChatLog(int channelIndex, int roomIndex);
	static void ClientReady(int client_socket, int size, void* data);
	static bool AllClientsReady(int channel, int room);
	static void SendVictory(int client_socket, int winTeam, int channel, int room);
	static void ClientStat(int client_socket);
	static void ClientChampInit(int client_socket);
	static void ClientChampInit(Client* client);
	static void ClientChampReconnectInit(int client_socket);
	static void MouseSearch(int client_socket, void* data);
	static void AttackClient(int client_socket, void* data);
	static void AttackStructure(int index, void* data);
	static void UpdateClientDelay(int client_socket);
	static int CalculateDamage(Client* attacker);
	static void NotifyAttackResult(int client_socket, int chan, int room, int attackerSocket, int attackedIndex);
	static void NewStructure(int index, int chan, int room, int team, int x, int y, int z);
	static void StructureDie(int index, int chan, int room);
	static void StructureStat(int index, int chan, int room);
	static void TurretSearch(int index, int chan, int room);
	static void TurretShot(int index, bullet* newBullet, int attacked_, int chan, int room);
	static void ClientDie(int client_socket, int killer);
	static void WaitAndRespawn(int client_socket, int respawnTime, const chrono::time_point<chrono::system_clock>& diedtTime);
	static void TurretSearchWorker(int index, int chan, int room);
	static void StopTurretSearch(int index);
	static void SaveMatchResult(const MatchResult& result);
	static void ClientRespawn(int client_socket);
	static void ItemStat(int client_socket, void* data);
	static void Well(int client_socket, void* data);
	static void CharLevelUp(Client* client);

	static void ClientAuth(int socket, void* data);
	static void reconnectClient(int socket, int index, int channel, int room);
	static void ReConnection(int socket, int chan, int room);

	static void RoomAuth(int socket,int size, void* data);

	static void ChampPickTimeOut(int channel, int room);
	static void RoomStateUpdate(int channel, int room, int curState);
	static void waitForPickTime(int channel, int room);
	static void sendTeamPackets(int channel, int room);
	static void sendTeamPackets(int clinet_socket);
	static void handleBattleStart(int channel, int room);
	static void handleDodgeResult(int channel, int room);


	static bool findEmptyRoom(roomData curRoom);
	static string clientToJson(const Client* client);
	static string matchResultToJson(const MatchResult& result);
};