#include "GameManager.h"

#include "Resource.h"
#include "GameSession.h"
#include "MatchManager.h"
#include "PacketManager.h"

#include<string>
#include<mutex>
#include<shared_mutex>

std::list<Client*> GameManager::client_list_all;
std::vector<RoomData> GameManager::auth_data;

Client* GameManager::clients_info[MAX_CLIENT];


std::shared_mutex GameManager::client_list_mutex;
std::shared_mutex GameManager::clients_info_mutex;
std::shared_mutex GameManager::session_mutex;

std::map<int, std::map<int, GameSession*>> GameManager::sessions;

//  level 마다 지정된 maxexp (전체 공통)
//  성장 능력치 상승치 grow* (챔프 개인)


#define TEST_CLIENT_SOCKET 1000



void GameManager::NewClient(SOCKET client_socket, LPPER_HANDLE_DATA handle, LPPER_IO_DATA ioinfo)
{
	Client* temp_cli = new Client;
	temp_cli->socket = client_socket;
	temp_cli->out_time = time(NULL) + 10;
	temp_cli->channel = 0;
	temp_cli->room = 0;

	temp_cli->handle = handle;
	temp_cli->ioinfo = ioinfo;

	int chan = -1, room = -1;
	{
		std::unique_lock<std::shared_mutex> lock(clients_info_mutex);
		clients_info[client_socket] = temp_cli;
		chan = clients_info[client_socket]->channel;
		room = clients_info[client_socket]->room;
	}

	if (chan == -1 || room == -1) {
		std::cout << "new Client lock error" << std::endl;
		return;
	}

	PacketManger::Send(client_socket, H_START, &temp_cli->socket, sizeof(int));

	{
		std::unique_lock<std::shared_mutex> lock(client_list_mutex);
		client_list_all.push_back(temp_cli);
	}

	GameSession* session = createGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	{
		std::unique_lock<std::shared_mutex> lock(session->room_mutex);
		session->client_list_room.push_back(temp_cli);
	}

	for (auto inst : session->client_list_room)
	{

		if (inst->socket == client_socket)
			continue;

		ClientInfo info;
		info.socket = client_socket;
		info.champindex = clients_info[client_socket]->champindex;
		info.x = clients_info[client_socket]->x;
		info.y = clients_info[client_socket]->y;
		info.z = clients_info[client_socket]->z;
		PacketManger::Send(inst->socket, H_NEWBI, &info, sizeof(ClientInfo));
	}



//	{
//		std::unique_lock<std::shared_mutex> lock(room_mutex);
//		client_channel[chan].client_list_room[room].push_back(temp_cli);
//	}



	std::cout << "Connect " << client_socket << std::endl;

	GameManager::tempConnection(client_socket, 0, 0);
}

void GameManager::ClientClose(int client_socket)
{
	{
		std::unique_lock<std::shared_mutex> lock(client_list_mutex);
		for (auto it = client_list_all.begin(); it != client_list_all.end(); ++it)
		{
			if ((*it)->socket == client_socket)
			{
				it = client_list_all.erase(it);
				break;
			}
		}
	}

	int chan = -1, room = -1;
	{
		std::unique_lock<std::shared_mutex> lock(clients_info_mutex);
		if (clients_info[client_socket] == nullptr) {
			std::cout << client_socket << " nullptr" << std::endl;
			return;
		}

		chan = clients_info[client_socket]->channel;
		room = clients_info[client_socket]->room;
		clients_info[client_socket] = nullptr;
	}

	if (chan == -1 || room == -1) {
		std::cout << "client close lock error" << std::endl;
		return;
	}

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
	}

	else {
		std::unique_lock<std::shared_mutex> lock(session->room_mutex);
		for (Client* inst : session->client_list_room) {

			if (inst->socket == client_socket) {
				inst->socket = -1;
				inst->ready = false;
			}

			PacketManger::Send(inst->socket, H_USER_DISCON, &client_socket, sizeof(int));
		}
	}

	closesocket(client_socket);
	std::cout << "Disconnected " << client_socket << std::endl;
}

