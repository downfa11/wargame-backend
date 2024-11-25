#pragma once

#include "base.h"
#include "GameSession.h"
#include "PacketManager.h"
#include "Resource.h"
#include "Timer.h"

#include <asio.hpp>
#include <shared_mutex>
#include <map>

class GameManager
{
public:
	static std::list<Client*> client_list_all;
	static std::vector<RoomData> auth_data;
	static Client* clients_info[MAX_CLIENT];

	static std::shared_mutex clients_info_mutex;
	static std::shared_mutex client_list_mutex;
	static std::shared_mutex session_mutex;

	static int timeout_check_time;


	static void NewClient(SOCKET client_socket, LPPER_HANDLE_DATA handle, LPPER_IO_DATA ioinfo);
	static void ClientClose(int client_socket);

	static void ClientChanMove(int client_socket, void* data);
	static void ClientRoomMove(int client_socket, void* data);

	static void ClientTimeOutSet(int client_socket);
	static void TimeOutCheck();

	static void ClientAuth(int socket, void* data);
	static void RoomAuth(int socket, int size, void* data);

	static void reconnectClient(int socket, int index, int channel, int room);

	static bool findEmptyRoom(RoomData curRoom);
	static std::vector<Client*> GetClientListInRoom(int channelIndex, int roomIndex);

	static void tempConnection(int client_socket, int channel, int room);
	static void tempClientCreate(int client_socket);

	static GameSession* getGameSession(int channel, int room);
	static GameSession* createGameSession(int channel, int room);
	static void removeGameSession(int channelm, int room);

	static std::map<int, std::map<int, GameSession*>> sessions;
};