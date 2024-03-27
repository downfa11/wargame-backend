#pragma once
#include "base.h"
#include "PacketManager.h"
#include <vector>
#include <string>
#include <codecvt>
#include <locale>
#include <random>
#include <fstream>
#include <sstream>
#include <float.h>
#include <chrono>
#include <unordered_map>
#include <algorithm>

#define MAX_CLIENT 5000
#define MAX_CHANNEL_COUNT 2
#define MAX_ROOM_COUNT_PER_CHANNEL 100
#define MAX_CLIENT_PER_ROOM 1
#define MAX_TEAM_PER_ROOM 2 // MAX_CLIENT_PER_ROOM/2


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
	static vector<Client*> client_match;
	static vector< roomData> auth_data;
	static ClientChannel client_channel[MAX_CHANNEL_COUNT];
	static Client* clients_info[MAX_CLIENT];

	static bool exit_connect;
	static int timeout_check_time;

	static chrono::high_resolution_clock::time_point lastUpdateTime;
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
	static int UpdateMMR(int MMR_A, int MMR_B, bool A_win);
	static int get_opposing_team_MMR(list<Client*>& clients_in_room);
	static void ClientReady(int client_socket, int size, void* data);
	static void ClientMatch(int client_socket);
	static void Assign_teams();
	static bool AllClientsReady(int channel, int room);
	static void SendVictory(int client_socket, int winTeam, int channel, int room);
	static void LoadChampions();
	static void ClientStat(int client_socket);
	static void ClientChampInit(int client_socket);
	static void MouseSearch(int client_socket, void* data);
	static void AttackClient(int client_socket, void* data);
	static void AttackStruct(int index, void* data);
	static void NewStructure(int index, int chan, int room, int team, int x, int y, int z);
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
	static void ItemInit();
	static void Well(int client_socket, void* data);
	static void CharLevelUp(Client* client);
	static void ClientAuth(int socket, void* data);
	static bool isExistClientCode(string user_code);
	static void ChampPickTimeOut(int timemin, int channel, int room);
	static void ReConnection(Client* client,int socket, int chan, int room);
};
