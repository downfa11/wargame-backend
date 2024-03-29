#include "GameManager.h"

syncList<Client*> GameManager::client_list_all; //모든 접속자

ClientChannel GameManager::client_channel[MAX_CHANNEL_COUNT]; //100개의 채널
Client* GameManager::clients_info[MAX_CLIENT];

bool GameManager::exit_connect = false;
int GameManager::timeout_check_time = 0;


#define PICK_TIME 60



void GameManager::TimeOutCheck()
{
	if (time(NULL) > timeout_check_time)
	{
		list<Client*> des_cli;
		for (auto inst : client_list_all)
		{
			if (inst->socket != -1 && time(NULL) > inst->out_time)
				des_cli.push_back(inst);
		}
		for (auto inst : des_cli)
		{
			if (!inst->isGame) {
				printf("Time out %d \n", inst->socket);
				closesocket(inst->socket);
			}
		}
		timeout_check_time = time(NULL) + 10;
	}
}

void GameManager::NewClient(SOCKET client_socket, LPPER_HANDLE_DATA handle, LPPER_IO_DATA ioinfo)
{
	Client* temp_cli = new Client;
	temp_cli->socket = client_socket;
	temp_cli->out_time = time(NULL) + 10;
	temp_cli->channel = 0;
	temp_cli->room = 0;
	temp_cli->handle = handle;
	temp_cli->ioinfo = ioinfo;

	clients_info[client_socket] = temp_cli;

	PacketManger::Send(client_socket, H_START, &temp_cli->socket, sizeof(int));

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	client_channel[chan].room_list[room].client_list_room.push_back(temp_cli);
	cout << "Connect " << client_socket << endl;
}