// deadlock todo
void GameManager::ClientChanMove(int client_socket, void* data)
{
	int move_chan;
	memcpy(&move_chan, data, sizeof(int));

	int chan = -1, room = -1;
	{
		std::unique_lock<std::shared_mutex> lock_info(clients_info_mutex);
		auto& client_info = clients_info[client_socket];
		if (!client_info) {
			std::cout << "Client info error" << std::endl;
			return;
		}
		chan = client_info->channel;
		room = client_info->room;
		client_info->channel = move_chan;
	}

	if (chan == -1 || room == -1) {
		std::cout << "lock error" << std::endl;
		return;
	}

	GameSession* session = getGameSession(chan, room);
	GameSession* move_session = getGameSession(move_chan, room);
    if (!session || !move_chan) {
        std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
        return;
    }

	for (auto inst : session->client_list_room)
	{
		if (inst->socket == client_socket)
		{
			session->client_list_room.remove(inst);
			move_session->client_list_room.push_back(inst);
			break;
		}
	}



	for (auto inst : session->client_list_room)
		PacketManger::Send(inst->socket, H_USER_DISCON, &client_socket, sizeof(client_socket));


	ClientInfo newbi_info;
	newbi_info.socket = clients_info[client_socket]->socket;
	newbi_info.x = clients_info[client_socket]->x;
	newbi_info.y = clients_info[client_socket]->y;



	for (auto inst : move_session->client_list_room)
	{
		if (inst->socket != client_socket)
		{
			//새로운 룸안의 유저들에게 뉴비소켓 알려주기
			PacketManger::Send(inst->socket, H_NEWBI, &newbi_info, sizeof(ClientInfo));

			ClientInfo info;
			info.socket = inst->socket;
			info.x = inst->x;
			info.y = inst->y;
			PacketManger::Send(client_socket, H_NEWBI, &info, sizeof(ClientInfo));
		}
	}

}

