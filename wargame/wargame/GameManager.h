#pragma once
#include "base.h"
#include "PacketManager.h"
#include "Room.h"
#include <chrono>


using ChatEntry = pair<string, pair<chrono::system_clock::time_point, string>>;
using ChatLog = vector<ChatEntry>;

#define MAX_CLIENT 5000
#define MAX_CHANNEL_COUNT 2
#define MAX_ROOM_COUNT_PER_CHANNEL 100

struct ClientChannel
{
	Room room_list[MAX_ROOM_COUNT_PER_CHANNEL];
	chrono::time_point<chrono::system_clock> startTime[MAX_ROOM_COUNT_PER_CHANNEL];
};


class GameManager
{

public:

	static list<Client*> client_list_all;
	static Client* clients_info[MAX_CLIENT];

	static ClientChannel client_channel[MAX_CHANNEL_COUNT];

	static bool exit_connect;
	static int timeout_check_time;


	static void TimeOutCheck();

	static void NewClient(SOCKET client_socket, LPPER_HANDLE_DATA handle, LPPER_IO_DATA ioinfo);
	static void ClientChanMove(int client_socket, void* data);
	static void ClientRoomMove(int client_socket, void* data);
	static void ClientTimeOutSet(int client_socket);

	static vector<Client*> GetClientListInRoom(int channelIndex, int roomIndex);
	static vector<ChatEntry> GetChatLog(int channelIndex, int roomIndex);

	static void ClientReady(int client_socket, int size, void* data);
	static void ReConnection(Client* client,int socket, int chan, int room);
};