void GameManager::ClientChanMove(int client_socket, void* data)
{
	int move_chan;
	memcpy(&move_chan, data, sizeof(int));

	int chan = clients_info[client_socket]->channel;;
	int room = clients_info[client_socket]->room;

	clients_info[client_socket]->channel = move_chan;

	for (auto inst : client_channel[chan].room_list[room].client_list_room)
	{
		if (inst->socket == client_socket)
		{
			client_channel[chan].room_list[room].client_list_room.remove(inst);
			client_channel[chan].room_list[room].client_list_room.push_back(inst);
			break;
		}
	}

	for (auto inst : client_channel[chan].room_list[room].client_list_room)
		PacketManger::Send(inst->socket, H_USER_DISCON, &client_socket, sizeof(client_socket));

	ClientInfo newbi_info;
	newbi_info.socket = clients_info[client_socket]->socket;
	newbi_info.x = clients_info[client_socket]->x;
	newbi_info.y = clients_info[client_socket]->y;

	for (auto inst : client_channel[chan].room_list[room].client_list_room)
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

void GameManager::ClientRoomMove(int client_socket, void* data)
{
	int move_room;
	memcpy(&move_room, data, sizeof(int));

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	clients_info[client_socket]->room = move_room;

	for (auto inst : client_channel[chan].room_list[room].client_list_room)
	{
		if (inst->socket == client_socket)
		{
			client_channel[chan].room_list[room].client_list_room.remove(inst);
			client_channel[chan].room_list[room].client_list_room.push_back(inst);
			break;
		}
	}

	for (auto inst : client_channel[chan].room_list[room].client_list_room)
	{
		BYTE* packet_data = new BYTE[sizeof(int) * 3];
		memcpy(packet_data, &inst->socket, sizeof(int));
		memcpy(packet_data + sizeof(int), &chan, sizeof(int));
		memcpy(packet_data + sizeof(int) * 2, &move_room, sizeof(int));

		PacketManger::Send(inst->socket, H_ROOM_MOVE, packet_data, sizeof(int) * 3);

		//cout << inst->socket<<" is Entered Ch." << chan << " Room #" << move_room << endl;
		delete[] packet_data;
	}
}

void GameManager::ClientTimeOutSet(int client_socket)
{
	if (clients_info[client_socket] == nullptr)
		return;

	PacketManger::Send(client_socket, H_TIMEOUT_SET);
	clients_info[client_socket]->out_time = time(NULL) + 10;
}

vector<Client*> GameManager::GetClientListInRoom(int channelIndex, int roomIndex)
{
	vector<Client*> clientList;

	if (channelIndex < 0 || channelIndex >= MAX_CHANNEL_COUNT)
		return clientList;

	if (roomIndex < 0 || roomIndex >= MAX_ROOM_COUNT_PER_CHANNEL)
		return clientList;


	clientList = vector<Client*>(client_channel[channelIndex].room_list[roomIndex].client_list_room.begin(),
		client_channel[channelIndex].room_list[roomIndex].client_list_room.end());

	//	clientList = GameManager::client_channel[channelIndex].client_list_room[roomIndex];

	return clientList;
}

vector<ChatEntry> GameManager::GetChatLog(int channelIndex, int roomIndex)
{
	vector<ChatEntry> chatLog;

	if (channelIndex < 0 || channelIndex >= MAX_CHANNEL_COUNT)
	{
		cerr << "Error: Invalid channel index " << channelIndex << endl;
		return chatLog;
	}

	if (roomIndex < 0 || roomIndex >= MAX_ROOM_COUNT_PER_CHANNEL)
	{
		cerr << "Error: Invalid room index " << roomIndex << " in channel " << channelIndex << endl;
		return chatLog;
	}

	chatLog = client_channel[channelIndex].room_list[roomIndex].chat_log;

	return chatLog;
}

void GameManager::ClientReady(int client_socket, int size, void* data)
{
	int champindex;
	memcpy(&champindex, data, sizeof(int));

	BYTE* packet_data = new BYTE[sizeof(int) + sizeof(bool) + sizeof(int)];


	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	for (auto inst : client_channel[chan].room_list[room].client_list_room)
	{
		if (inst->socket == client_socket)
		{
			inst->ready = !inst->ready;
			inst->champindex = champindex;

			memcpy(packet_data, &client_socket, sizeof(int));
			memcpy(packet_data + sizeof(int), &inst->ready, sizeof(bool));
			memcpy(packet_data + sizeof(int) + sizeof(bool), &champindex, sizeof(int));

			for (auto client : client_channel[chan].room_list[room].client_list_room)
				PacketManger::Send(client->socket, H_IS_READY, packet_data, sizeof(int) + sizeof(bool) + sizeof(int));


			if (inst->ready)
				cout << "ready to " << inst->socket << endl;
			else
				cout << "disready to " << inst->socket << endl;

			delete[] packet_data;
			break;
		}
	}
}

//  level 마다 지정된 maxexp (전체 공통)
//  성장 능력치 상승치 grow* (챔프 개인)

/*void GameManager::ClientAuth(int socket, void* data) {

	const size_t length = 40;
	char user_code[length];
	memcpy(user_code, data, length);

	//간혹 여기서 두번 실행될떄가 있다. 그러면 my_client가 두개가 되므로 시큐어 코딩 요구
	if (GameManager::isExistClientCode(user_code)) {
		cout << "인가된 사용자입니다." << endl;

		Client* client = nullptr;
		for (Client* inst : client_list_all) {
			if (inst->user_code == user_code) {
				client = inst;
				break;
			}
		}

		if (client == nullptr)
			client = clients_info[socket];


		/*for (auto& roominfo : auth_data) {
			for (auto& j : roominfo.user_data) {
				if (j.user_code == user_code) {

					if (roominfo.channel == 0 && roominfo.room == 0) //방안에 들어가면 roominfo의 chan,room을 등록한다.
					{
						client_list_all.push_back(client);
						client->user_code = j.user_code;
						client->user_name = j.user_name;
						j.user_client = client;
						ClientMatch(socket);
						return;
					}
					else {

						client = j.user_client;
						client->socket = socket;
						clients_info[socket] = client;
						cout << "socket : " << client->socket << endl;
						ReConnection(client, socket, client->channel, client->room);
					}
				}
				
			
		
	}
	else
	{
		cout << socket << "는 웹서버 비인가 사용자입니다." << endl;
		closesocket(socket);
	}

}
*/

void GameManager::ReConnection(Client* client, int socket, int chan, int room) {
	//게임중인 녀석들중에서 user_code에 해당하는 녀석이 있다 >> 해당 룸을 동기화

	for (auto inst : client_channel[chan].room_list[room].client_list_room) // 룸의 클라이언트들을 생성합니다.
	{
		if (inst->socket == socket)
			continue;

		ClientInfo info;
		info.socket = inst->socket;
		info.champindex = inst->champindex;
		PacketManger::Send(socket, H_NEWBI, &info, sizeof(ClientInfo));

	}

	for (auto inst : client_channel[chan].room_list[room].client_list_room) // 원래 있던 클라이언트들도 알아야지
	{
		if (inst->socket == socket)
			continue;

		ClientInfo info;
		info.socket = socket;
		info.champindex = inst->champindex;
		PacketManger::Send(inst->socket, H_NEWBI, &info, sizeof(ClientInfo));

	}

	list<Client*> selected_clients = client_channel[chan].room_list[room].client_list_room;
	size_t client_count = selected_clients.size();
	BYTE* packet_data = new BYTE[sizeof(int) * 2 * client_count];
	size_t packet_size = sizeof(int) * 2 * client_count;
	int index = 0;
	for (auto& client : selected_clients)
	{
		memcpy(packet_data + sizeof(int) * index, &(client->socket), sizeof(int));
		index++;

		memcpy(packet_data + sizeof(int) * index, &(client->team), sizeof(int));
		index++;
	}

	for (auto& inst : selected_clients)
	{
		PacketManger::Send(inst->socket, H_BATTLE_START, packet_data, packet_size);
		Room::ClientChampInit(inst->socket);
		Room::ClientStat(inst->socket);
	}
	for (auto& inst : client_channel[chan].room_list[room].structure_list_room)
	{
		structureInfo info;
		info.index = inst->index;
		info.team = inst->team;
		info.x = inst->x;
		info.y = inst->y;
		info.z = inst->z;

		info.curhp = inst->curhp;
		info.maxhp = inst->maxhp;
		info.attrange = inst->attrange;
		info.bulletspeed = inst->bulletspeed;
		info.bulletdmg = inst->bulletdmg;

		PacketManger::Send(socket, H_STRUCTURE_CREATE, &info, sizeof(structureInfo));
	}

	delete[] packet_data;
}