// deadlock todo
void GameManager::ClientRoomMove(int client_socket, void* data)
{
	int move_room;
	memcpy(&move_room, data, sizeof(int));

	int chan = -1, room = -1;

	{
		std::unique_lock<std::shared_mutex> lock(clients_info_mutex);
		auto& client_info = clients_info[client_socket];
		if (!client_info) {
			std::cout << "Client info error" << std::endl;
			return;
		}

		chan = client_info->channel;
		room = client_info->room;
		client_info->room = move_room;
	}

	if (chan == -1 || room == -1) {
		std::cout << "lock error" << std::endl;
		return;
	}

	GameSession* session = getGameSession(chan, room);
	GameSession* move_session = getGameSession(chan, move_room);
	if (!session || !move_session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	for (auto inst : session->client_list_room)
	{
		if (inst->socket == client_socket)
		{
			session->client_list_room.remove(inst);
			move_session->client_list_room.push_back(inst);
			break;
		}
	}

	for (auto inst : session->client_list_room)
	{
		BYTE* packet_data[sizeof(int) * 3];
		memcpy(packet_data, &inst->socket, sizeof(int));
		memcpy(packet_data + sizeof(int), &chan, sizeof(int));
		memcpy(packet_data + sizeof(int) * 2, &move_room, sizeof(int));

		PacketManger::Send(inst->socket, H_ROOM_MOVE, packet_data, sizeof(packet_data));
	}
}

void GameManager::ClientTimeOutSet(int client_socket)
{
	{
		std::unique_lock<std::shared_mutex> lock(clients_info_mutex);
		auto& client_info = clients_info[client_socket];
		if (client_info == nullptr)
			return;


		PacketManger::Send(client_socket, H_TIMEOUT_SET);
		client_info->out_time = time(NULL) + 10;
	}
}

std::vector<Client*> GameManager::GetClientListInRoom(int channelIndex, int roomIndex)
{
	std::vector<Client*> clientList;

	if (channelIndex < 0 || channelIndex >= MAX_CHANNEL_COUNT)
		return clientList;

	if (roomIndex < 0 || roomIndex >= MAX_ROOM_COUNT_PER_CHANNEL)
		return clientList;

	GameSession* session = getGameSession(channelIndex, roomIndex);
	if (!session) {
		std::cout << "GameSession not found for channel " << channelIndex << ", room " << roomIndex << std::endl;
		return clientList;
	}
	
	{
		std::shared_lock<std::shared_mutex> lock(session->room_mutex);
		clientList = std::vector<Client*>(session->client_list_room.begin(), session->client_list_room.end());
	}
	//	clientList = GameManager::client_channel[channelIndex].client_list_room[roomIndex];

	return clientList;
}

void GameManager::RoomAuth(int socket, int size, void* data) {
	BYTE* packet_data = new BYTE[size];
	memcpy(packet_data, data, size);

	std::string code(reinterpret_cast<char*>(packet_data), size);

	std::cout << socket << "의 " << code << "번 방 접속 허용." << std::endl;


	RoomData curRoom;
	for (auto roomData : auth_data) {

		if (roomData.spaceId == code) {
			curRoom = roomData;

			int chan = roomData.channel;
			int room = roomData.room;

			if (chan == 0)
				ClientRoomMove(socket, (void*)&room);
			else {
				ClientChanMove(socket, (void*)&chan);
				ClientRoomMove(socket, (void*)&room);
			}


		}
	}


	{
		std::unique_lock<std::shared_mutex> lock(clients_info_mutex);
		clients_info[socket]->code = code;
	}

	PacketManger::Send(socket, H_RAUTHORIZATION, &code, size);


}

void GameManager::ClientAuth(int socket, void* data) {

	int index = -1; // 사용자의 고유 인식번호
	memcpy(&index, data, sizeof(int));

	std::string code = "";

	int chan = -1, room = -1;
	RoomData curRoom;

	{
		std::unique_lock<std::shared_mutex> lock(clients_info_mutex);
		clients_info[socket]->clientindex = index;
		code = clients_info[socket]->code;
	}

	if (code == "") {
		std::cout << socket << " 사용자는 room 인가를 받지 못했습니다." << std::endl;
		return;
	}


	for (auto roomData : auth_data) {
		if (roomData.spaceId == code) {

			if (roomData.spaceId == "") {
				std::cout << "clients_info[socket]->code가 잘못되었습니다." << std::endl;
				ClientClose(socket);
				return;
			}

			curRoom = roomData;

			chan = curRoom.channel;
			room = curRoom.room;

			if (chan == 0)
				ClientRoomMove(socket, (void*)&room);
			else {
				ClientChanMove(socket, (void*)&chan);
				ClientRoomMove(socket, (void*)&room);
			}
		}
	}


	for (auto inst : curRoom.blueTeam) {
		if (stoi(inst.user_index) == index) {
			clients_info[socket]->user_name = inst.user_name;
			clients_info[socket]->team = 0;
			break;
		}
	}

	if (clients_info[socket]->team == -1) {
		for (auto inst : curRoom.redTeam) {
			if (stoi(inst.user_index) == index) {
				clients_info[socket]->user_name = inst.user_name;
				clients_info[socket]->team = 1;
				break;
			}
		}
	}

	std::cout << socket << "'s team : " << clients_info[socket]->team << std::endl;



	if (curRoom.isGame == 0) {
		std::cout << "픽창 재접속을 시도합니다. " << "code : " << curRoom.isGame << std::endl;

		GameSession* session = getGameSession(chan, room);
		if (!session) {
			std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
			return;
		}

		{
			std::shared_lock<std::shared_mutex> lock(session->room_mutex);
			for (auto inst : session->client_list_room)
			{
				if (inst->socket == socket)
					return;

				ClientInfo info;
				info.socket = inst->socket;
				info.champindex = inst->champindex;
				PacketManger::Send(socket, H_NEWBI, &info, sizeof(ClientInfo));
			}

			for (auto inst : session->client_list_room)
			{
				if (inst->socket == socket)
					continue;

				ClientInfo info;
				info.socket = inst->socket;
				info.champindex = inst->champindex;
				PacketManger::Send(socket, H_NEWBI, &info, sizeof(ClientInfo));
			}
		}
		MatchManager::sendTeamPackets(socket);
	}

	else if (curRoom.isGame == 1)
		reconnectClient(socket, index, chan, room);
}
//todo
void GameManager::reconnectClient(int socket, int index, int channel, int room) {

	std::cout << "인게임 재접속을 시도합니다." << std::endl;

	GameSession* session = getGameSession(channel, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << channel << ", room " << room << std::endl;
		return;
	}

	auto it = session->client_list_room.begin();
	while (it != session->client_list_room.end()) {
		if ((*it)->clientindex == index && (*it)->socket == -1) {

			if ((*it)->maxhp == 0 && (*it)->maxmana == 0)
				session->ClientChampInit((*it));

			clients_info[socket] = (*it);
			clients_info[socket]->socket = socket;
			it = session->client_list_room.erase(it);
		}
		else if ((*it)->clientindex == index) {
			(*it) = clients_info[socket];

			for (auto& instance : client_list_all) {
				if (instance->clientindex == index) {
					instance = clients_info[socket];
					break;
				}
			}
			++it;
		}
		else ++it;

	}

	// ReConnection(socket, channel, room);
}

bool GameManager::findEmptyRoom(RoomData curRoom) {
	int empty_room = -1, empty_room_channel = -1;
	GameSession* session = nullptr;

	for (int i = 0; i < MAX_CHANNEL_COUNT; i++) {
		for (int j = 1; j < MAX_ROOM_COUNT_PER_CHANNEL; j++) {
			bool roomFound = false;
			for (auto& room : auth_data) {
				if (room.channel == i && room.room == j) {
					roomFound = true;
					break;
				}
			}

			session = getGameSession(i, j);
			if (!session) {
				std::cout << "GameSession not found for channel " << i << ", room " << j << std::endl;
				return false;
			}

			{
				std::shared_lock<std::shared_mutex> lock(session->room_mutex);
				if (!roomFound && session->client_list_room.size() == 0) {
					empty_room = j;
					empty_room_channel = i;
					break;
				}
			}
		}
		if (empty_room != -1) {
			break;
		}
	}

	if (empty_room == -1) {
		printf("There are no empty rooms\n");
		return false;
	}

	if (session == nullptr) {
		printf("session nullptr\n");
		return false;
	}

	std::cout << "Ch." << empty_room_channel << " Room #" << empty_room << " Created." << std::endl;

	for (auto& room : auth_data) {
		if (room.spaceId == curRoom.spaceId) {
			room.channel = empty_room_channel;
			room.room = empty_room;
			break;
		}
	}

	std::chrono::time_point<std::chrono::system_clock>& startTime = session->startTime;
	startTime = std::chrono::system_clock::now();

	MatchManager::ChampPickTimeOut(empty_room_channel, empty_room);

	return true;
}

GameSession* GameManager::getGameSession(int channel, int room) {
	std::unique_lock<std::shared_mutex> lock(GameManager::session_mutex);
//	if (!GameManager::sessions[channel][room])
//		GameManager::sessions[channel][room] = new GameSession(channel, room);
	
	return GameManager::sessions[channel][room];
}

GameSession* GameManager::createGameSession(int channel, int room) {
	std::unique_lock<std::shared_mutex> lock(GameManager::session_mutex);
	GameManager::sessions[channel][room] = new GameSession(channel, room);
	
	return GameManager::sessions[channel][room];
}

void GameManager::removeGameSession(int channel, int room) {
	std::unique_lock<std::shared_mutex> lock(GameManager::session_mutex);
	if (GameManager::sessions[channel][room]) {
		delete GameManager::sessions[channel][room];
		GameManager::sessions[channel][room] = nullptr;
	}
}

void GameManager::tempConnection(int client_socket, int channel, int room) {
	clients_info[client_socket]->champindex = 0;
	clients_info[client_socket]->team = 0;
	clients_info[client_socket]->user_name = "test";
	clients_info[client_socket]->clientindex = 0;


	tempClientCreate(TEST_CLIENT_SOCKET);

	MatchManager::handleBattleStart(channel, room);
}

void GameManager::tempClientCreate(int client_socket) {
	Client* temp_cli = new Client;
	temp_cli->socket = client_socket;
	temp_cli->out_time = time(NULL) + 10;
	temp_cli->channel = 0;
	temp_cli->room = 0;
	temp_cli->champindex = 1;
	temp_cli->clientindex = 1;
	temp_cli->user_name = "Test Player";
	temp_cli->team = 1;

	temp_cli->x = 18;
	temp_cli->y = 0;
	temp_cli->z = -55;

	int chan = -1, room = -1;
	{
		std::unique_lock<std::shared_mutex> lock(clients_info_mutex);
		clients_info[client_socket] = temp_cli;
		chan = clients_info[client_socket]->channel;
		room = clients_info[client_socket]->room;
	}

	if (chan == -1 || room == -1) {
		std::cout << "lock error" << std::endl;
		return;
	}

	PacketManger::Send(client_socket, H_START, &temp_cli->socket, sizeof(int));

	{
		std::unique_lock<std::shared_mutex> lock(client_list_mutex);
		client_list_all.push_back(temp_cli);
	}

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	{
		std::unique_lock<std::shared_mutex> lock(session->room_mutex);
		session->client_list_room.push_back(temp_cli);
	}

	for (auto inst : session->client_list_room)
	{

		if (inst->socket == client_socket)
			continue;

		ClientInfo info;
		info.socket = client_socket;
		info.champindex = clients_info[client_socket]->champindex;
		info.x = clients_info[client_socket]->x;
		info.y = clients_info[client_socket]->y;
		info.z = clients_info[client_socket]->z;
		PacketManger::Send(inst->socket, H_NEWBI, &info, sizeof(ClientInfo));
	}

	std::cout << "Connect " << client_socket << std::endl;
}

void GameManager::ClientChat(int client_socket, int size, void* data)
{
	std::vector<BYTE> packet_data(size + sizeof(int));
	memcpy(packet_data.data(), &client_socket, sizeof(int));
	memcpy(packet_data.data() + sizeof(int), data, size);

	std::string name = "";
	int chan = -1, room = -1;

	{
		std::shared_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		Client* sender = GameManager::clients_info[client_socket];
		if (sender == nullptr) {
			std::cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << std::endl;
			return;
		}
		chan = sender->channel;
		room = sender->room;
		name = sender->user_name;
	}

	if (chan == -1 || room == -1 || name == "") {
		std::cout << "lock error" << std::endl;
		return;
	}


	if (chan < 0 || chan >= MAX_CHANNEL_COUNT || room < 0 || room >= MAX_ROOM_COUNT_PER_CHANNEL) {
		std::cout << "Invalid channel or room index." << std::endl;
		return;
	}


	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->ClientChat(name, size, packet_data.data());
}

void GameManager::ClientMoveStart(int client_socket, void* data)
{
	ClientMovestart info;
	memcpy(&info, data, sizeof(ClientMovestart));

	int chan = -1, room = -1;

	{
		std::unique_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			std::cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << std::endl;
			return;
		}
		chan = client_info->channel;
		room = client_info->room;

		if (chan == -1 || room == -1) {
			std::cout << "lock error" << std::endl;
			return;
		}

		client_info->rotationX = info.rotationX;
		client_info->rotationY = info.rotationY;
		client_info->rotationZ = info.rotationZ;
	}

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->ClientMoveStart(client_socket, &info);
}

void GameManager::ClientMove(int client_socket, void* data)
{
	ClientInfo info;
	memcpy(&info, data, sizeof(ClientInfo));

	int chan = -1, room = -1;
	bool positionValid = false;
	{
		std::unique_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			std::cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << std::endl;
			return;
		}
		chan = client_info->channel;
		room = client_info->room;

		if (chan == -1 || room == -1) {
			std::cout << "lock error" << std::endl;
			return;
		}

		GameSession* session = getGameSession(chan, room);
		if (!session) {
			std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
			return;
		}

		session->ClientMove(client_socket, info);
	}
}

void GameManager::ClientMoveStop(int client_socket, void* data)
{
	//움직인다
	ClientInfo info;
	memcpy(&info, data, sizeof(ClientInfo));

	int chan = -1, room = -1;

	{
		std::unique_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			std::cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << std::endl;
			return;
		}
		chan = client_info->channel;
		room = client_info->room;

		if (chan == -1 || room == -1) {
			std::cout << "ClientMoveStop lock error" << std::endl;
			return;
		}
	}

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->ClientMoveStop(client_socket, info);
}

void GameManager::ClientReady(int client_socket, int size, void* data)
{
	int champindex;
	memcpy(&champindex, data, sizeof(int));

	int chan = -1, room = -1;

	{
		std::shared_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			std::cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << std::endl;
			return;
		}
		chan = client_info->channel;
		room = client_info->room;
	}

	if (chan == -1 || room == -1) {
		std::cout << "ClientReady lock error" << std::endl;
		return;
	}

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->ClientReady(client_socket, champindex);
	
}

void GameManager::ClientStat(int client_socket) {
	int chan = -1, room = -1;

	{
		std::shared_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr)
			return;

		chan = client_info->channel;
		room = client_info->room;
	}

	if (chan == -1 || room == -1) {
		std::cout << "ClientStat lock error" << std::endl;
		return;
	}


	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->ClientStat(client_socket);
}

void GameManager::ClientChampInit(int client_socket) {

	int chan = -1, room = -1, champIndex = -1;

	{
		std::shared_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			std::cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << std::endl;
			return;
		}

		chan = client_info->channel;
		room = client_info->room;
		champIndex = client_info->champindex;
	}

	if (chan == -1 || room == -1 || champIndex == -1) {
		std::cout << "ClientChampInit lock error" << std::endl;
		return;
	}

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->ClientChampInit(GameManager::clients_info[client_socket], champIndex);
}

void GameManager::MouseSearch(int client_socket, void* data)
{
	MouseInfo info;
	memcpy(&info, data, sizeof(MouseInfo));

	int chan = -1, room = -1;

	{
		std::shared_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		chan = GameManager::clients_info[client_socket]->channel;
		room = GameManager::clients_info[client_socket]->room;
	}

	if (chan == -1 || room == -1) {
		std::cout << "MouseSearch lock error" << std::endl;
		return;
	}

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->MouseSearch(GameManager::clients_info[client_socket], info);
}

void GameManager::AttackClient(int client_socket, void* data) {
	AttInfo info;
	memcpy(&info, data, sizeof(AttInfo));

	int chan = -1, room = -1;
	{
		std::shared_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		chan = GameManager::clients_info[client_socket]->channel;
		room = GameManager::clients_info[client_socket]->room;
	}

	if (chan == -1 || room == -1) {
		std::cout << "AttackClient lock error" << std::endl;
		return;
	}

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->AttackClient(GameManager::clients_info[client_socket], info);
}

void GameManager::AttackStructure(int client_socket, void* data)
{
	AttInfo info;
	memcpy(&info, data, sizeof(AttInfo));

	if (info.kind != 1) {
		std::cout << "왜 AttackStructure에 오신건가요? " << info.kind << ", object_kind: " << info.object_kind << std::endl;
		return;
	}

	int chan = -1, room = -1;
	{
		std::shared_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		chan = GameManager::clients_info[client_socket]->channel;
		room = GameManager::clients_info[client_socket]->room;
	}

	if (chan == -1 || room == -1) {
		std::cout << "AttackStructure lock error" << std::endl;
		return;
	}

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->AttackStructure(GameManager::clients_info[client_socket], info);


}

void GameManager::ItemStat(int client_socket, void* data)
{
	Item info;
	memcpy(&info, data, sizeof(Item));

	int chan = GameManager::clients_info[client_socket]->channel;
	int room = GameManager::clients_info[client_socket]->room;

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->ItemStat(GameManager::clients_info[client_socket], info);
}

void GameManager::Well(int client_socket, void* data) {
	int chan = -1, room = -1;

	{
		std::shared_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		chan = GameManager::clients_info[client_socket]->channel;
		room = GameManager::clients_info[client_socket]->room;
	}

	if (chan == -1 || room == -1) {
		std::cout << "lock error" << std::endl;
		return;
	}

	ClientInfo info;
	memcpy(&info, data, sizeof(ClientInfo));

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->structureManager->Well(GameManager::clients_info[client_socket],info.x,info.y,info.z);
}

void GameManager::champ1Passive(void* data) {
	AttInfo info;
	memcpy(&info, data, sizeof(AttInfo));
	int attacker_socket = info.attacker;

	int chan = -1, room = -1;
	{
		std::shared_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		chan = GameManager::clients_info[attacker_socket]->channel;
		room = GameManager::clients_info[attacker_socket]->room;
	}

	if (chan == -1 || room == -1) {
		std::cout << "lock error" << std::endl;
		return;
	}

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->champ1Passive(attacker_socket, info, chan, room);
}

void GameManager::BulletStat(int client_socket, void* data) {
	int bulletIndex = -1;
	memcpy(&bulletIndex, data, sizeof(int));

	int chan = GameManager::clients_info[client_socket]->channel;
	int room = GameManager::clients_info[client_socket]->room;

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->BulletStat(client_socket, bulletIndex);
}

void GameManager::UnitMoveStart(int client_socket, void* data)
{
	UnitMovestart info;
	memcpy(&info, data, sizeof(UnitMovestart));

	int chan = -1, room = -1;

	{
		std::unique_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			std::cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << std::endl;
			return;
		}
		chan = client_info->channel;
		room = client_info->room;

		if (chan == -1 || room == -1) {
			std::cout << "lock error" << std::endl;
			return;
		}
	}

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->unitManager->UnitMoveStart(client_socket, &info);
}

void GameManager::UnitMove(int client_socket, void* data)
{
	UnitInfo info;
	memcpy(&info, data, sizeof(UnitInfo));

	int chan = -1, room = -1;
	{
		std::unique_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			std::cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << std::endl;
			return;
		}
		chan = client_info->channel;
		room = client_info->room;

		if (chan == -1 || room == -1) {
			std::cout << "lock error" << std::endl;
			return;
		}

		GameSession* session = getGameSession(chan, room);
		if (!session) {
			std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
			return;
		}

		session->unitManager->UnitMove(client_socket, info);
	}
}

void GameManager::UnitMoveStop(int client_socket, void* data)
{
	//움직인다
	UnitInfo info;
	memcpy(&info, data, sizeof(UnitInfo));

	int chan = -1, room = -1;

	{
		std::unique_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			std::cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << std::endl;
			return;
		}
		chan = client_info->channel;
		room = client_info->room;

		if (chan == -1 || room == -1) {
			std::cout << "ClientMoveStop lock error" << std::endl;
			return;
		}
	}

	GameSession* session = getGameSession(chan, room);
	if (!session) {
		std::cout << "GameSession not found for channel " << chan << ", room " << room << std::endl;
		return;
	}

	session->unitManager->UnitMoveStop(client_socket, info);
}