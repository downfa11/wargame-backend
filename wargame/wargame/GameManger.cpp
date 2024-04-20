#include "GameManager.h"

list<Client*> GameManager::client_list_all;
vector<roomData> GameManager::auth_data;

ClientChannel GameManager::client_channel[MAX_CHANNEL_COUNT];
Client* GameManager::clients_info[MAX_CLIENT];

bool GameManager::exit_connect = false;
int GameManager::timeout_check_time = 0;
chrono::high_resolution_clock::time_point GameManager::lastUpdateTime = chrono::high_resolution_clock::now();

unordered_map<int, atomic<bool>> turretSearchStopMap;


//  level 마다 지정된 maxexp (전체 공통)
//  성장 능력치 상승치 grow* (챔프 개인)


#define PICK_TIME 60

using namespace std;


float Distance(float x1, float y1, float z1, float x2, float y2, float z2)
{
	float dx = x1 - x2;
	float dy = y1 - y2;
	float dz = z1 - z2;

	return sqrt(dx * dx + dy * dy + dz * dz);
}

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
			printf("Time out %d \n", inst->socket);
			ClientClose(inst->socket);
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

	client_channel[chan].client_list_room[room].push_back(temp_cli);
	client_list_all.push_back(temp_cli);

	cout << "Connect " << client_socket << endl;
}

void GameManager::ClientClose(int client_socket)
{
	if (clients_info[client_socket] == nullptr) {
		cout << "웅!" << endl;
		return;
	}

	int chan = clients_info[client_socket]->channel, room = clients_info[client_socket]->room;
	clients_info[client_socket] = nullptr;


	for (auto it = client_list_all.begin(); it != client_list_all.end(); ++it)
	{
		if ((*it)->socket == client_socket)
		{
			it = client_list_all.erase(it);
			cout << client_socket<<"의 client_list_all 삭제" << endl;
			break;
		}
	}

	for (Client* inst : client_channel[chan].client_list_room[room]) {

		if (inst->socket == client_socket)
			inst->socket = -1;

		PacketManger::Send(inst->socket, H_USER_DISCON, &client_socket, sizeof(int));
	}

	closesocket(client_socket);
	cout << "Disconnected " << client_socket << endl;
}

void GameManager::ClientChat(int client_socket, int size, void* data)
{
	BYTE* packet_data = new BYTE[size + sizeof(int)];
	memcpy(packet_data, &client_socket, sizeof(int));
	memcpy(&packet_data[sizeof(int)], data, size);

	Client* sender = clients_info[client_socket];
	int chan = sender->channel;
	int room = sender->room;

	if (chan < 0 || chan >= 100 || room < 0 || room >= client_channel[chan].chat_log.size()) {
		cout << "Invalid channel or room index." << endl;
		delete[] packet_data;
		return;
	}
	vector<ChatEntry> chatLog = client_channel[chan].chat_log[room];

	auto curtime = chrono::system_clock::now();
	if (chatLog.size() == 0)
		chatLog.push_back({ "host", {curtime, "Game Start - 게임 시작을 알립니다."} });

	string chat(reinterpret_cast<char*>(&packet_data[sizeof(int)]), size);

	chatLog.push_back({ sender->user_name, {curtime, chat} });
	client_channel[chan].chat_log[room] = chatLog;

	for (auto cli : client_channel[chan].client_list_room[room])
	{
		PacketManger::Send(cli->socket, H_CHAT, packet_data, size + sizeof(int));
	}
	delete[] packet_data;
}

void GameManager::ClientChanMove(int client_socket, void* data)
{
	int move_chan;
	memcpy(&move_chan, data, sizeof(int));

	int chan = clients_info[client_socket]->channel;;
	int room = clients_info[client_socket]->room;

	clients_info[client_socket]->channel = move_chan;

	for (auto inst : client_channel[chan].client_list_room[room])
	{
		if (inst->socket == client_socket)
		{
			client_channel[chan].client_list_room[room].remove(inst);
			client_channel[move_chan].client_list_room[room].push_back(inst);
			break;
		}
	}

	for (auto inst : client_channel[chan].client_list_room[room])
		PacketManger::Send(inst->socket, H_USER_DISCON, &client_socket, sizeof(client_socket));

	ClientInfo newbi_info;
	newbi_info.socket = clients_info[client_socket]->socket;
	newbi_info.x = clients_info[client_socket]->x;
	newbi_info.y = clients_info[client_socket]->y;

	for (auto inst : client_channel[move_chan].client_list_room[room])
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

	for (auto inst : client_channel[chan].client_list_room[room])
	{
		if (inst->socket == client_socket)
		{
			client_channel[chan].client_list_room[room].remove(inst);
			client_channel[chan].client_list_room[move_room].push_back(inst);
			break;
		}
	}

	for (auto inst : client_channel[chan].client_list_room[move_room])
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

void GameManager::ClientMoveStart(int client_socket, void* data)
{
	ClientMovestart info;
	memcpy(&info, data, sizeof(ClientMovestart));

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	clients_info[client_socket]->rotationX = info.rotationX;
	clients_info[client_socket]->rotationY = info.rotationY;
	clients_info[client_socket]->rotationZ = info.rotationZ;
	clients_info[client_socket]->stopped = false;
	for (auto inst : client_channel[chan].client_list_room[room])
	{
		if (inst->socket != client_socket)
			PacketManger::Send(inst->socket, H_MOVESTART, &info, sizeof(ClientMovestart));
	}
}

void GameManager::ClientMove(int client_socket, void* data)
{
	ClientInfo info;
	memcpy(&info, data, sizeof(ClientInfo));

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	clients_info[client_socket]->x = info.x;
	clients_info[client_socket]->y = info.y;
	clients_info[client_socket]->z = info.z;

	for (auto inst : client_channel[chan].client_list_room[room])
	{
		if (inst->socket != client_socket)
			PacketManger::Send(inst->socket, H_MOVE, &info, sizeof(ClientInfo));
	}
}

void GameManager::ClientMoveStop(int client_socket, void* data)
{
	//움직인다
	ClientInfo info;
	memcpy(&info, data, sizeof(ClientInfo));

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	clients_info[client_socket]->x = info.x;
	clients_info[client_socket]->y = info.y;
	clients_info[client_socket]->z = info.z;
	clients_info[client_socket]->stopped = true;
	for (auto inst : client_channel[chan].client_list_room[room])
	{
		if (inst->socket != client_socket)
			PacketManger::Send(inst->socket, H_MOVESTOP, &info, sizeof(ClientInfo));
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


	clientList = vector<Client*>(client_channel[channelIndex].client_list_room[roomIndex].begin(),
		client_channel[channelIndex].client_list_room[roomIndex].end());

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

	chatLog = client_channel[channelIndex].chat_log[roomIndex];

	return chatLog;
}

void GameManager::ClientReady(int client_socket, int size, void* data)
{
	int champindex;
	memcpy(&champindex, data, sizeof(int));

	BYTE* packet_data = new BYTE[sizeof(int) + sizeof(bool) + sizeof(int)];


	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	for (auto inst : client_channel[chan].client_list_room[room])
	{
		if (inst->socket == client_socket)
		{
			inst->ready = !inst->ready;
			inst->champindex = champindex;

			memcpy(packet_data, &client_socket, sizeof(int));
			memcpy(packet_data + sizeof(int), &inst->ready, sizeof(bool));
			memcpy(packet_data + sizeof(int) + sizeof(bool), &champindex, sizeof(int));

			for (auto client : client_channel[chan].client_list_room[room])
				PacketManger::Send(client->socket, H_IS_READY, packet_data, sizeof(int) + sizeof(bool) + sizeof(int));


			if (inst->ready)
				cout << "ready to " << inst->socket <<":"<<champindex << endl;
			else
				cout << "disready to " << inst->socket << endl;

			delete[] packet_data;
			break;
		}
	}
}

bool GameManager::AllClientsReady(int channel, int room) {

	if (client_channel[channel].client_list_room[room].size() != MAX_CLIENT_PER_ROOM)
		return false;

	for (auto inst : client_channel[channel].client_list_room[room]) {
		if (!inst->ready)
			return false;
	}

	return true;
}

void GameManager::SendVictory(int client_socket, int winTeam, int channel, int room)
{
	auto structurelist = client_channel[channel].structure_list_room[room];
	auto clientist = client_channel[channel].client_list_room[room];

	for (auto inst : structurelist)
		StopTurretSearch(inst->index);
	structurelist.clear();

	auto condition = [channel, room](roomData& roomInfo) { 
		return roomInfo.channel == channel && roomInfo.room == room; };
	auth_data.erase(std::remove_if(auth_data.begin(), auth_data.end(), condition), auth_data.end());

	int win = (clients_info[client_socket]->team == winTeam) ? 1 : 0;
	BYTE packet_data[sizeof(int) + sizeof(int)];
	memcpy(packet_data, &client_socket, sizeof(int));
	memcpy(packet_data + sizeof(int), &win, sizeof(int));

	MatchResult result;
	for (auto inst : clientist)
	{
		PacketManger::Send(inst->socket, H_VICTORY, packet_data, sizeof(int) + sizeof(int));

		if (inst->team == winTeam)
			result.winTeams.push_back(inst);
		else
			result.loseTeams.push_back(inst);

	}


	char dateTimeFormat[] = "%Y-%m-%d %H:%M:%S";

	result.state = "success";

	for (auto& inst : auth_data)
		if (inst.channel == channel && inst.room == room)
			result.spaceId = inst.spaceId;
	
	auto now = chrono::system_clock::now();
	auto now_c = chrono::system_clock::to_time_t(now);
	std::tm tm;
	localtime_s(&tm, &now_c);

	std::stringstream ss;
	ss << std::put_time(&tm, "%Y-%m-%d %H:%M:%S");

	result.dateTime = ss.str();
	result.winTeamString = (winTeam == 0 ? "blue" : "red");
	result.loseTeamString = (winTeam == 0 ? "red" : "blue");


	chrono::time_point<chrono::system_clock> endTime = chrono::system_clock::now();
	chrono::time_point<chrono::system_clock>& startTime = client_channel[channel].startTime[room];

	chrono::duration<double> elapsed_seconds = endTime - startTime;
	int gameMinutes = static_cast<int>(elapsed_seconds.count() / 60);

	result.gameDuration = gameMinutes;

	SaveMatchResult(result);


	for (auto inst : clientist)
		ClientClose(inst->socket);

	startTime = chrono::time_point<chrono::system_clock>(); //init

	client_channel[channel].client_list_room[room].clear();
	client_channel[channel].structure_list_room[room].clear();
}

bool SendToMailslot(const string& message) {
	HANDLE hMailslot;
	DWORD bytesWritten;

	hMailslot = CreateFile(MAILSLOT_RESULT_ADDRESS, GENERIC_WRITE, FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
	if (hMailslot == INVALID_HANDLE_VALUE) {
		cerr << "Failed to open mailslot" << endl;
		return false;
	}

	BOOL result = WriteFile(hMailslot, message.c_str(), message.length(), &bytesWritten, NULL);
	CloseHandle(hMailslot);

	return result != 0;
}

void GameManager::SaveMatchResult(const MatchResult& result) {
	string json = matchResultToJson(result);
	cout << "send kafka : " << json << endl;

	if (!SendToMailslot(json))
		cerr << "Failed to send message through mailslot" << endl;
	else cout << "Send successfully." << endl;
}

void GameManager::ClientStat(int client_socket) {

	if (clients_info[client_socket] == nullptr)
		return;


	ClientInfo info;
	info.socket = client_socket;
	info.champindex = clients_info[client_socket]->champindex;
	info.gold = clients_info[client_socket]->gold;

	info.kill = clients_info[client_socket]->kill;
	info.death = clients_info[client_socket]->death;
	info.assist = clients_info[client_socket]->assist;

	info.level = clients_info[client_socket]->level;
	info.curhp = clients_info[client_socket]->curhp;
	info.maxhp = clients_info[client_socket]->maxhp;
	info.curmana = clients_info[client_socket]->curmana;
	info.maxmana = clients_info[client_socket]->maxmana;
	info.attack = clients_info[client_socket]->attack;
	info.critical = clients_info[client_socket]->critical;
	info.criProbability = clients_info[client_socket]->criProbability;
	info.attrange = clients_info[client_socket]->attrange;
	info.attspeed = clients_info[client_socket]->attspeed;
	info.movespeed = clients_info[client_socket]->movespeed;

	itemSlots slots;
	slots.socket = client_socket;
	size_t itemCount = clients_info[client_socket]->itemList.size();
	size_t maxItemCount = min(itemCount, size_t(6));
	for (size_t i = 0; i < maxItemCount; ++i) {
		if (i == 0) slots.id_0 = clients_info[client_socket]->itemList[i];
		if (i == 1) slots.id_1 = clients_info[client_socket]->itemList[i];
		if (i == 2) slots.id_2 = clients_info[client_socket]->itemList[i];
		if (i == 3) slots.id_3 = clients_info[client_socket]->itemList[i];
		if (i == 4) slots.id_4 = clients_info[client_socket]->itemList[i];
		if (i == 5) slots.id_5 = clients_info[client_socket]->itemList[i];
	}
	for (size_t i = maxItemCount; i < 6; ++i) {
		if (i == 0) slots.id_0 = 0;
		if (i == 1) slots.id_1 = 0;
		if (i == 2) slots.id_2 = 0;
		if (i == 3) slots.id_3 = 0;
		if (i == 4) slots.id_4 = 0;
		if (i == 5) slots.id_5 = 0;
	}

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	for (auto inst : client_channel[chan].client_list_room[room])
	{
		PacketManger::Send(inst->socket, H_CLIENT_STAT, &info, sizeof(ClientInfo));
		PacketManger::Send(inst->socket, H_ITEM_STAT, &slots, sizeof(itemSlots));
	}
}

void GameManager::ClientChampInit(int client_socket) {
	ClientInfo info;

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	int champIndex = clients_info[client_socket]->champindex;
	ChampionStats* champ = nullptr;
	cout << "사용자의 챔피언은 " << champIndex << endl;
	for (auto& champion : ChampionSystem::champions) {
		if (champion.index == champIndex) {
			champ = &champion;
			break;
		}
	}

	if (champ == nullptr) {
		cout << "champ를 찾을 수 없엉" << endl;
		return;
	}

	clients_info[client_socket]->level = 1;
	clients_info[client_socket]->curhp = (*champ).maxhp;
	clients_info[client_socket]->maxhp = (*champ).maxhp;
	clients_info[client_socket]->curmana = (*champ).maxmana;
	clients_info[client_socket]->maxmana = (*champ).maxmana;
	clients_info[client_socket]->attack = (*champ).attack;
	clients_info[client_socket]->maxdelay = (*champ).maxdelay;
	clients_info[client_socket]->attrange = (*champ).attrange;
	clients_info[client_socket]->attspeed = (*champ).attspeed;
	clients_info[client_socket]->movespeed = (*champ).movespeed;
	clients_info[client_socket]->critical = (*champ).critical;
	clients_info[client_socket]->criProbability = (*champ).criProbability;

	clients_info[client_socket]->growhp = (*champ).growHp;
	clients_info[client_socket]->growmana = (*champ).growMana;
	clients_info[client_socket]->growAtt = (*champ).growAtt;
	clients_info[client_socket]->growCri = (*champ).growCri;
	clients_info[client_socket]->growCriPro = (*champ).growCriPob;

	info.socket = client_socket;
	info.x = clients_info[client_socket]->x;
	info.y = clients_info[client_socket]->y;
	info.z = clients_info[client_socket]->z;

	info.champindex = clients_info[client_socket]->champindex;
	info.gold = clients_info[client_socket]->gold;
	info.level = clients_info[client_socket]->level;
	info.curhp = clients_info[client_socket]->curhp;
	info.maxhp = clients_info[client_socket]->maxhp;
	info.curmana = clients_info[client_socket]->curmana;
	info.maxmana = clients_info[client_socket]->maxmana;
	info.attack = clients_info[client_socket]->attack;
	info.critical = clients_info[client_socket]->critical;
	info.criProbability = clients_info[client_socket]->criProbability;
	info.attspeed = clients_info[client_socket]->attspeed;
	info.attrange = clients_info[client_socket]->attrange;
	info.movespeed = clients_info[client_socket]->movespeed;

	itemSlots slots;

	slots.socket = client_socket;
	slots.id_0 = clients_info[client_socket]->itemList[0];
	slots.id_1 = clients_info[client_socket]->itemList[1];
	slots.id_2 = clients_info[client_socket]->itemList[2];
	slots.id_3 = clients_info[client_socket]->itemList[3];
	slots.id_4 = clients_info[client_socket]->itemList[4];
	slots.id_5 = clients_info[client_socket]->itemList[5];

	for (auto inst : client_channel[chan].client_list_room[room])
	{
		PacketManger::Send(inst->socket, H_CHAMPION_INIT, &info, sizeof(ClientInfo));
		PacketManger::Send(inst->socket, H_ITEM_STAT, &slots, sizeof(itemSlots));
	}
}

void GameManager::ClientChampReconnectInit(int client_socket) {
	ClientInfo info;

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	
	info.socket = client_socket;

	info.x = clients_info[client_socket]->x;
	info.y = clients_info[client_socket]->y;
	info.z = clients_info[client_socket]->z;

	info.kill = clients_info[client_socket]->kill;
	info.death = clients_info[client_socket]->death;
	info.assist = clients_info[client_socket]->assist;

	info.champindex = clients_info[client_socket]->champindex;
	info.gold = clients_info[client_socket]->gold;
	info.level = clients_info[client_socket]->level;
	info.curhp = clients_info[client_socket]->curhp;
	info.maxhp = clients_info[client_socket]->maxhp;
	info.curmana = clients_info[client_socket]->curmana;
	info.maxmana = clients_info[client_socket]->maxmana;
	info.attack = clients_info[client_socket]->attack;
	info.critical = clients_info[client_socket]->critical;
	info.criProbability = clients_info[client_socket]->criProbability;
	info.attspeed = clients_info[client_socket]->attspeed;
	info.attrange = clients_info[client_socket]->attrange;
	info.movespeed = clients_info[client_socket]->movespeed;

	itemSlots slots;
	slots.socket = client_socket;
	size_t itemCount = clients_info[client_socket]->itemList.size();
	size_t maxItemCount = min(itemCount, size_t(6));
	for (size_t i = 0; i < maxItemCount; ++i) {
		if (i == 0) slots.id_0 = clients_info[client_socket]->itemList[i];
		if (i == 1) slots.id_1 = clients_info[client_socket]->itemList[i];
		if (i == 2) slots.id_2 = clients_info[client_socket]->itemList[i];
		if (i == 3) slots.id_3 = clients_info[client_socket]->itemList[i];
		if (i == 4) slots.id_4 = clients_info[client_socket]->itemList[i];
		if (i == 5) slots.id_5 = clients_info[client_socket]->itemList[i];
	}
	for (size_t i = maxItemCount; i < 6; ++i) {
		if (i == 0) slots.id_0 = 0;
		if (i == 1) slots.id_1 = 0;
		if (i == 2) slots.id_2 = 0;
		if (i == 3) slots.id_3 = 0;
		if (i == 4) slots.id_4 = 0;
		if (i == 5) slots.id_5 = 0;
	}

	for (auto inst : client_channel[chan].client_list_room[room])
	{
		PacketManger::Send(inst->socket, H_CHAMPION_INIT, &info, sizeof(ClientInfo));
		PacketManger::Send(inst->socket, H_ITEM_STAT, &slots, sizeof(itemSlots));
	}
}

void GameManager::MouseSearch(int client_socket, void* data)
{

	mouseInfo info;
	memcpy(&info, data, sizeof(mouseInfo));

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;
	int team = clients_info[client_socket]->team;

	double minDistance = FLT_MAX * 2;
	Client* closestClient = nullptr;
	structure* closestStruct = nullptr;

	for (auto inst : client_channel[chan].structure_list_room[room])
	{
		if (team != inst->team)
		{
			float distance = Distance(info.x, info.y, info.z, inst->x, inst->y, inst->z);
			if (distance < minDistance)
			{
				closestStruct = inst;
				minDistance = distance;
				closestClient = nullptr;
			}
		}
	}

	for (auto inst : client_channel[chan].client_list_room[room])
	{
		if (inst->socket != client_socket && team != inst->team)
		{
			float distance = Distance(info.x, info.y, info.z, inst->x, inst->y, inst->z);
			if (distance < minDistance)
			{
				closestClient = inst;
				minDistance = distance;
				closestStruct = nullptr;
			}
		}
	}

	if (closestStruct != nullptr)
	{

		for (auto inst : client_channel[chan].client_list_room[room])
		{
			if (inst->socket == client_socket)
			{
				for (auto struc : client_channel[chan].structure_list_room[room])
				{
					if (struc->index == closestStruct->index)
					{

						BYTE* packet_data = new BYTE[sizeof(int) + sizeof(int)];
						memcpy(packet_data, &client_socket, sizeof(int));
						memcpy(packet_data + sizeof(int), &closestStruct->index, sizeof(int));
						PacketManger::Send(inst->socket, H_ATTACK_TARGET, packet_data, sizeof(int) + sizeof(int));
						break;
					}
				}
			}
		}
	}
	else if (closestClient != nullptr)
	{
		for (auto inst : client_channel[chan].client_list_room[room])
		{
			if (inst->socket == client_socket)
			{
				if (inst->socket != closestClient->socket)
				{
					BYTE* packet_data = new BYTE[sizeof(int) + sizeof(int)];
					memcpy(packet_data, &client_socket, sizeof(int));
					memcpy(packet_data + sizeof(int), &closestClient->socket, sizeof(int));
					PacketManger::Send(inst->socket, H_ATTACK_TARGET, packet_data, sizeof(int) + sizeof(int));
					break;
				}

			}
		}

	}
}

void GameManager::AttackClient(int client_socket, void* data)
{
	attinfo info;
	memcpy(&info, data, sizeof(attinfo));
	int attackedsocket = info.attacked;

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	list<Client*> clients_in_room = client_channel[chan].client_list_room[room];

	auto attacker = find_if(clients_in_room.begin(), clients_in_room.end(), [client_socket](Client* client) {
		return client->socket == client_socket;
		});
	auto attacked = find_if(clients_in_room.begin(), clients_in_room.end(), [attackedsocket](Client* client) {
		return client->socket == attackedsocket;
		});

	if (attacker == clients_in_room.end() || attacked == clients_in_room.end())
		return;

	if ((*attacked)->curhp <= 0)
		return;

	float distance = Distance((*attacker)->x, (*attacker)->y, (*attacker)->z, (*attacked)->x, (*attacked)->y, (*attacked)->z);
	if (distance <= (*attacker)->attrange)
	{

		if (!clients_info[client_socket]->stopped)
		{

			int i = 0;
			PacketManger::Send(client_socket, H_CLIENT_STOP, &i, sizeof(int));
			clients_info[client_socket]->stopped = true;
		}


		if (clients_info[client_socket]->curdelay >= clients_info[client_socket]->maxdelay) {
			if (!clients_info[client_socket]->attacked) {

				for (auto inst : client_channel[chan].client_list_room[room])
				{
					BYTE* packet_data = new BYTE[sizeof(int) + sizeof(int)];
					memcpy(packet_data, &((*attacker)->socket), sizeof(int));
					memcpy(packet_data + sizeof(int), &((*attacked)->socket), sizeof(int));
					PacketManger::Send(inst->socket, H_ATTACK_CLIENT, packet_data, sizeof(int) + sizeof(int));
				}
				clients_info[client_socket]->attacked = true;
			}

			if (clients_info[client_socket]->curdelay >= clients_info[client_socket]->maxdelay + clients_info[client_socket]->attspeed) {

				random_device rd;
				mt19937 gen(rd());
				uniform_real_distribution<float> dis(0, 1); //균일한 분포;를 형성
				float chance = (*attacker)->criProbability / 100.0;
				float cridmg = (*attacker)->critical / 100.0;
				int att = (*attacker)->attack;

				if (dis(gen) < chance)
				{
					float critDamage = (*attacker)->attack * cridmg;
					att = (*attacker)->attack + (int)critDamage;
				}

				//cout << (*attacker)->socket << " 의 공격 :: " << (*attacked)->socket << " " << clients_info[client_socket]->curdelay << " " << endl;;
				(*attacked)->curhp -= att;
				clients_info[client_socket]->curdelay = 0;
				clients_info[client_socket]->attacked = false;
				chrono::time_point<chrono::system_clock> startTime = client_channel[chan].startTime[room];
				chrono::time_point<chrono::system_clock> currentTime = chrono::system_clock::now();
				chrono::duration<double> newTime = currentTime - startTime;

				while ((*attacked)->assistList.size() > MAX_TEAM_PER_ROOM-1) {
					(*attacked)->assistList.pop();
				}

				while (!(*attacked)->assistList.empty() && (*attacked)->assistList.top().first == clients_info[client_socket]->socket) {
					(*attacked)->assistList.pop();
				}

				while (!(*attacked)->assistList.empty() && newTime.count() - (*attacked)->assistList.top().second > 30) {
					(*attacked)->assistList.pop();
				}

				(*attacked)->assistList.push(make_pair(clients_info[client_socket]->socket, newTime.count()));
				//cout << (*attacked)->assistList.top().first << " 의 시간 : " << newTime.count() << endl;


				for (auto inst : client_channel[chan].client_list_room[room])
				{
					if (inst->socket == (*attacked)->socket)
						ClientStat((*attacked)->socket);

					if ((*attacked)->curhp <= 0) { //승리조건 상대를 죽일시
						//SendVictory((*attacked)->socket, (*attacker)->team, chan, room);
						(*attacker)->gold += 1000;
						(*attacker)->exp += 100;
						CharLevelUp(*attacker);
						ClientStat((*attacker)->socket);
						ClientDie((*attacked)->socket, (*attacker)->socket);
						break;
					}


				}

				attacker = find_if(clients_in_room.begin(), clients_in_room.end(), [client_socket](Client* client) {
					return client->socket == client_socket;
					});
				attacked = find_if(clients_in_room.begin(), clients_in_room.end(), [attackedsocket](Client* client) {
					return client->socket == attackedsocket;
					});
			}
			else {
				clients_info[client_socket]->stopped = true;


				auto currentTime = chrono::high_resolution_clock::now();
				auto deltaTime = currentTime - lastUpdateTime;
				float deltaTimeInSeconds = chrono::duration<float>(deltaTime).count();
				clients_info[client_socket]->curdelay += deltaTimeInSeconds;
				lastUpdateTime = currentTime;
			}
		}
		else {
			clients_info[client_socket]->stopped = true;


			auto currentTime = chrono::high_resolution_clock::now();
			auto deltaTime = currentTime - lastUpdateTime;
			float deltaTimeInSeconds = chrono::duration<float>(deltaTime).count();
			clients_info[client_socket]->curdelay += deltaTimeInSeconds;
			lastUpdateTime = currentTime;
		}
	}
	else {
		mouseInfo info4;
		info4.x = (*attacked)->x;
		info4.y = (*attacked)->y;
		info4.z = (*attacked)->z;
		MouseSearch(client_socket, &info4);
	}


}

void GameManager::AttackStruct(int client_socket, void* data)
{


	attinfo info;
	memcpy(&info, data, sizeof(attinfo));

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	list<Client*> clients_in_room = client_channel[chan].client_list_room[room];
	list<structure*> structure_in_room = client_channel[chan].structure_list_room[room];

	auto attacker = find_if(clients_in_room.begin(), clients_in_room.end(), [client_socket](Client* client) {
		return client->socket == client_socket;
		});

	int attackedIndex = info.attacked;
	auto attacked = find_if(structure_in_room.begin(), structure_in_room.end(), [attackedIndex](structure* struc_) {
		return struc_->index == attackedIndex;
		});

	if (attacker == clients_in_room.end() || attacked == structure_in_room.end())
		return;


	if ((*attacked)->curhp <= 0)
		return;


	float distance = Distance((*attacker)->x, (*attacker)->y, (*attacker)->z, (*attacked)->x, (*attacked)->y, (*attacked)->z);

	if (distance <= (*attacker)->attrange)
	{
		if (clients_info[client_socket]->curdelay >= clients_info[client_socket]->maxdelay) {


			random_device rd;
			mt19937 gen(rd());
			uniform_real_distribution<float> dis(0, 1); //균일한 분포;를 형성
			float chance = (*attacker)->criProbability / 100.0;
			float cridmg = (*attacker)->critical / 100.0;
			int att = (*attacker)->attack;

			if (dis(gen) < chance)
			{
				float critDamage = (*attacker)->attack * cridmg;
				att = (*attacker)->attack + (int)critDamage;
			}

			//cout << (*attacker)->socket << " 의 공격 :: " << (*attacked)->index << "'s Hp : " << (*attacked)->curhp;
			(*attacked)->curhp -= att;
			clients_info[client_socket]->curdelay = 0;

			for (auto inst : client_channel[chan].client_list_room[room])
			{
				BYTE* packet_data = new BYTE[sizeof(int) + sizeof(int)];
				memcpy(packet_data, &((*attacker)->socket), sizeof(int));
				memcpy(packet_data + sizeof(int), &((*attacked)->index), sizeof(int));
				PacketManger::Send(inst->socket, H_ATTACK_STRUCT, packet_data, sizeof(int) + sizeof(int));


				if ((*attacked)->curhp <= 0) { //승리조건 상대를 죽일시
					StructureDie((*attacked)->index, chan, room);
					break;
				}


			}
			StructureStat((*attacked)->index, chan, room);
			//cout << " -> " << (*attacked)->curhp << endl;

		}
		else {
			auto currentTime = chrono::high_resolution_clock::now();
			auto deltaTime = currentTime - lastUpdateTime;
			float deltaTimeInSeconds = chrono::duration<float>(deltaTime).count();
			clients_info[client_socket]->curdelay += deltaTimeInSeconds;
			lastUpdateTime = currentTime;
		}


	}
	else {
		mouseInfo info4;
		info4.x = (*attacked)->x;
		info4.y = (*attacked)->y;
		info4.z = (*attacked)->z;
		MouseSearch(client_socket, &info4);
	}
}

void GameManager::NewStructure(int index, int chan, int room, int team, int x, int y, int z)
{
	structure* temp_ = new structure;
	temp_->index = index;
	temp_->x = x;
	temp_->y = y;
	temp_->z = z;
	temp_->team = team;

	if (temp_->index > 30000) {//gate
		temp_->maxhp = 1500;
		temp_->curhp = temp_->maxhp;
		temp_->attrange = 0;
		temp_->bulletspeed = 0;
		temp_->bulletdmg = 0;
	}
	else if (temp_->index > 20000) { //turret
		temp_->maxhp = 1500;
		temp_->curhp = temp_->maxhp;
		temp_->attrange = 30;
		temp_->bulletspeed = 20;
		temp_->bulletdmg = 50;
		temp_->curdelay = 0;
		temp_->maxdelay = 2;
	}
	else if (temp_->index > 10000) {//nexus
		temp_->maxhp = 2000;
		temp_->curhp = temp_->maxhp;
		temp_->attrange = 0;
		temp_->bulletspeed = 0;
		temp_->bulletdmg = 0;
	}

	client_channel[chan].structure_list_room[room].push_back(temp_);


	structureInfo info;
	info.index = temp_->index;
	info.team = temp_->team;
	info.x = temp_->x;
	info.y = temp_->y;
	info.z = temp_->z;

	info.curhp = temp_->curhp;
	info.maxhp = temp_->maxhp;
	info.attrange = temp_->attrange;
	info.bulletspeed = temp_->bulletspeed;
	info.bulletdmg = temp_->bulletdmg;

	for (Client* inst : client_channel[chan].client_list_room[room])
		PacketManger::Send(inst->socket, H_STRUCTURE_CREATE, &info, sizeof(structureInfo));


	if (30000 > temp_->index && temp_->index > 20000) {
		thread turretSearchThread(&TurretSearchWorker, index, chan, room);
		turretSearchThread.detach();
		//TurretSearch
	}
}

void GameManager::StructureDie(int index, int chan, int room)
{
	for (auto inst : client_channel[chan].client_list_room[room])
		PacketManger::Send(inst->socket, H_STRUCTURE_DIE, &index, sizeof(int));

	auto list = client_channel[chan].structure_list_room[room];
	for (auto inst : list)
	{
		if (inst->index == index)
			client_channel[chan].structure_list_room[room].remove(inst);
	}

	if (30000 > index && index > 20000)
		StopTurretSearch(index);
}

void GameManager::StructureStat(int index, int chan, int room) {
	structure* stru_ = nullptr;
	for (auto inst : client_channel[chan].structure_list_room[room])
	{
		if (inst->index == index)
			stru_ = inst;
	}

	if (stru_ == nullptr)
		return;

	if (stru_->curhp <= 0)
	{
		stru_->curhp = 0;
		stru_->index = -1;
		StructureDie(index, chan, room);
		return;
	}

	structureInfo info;
	info.index = stru_->index;
	info.curhp = stru_->curhp;
	info.maxhp = stru_->maxhp;
	info.attrange = stru_->attrange;
	info.bulletdmg = stru_->bulletdmg;
	info.bulletspeed = stru_->bulletspeed;

	for (Client* inst : client_channel[chan].client_list_room[room])
	{
		PacketManger::Send(inst->socket, H_STRUCTURE_STAT, &info, sizeof(structureInfo));
	}
}

void GameManager::TurretSearch(int index, int chan, int room) {
	list<Client*>& clients_in_room = client_channel[chan].client_list_room[room];
	list<structure*>& structures_in_room = client_channel[chan].structure_list_room[room];
	auto attacker = find_if(structures_in_room.begin(), structures_in_room.end(), [index](structure* struc) {
		return struc->index == index;
		});

	//cout << (*attacker)->curdelay << endl;
	if (attacker != structures_in_room.end()) {
		for (auto client : clients_in_room) {

			if (client->team == (*attacker)->team)
				return;

			if (turretSearchStopMap[index])
				break;

			if (client == nullptr)
			{
				StopTurretSearch(index);
				break;
			}

			float distance = Distance((*attacker)->x, (*attacker)->y, (*attacker)->z, client->x, client->y, client->z);

			if (distance <= (*attacker)->attrange) {


				int attacked_ = client->socket;

				if (clients_info[attacked_] == nullptr)
				{
					ClientClose(attacked_);
					return;
				}

				if ((*attacker)->curdelay >= (*attacker)->maxdelay) {

					bullet* newBullet = new bullet;
					newBullet->x = (*attacker)->x;
					newBullet->y = (*attacker)->y;
					newBullet->z = (*attacker)->z;
					newBullet->dmg = (*attacker)->bulletdmg;
					TurretShot(index, newBullet, attacked_, chan, room);

				}
				else {
					auto currentTime = chrono::high_resolution_clock::now();
					auto deltaTime = currentTime - lastUpdateTime;
					float deltaTimeInSeconds = chrono::duration<float>(deltaTime).count();
					(*attacker)->curdelay += deltaTimeInSeconds;
					lastUpdateTime = currentTime;
				}
			}
		}
	}
}

void GameManager::TurretShot(int index, bullet* newBullet, int attacked_, int chan, int room) {
	list<Client*>& clients_in_room = client_channel[chan].client_list_room[room];
	list<structure*>& structures_in_room = client_channel[chan].structure_list_room[room];

	auto attacked = find_if(clients_in_room.begin(), clients_in_room.end(), [attacked_](Client* client) {
		return client->socket == attacked_;
		});

	auto attacker = find_if(structures_in_room.begin(), structures_in_room.end(), [index](structure* struc) {
		return struc->index == index;
		});

	if ((*attacked)->curhp <= 0)
		return;

	if (attacked != clients_in_room.end() && attacker != structures_in_room.end()) {
		float targetX = (*attacked)->x;
		float targetY = (*attacked)->y;
		float targetZ = (*attacked)->z;

		float directionX = targetX - newBullet->x;
		float directionY = targetY - newBullet->y;
		float directionZ = targetZ - newBullet->z;

		float distance = sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
		directionX /= distance;
		directionY /= distance;
		directionZ /= distance;

		float moveDistance = (*attacker)->bulletspeed / 500.0;  // 프레임 시간 간격 (단위: s)

		if (clients_info[attacked_] == nullptr)
		{
			ClientClose(attacked_);
			return;
		}

		while (Distance(newBullet->x, newBullet->y, newBullet->z, targetX, targetY, targetZ) > 2) {
			newBullet->x += directionX * moveDistance;
			newBullet->y += directionY * moveDistance;
			newBullet->z += directionZ * moveDistance;
			//cout << " Bullet position : (" << newBullet->x << ", " << newBullet->y << ", " << newBullet->z <<")"<< endl;
		}
		//cout << (*attacker)->index << " 의 공격 :: " << (*attacked)->socket << "'s Hp : " << (*attacked)->curhp << " -> ";
		(*attacked)->curhp -= newBullet->dmg;
		ClientStat((*attacked)->socket);
		(*attacker)->curdelay = 0;
		//cout << (*attacked)->curhp << endl;

		if ((*attacked)->curhp <= 0)
			ClientDie((*attacked)->socket, (*attacker)->index);

	}

	delete newBullet;
}

void GameManager::ClientDie(int client_socket, int killer) {

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	clients_info[client_socket]->death += 1;
	ClientStat(client_socket);
	if (killer < 1000)
	{
		cout << clients_info[killer]->socket << "의 킬 : " << clients_info[killer]->kill << endl;
		clients_info[killer]->kill += 1;
		ClientStat(killer);
	}
	else {
		list<structure*>& structures_in_room = client_channel[chan].structure_list_room[room];
		auto attacker = find_if(structures_in_room.begin(), structures_in_room.end(), [killer](structure* struc) {
			return struc->index == killer;
			});
		cout << (*attacker)->index << "의 킬 : " << clients_info[killer]->kill << endl;
	}
	cout << clients_info[client_socket]->socket << "의 데스 : " << clients_info[client_socket]->death << endl;

	if (clients_info[killer]->kill >= 2)
	{
		SendVictory(clients_info[killer]->socket, clients_info[killer]->team, chan, room);
		return;
	}
	attinfo info;
	info.attacked = client_socket;
	info.attacker = killer;



	int secondValue = -1; // top 바로 아래 값 (default는 -1)

	if (clients_info[client_socket]->assistList.size() > MAX_TEAM_PER_ROOM-1)
	{
		stack<pair<int, int>> tempStack = clients_info[client_socket]->assistList;

		// 스택이 MAX_TEAM_PER_ROOM 크기보다 크면 맨 아래 값을 제거
		vector<int> assistTargets;
		while (tempStack.size() > MAX_TEAM_PER_ROOM-1) {
			assistTargets.push_back(tempStack.top().first);
			tempStack.pop();
		}

		//cout << tempStack.top().first << " 킬, 시간 : " << tempStack.top().second << endl;

		// top을 pop하여 제거하고, 나머지 어시스트 사용자들의 정보를 출력
		tempStack.pop();

		int index = 0;
		while (!tempStack.empty())
		{
			int assistSocket = tempStack.top().first;

			for (auto inst : client_channel[chan].client_list_room[room])
			{
				if (inst->socket == tempStack.top().first) {
					inst->assist += 1;
					break;
				}
			}

			assistTargets.push_back(assistSocket);
			tempStack.pop();
		}
		int Assistindex = 0;
		for (int socket : assistTargets) {
			switch (Assistindex) {
			case 0:
				info.assist1 = socket;
				break;
			case 1:
				info.assist2 = socket;
				break;
			case 2:
				info.assist3 = socket;
				break;
			case 3:
				info.assist4 = socket;
				break;
			default:
				break;
			}
			Assistindex++;
		}

		// 어시스트 대상자들의 소켓을 출력
		//cout << "어시스트 대상자 소켓: " << info.assist1 << endl;
	}

	else {
		cout << "어시스트가 없거나 assistList에 요소가 부족합니다." << endl;
	}


	chrono::time_point<chrono::system_clock> startTime = client_channel[chan].startTime[room];
	chrono::time_point<chrono::system_clock> currentTime = chrono::system_clock::now();
	chrono::duration<double> elapsed_seconds = currentTime - startTime;

	int gameMinutes = static_cast<int>(elapsed_seconds.count() / 60);

	int respawnTime = 3 + (gameMinutes * 3); //min 3s

	for (auto inst : client_channel[chan].client_list_room[room]) {
		BYTE* packet_data = new BYTE[sizeof(attinfo)];
		memcpy(packet_data, &info, sizeof(attinfo));
		PacketManger::Send(inst->socket, H_KILL_LOG, packet_data, sizeof(attinfo));

		delete[] packet_data;

		BYTE* packet_data2 = new BYTE[sizeof(int) * 2];
		memcpy(packet_data2, &client_socket, sizeof(int));
		memcpy(packet_data2 + sizeof(int), &respawnTime, sizeof(int));
		PacketManger::Send(inst->socket, H_CLIENT_DIE, packet_data2, sizeof(int) * 2);

		delete[] packet_data2;
	}

	cout << client_socket << " 님이 사망 대기 시간은 " << respawnTime << "초입니다." << endl;

	thread respawnThread(WaitAndRespawn, respawnTime, currentTime, client_socket);
	respawnThread.detach();
}

void GameManager::WaitAndRespawn(int respawnTime, const chrono::time_point<chrono::system_clock>& diedtTime, int client_socket) {
	chrono::time_point<chrono::system_clock> currentTime = chrono::system_clock::now();
	chrono::duration<double> elapsed_seconds = currentTime - diedtTime;

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	while (elapsed_seconds.count() < respawnTime) {


		if (clients_info[client_socket] == nullptr)
			return;

		currentTime = chrono::system_clock::now();
		elapsed_seconds = currentTime - diedtTime;
	}
	ClientRespawn(client_socket);
}

void GameManager::TurretSearchWorker(int index, int chan, int room) {
	while (!turretSearchStopMap[index]) {
		this_thread::sleep_for(chrono::seconds(1));

		TurretSearch(index, chan, room);
	}
}

void GameManager::StopTurretSearch(int index) {

	turretSearchStopMap[index] = true;
}

void GameManager::ClientRespawn(int client_socket) {

	if (clients_info[client_socket] == nullptr)
		return;

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	for (auto inst : client_channel[chan].client_list_room[room])
	{
		if (inst->socket == client_socket) {
			inst->x = 0;
			inst->y = 0;
			inst->z = 0;
			inst->curhp = inst->maxhp;
			ClientStat(client_socket);
		}
	}

	for (auto inst : client_channel[chan].client_list_room[room])
	{
		BYTE* packet_data = new BYTE[sizeof(int)];
		memcpy(packet_data, &client_socket, sizeof(int));
		PacketManger::Send(inst->socket, H_CLIENT_RESPAWN, packet_data, sizeof(int));

		delete[] packet_data;
	}
}

void GameManager::ItemStat(int client_socket, void* data)
{
	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	Item info;
	memcpy(&info, data, sizeof(Item));

	int id = info.id;
	bool isPerchase = info.isPerchase;

	itemStats* curItem = nullptr;

	for (auto& item : ItemSystem::items) {
		if (item.id == id) {
			curItem = &item;
			break;
		}
	}

	int NeedGold = (*curItem).gold;

	int index = 0;
	auto nonZeroIt = find_if(clients_info[client_socket]->itemList.begin(), clients_info[client_socket]->itemList.end(),
		[](int item) { return item == 0; });

	if (nonZeroIt != clients_info[client_socket]->itemList.end())
		index = distance(clients_info[client_socket]->itemList.begin(), nonZeroIt);
	else index = clients_info[client_socket]->itemList.size() - 1;

	if (isPerchase)
	{
		if (index >= clients_info[client_socket]->itemList.size()) // 더 이상 추가 아이템을 구매할 수 없을 때
		{
			cout << "꽉 찼어." << endl;
			return;
		}
		else if (clients_info[client_socket]->gold >= NeedGold)
		{
			clients_info[client_socket]->gold -= NeedGold;
			clients_info[client_socket]->maxhp += (*curItem).maxhp;
			clients_info[client_socket]->attack += (*curItem).attack;
			clients_info[client_socket]->maxdelay += (*curItem).maxdelay;
			clients_info[client_socket]->attspeed += (*curItem).attspeed;
			clients_info[client_socket]->movespeed += (*curItem).movespeed;
			clients_info[client_socket]->criProbability += (*curItem).criProbability;

			cout << client_socket << "님이 " << (*curItem).name << " 를 " << NeedGold << "에 구매하는데 성공했습니다." << endl;

			clients_info[client_socket]->itemList[index] = ((*curItem).id); // 아이템 추가
			/*
			for (const int& itemID : clients_info[client_socket]->itemList)
			{
				cout << itemID << endl;
			}
			*/

			ClientStat(client_socket);
		}
		else
		{
			cout << "show me the money" << endl;
		}
	}
	else
	{
		for (int i = 0; i < clients_info[client_socket]->itemList.size(); i++)
		{
			if (clients_info[client_socket]->itemList[i] == id)
			{
				clients_info[client_socket]->gold += NeedGold * 0.8f;
				clients_info[client_socket]->maxhp -= (*curItem).maxhp;
				clients_info[client_socket]->attack -= (*curItem).attack;
				clients_info[client_socket]->maxdelay -= (*curItem).maxdelay;
				clients_info[client_socket]->attspeed -= (*curItem).attspeed;
				clients_info[client_socket]->movespeed -= (*curItem).movespeed;
				clients_info[client_socket]->criProbability -= (*curItem).criProbability;

				cout << client_socket << "님이 " << (*curItem).name << " 를 " << NeedGold * 0.8f << "에 판매하는데 성공했습니다." << endl;

				clients_info[client_socket]->itemList[i] = 0; // 아이템 삭제
				/*for (const int& itemID : clients_info[client_socket]->itemList)
				{
					cout << itemID << endl;
				}*/

				ClientStat(client_socket);
				break;
			}
		}
	}
}

void GameManager::Well(int client_socket, void* data) {
	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	ClientInfo info;
	memcpy(&info, data, sizeof(ClientInfo));

	structure* stru_ = nullptr;
	for (auto inst : client_channel[chan].structure_list_room[room])
	{
		if (inst->index > 10000 && inst->index < 20000 && inst->team == clients_info[client_socket]->team)
			stru_ = inst;
	}

	if (stru_ == nullptr)
		return;

	float distance = Distance(stru_->x, stru_->y, stru_->z, info.x, info.y, info.z);
	int minDistance = 18;
	if (distance <= minDistance && clients_info[client_socket]->curhp < clients_info[client_socket]->maxhp)
	{
		clients_info[client_socket]->curhp += 1;
		ClientStat(client_socket);
	}
	else return;
}

void GameManager::CharLevelUp(Client* client) {
	if (client->exp < client->maxexp)
		return;

	client->maxhp += client->growhp;
	client->maxmana += client->growmana;
	client->attack += client->growAtt;
	client->critical += client->growCri;
	client->criProbability += client->growCriPro;

	client->exp = 0;

	client->maxexp = client->maxexp + (client->maxexp / 10 * client->level);
	client->level++;
	cout << "level up " << client->socket << endl;
	ClientStat(client->socket);
}

void GameManager::RoomAuth(int socket,int size, void* data) {
	BYTE* packet_data = new BYTE[size];
	memcpy(packet_data, data, size);

	string code(reinterpret_cast<char*>(packet_data), size);

	cout << code << "번 방 접속 허용." << endl;


	roomData curRoom;
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

	clients_info[socket]->code = code;
	PacketManger::Send(socket, H_RAUTHORIZATION, &code, size);


}

void GameManager::ClientAuth(int socket, void* data) {

	if (clients_info[socket]->code == "") {
		cout << socket << " 사용자는 room 인가를 받지 못했습니다." << endl;
		return;
	}

	int index = -1; // 사용자의 고유 인식번호

	memcpy(&index, data, sizeof(int));


	cout << index << "는 접속 허용." << endl;

	int chan = -1, room = -1;
	roomData curRoom;
	for (auto roomData : auth_data) {
		//cout << roomData.spaceId << " == " << clients_info[socket]->code << endl;
		if (roomData.spaceId == clients_info[socket]->code) {
			curRoom = roomData;

			chan = curRoom.channel;
			room = curRoom.room;

			cout << "올바른 방의 아이디 : " << curRoom.spaceId << endl;
			if (chan == 0)
				ClientRoomMove(socket, (void*)&room);
			else {
				ClientChanMove(socket, (void*)&chan);
				ClientRoomMove(socket, (void*)&room);
			}
		}
	}

	if (curRoom.spaceId == "") {
		cout << "clients_info[socket]->code가 잘못되었슴" << endl;
		return;
	}

	clients_info[socket]->clientindex = index;
	// cout <<"isGame : "<< curRoom.isGame << endl;
	
	// 처음 게임 접속
	if (curRoom.isGame==-1) {
		cout << "최초 접속 시도중 " << "code : " << curRoom.isGame << endl;
		int team = -1;
		vector<pair<int, int>> selected_clients;
		cout << "curRoom size - blue : " << curRoom.blueTeam.size() << ", red : " << curRoom.redTeam.size() << endl;
		
		for (auto& inst : auth_data) {
			if (inst.channel == chan && inst.room == room) {
				if (inst.isGame == -1)
					inst.isGame = 0;
				else cout << "isGame 오류" << endl;
			}
		}

		for (auto inst : curRoom.blueTeam) {
			cout << "blue : " << stoi(inst.user_index) << endl;
			if (stoi(inst.user_index) == index) {
				clients_info[socket]->user_name = inst.user_name;
				clients_info[socket]->team = 0;
				team = 0;
			}
			selected_clients.push_back(make_pair(stoi(inst.user_index), 0));
		}

		for (auto inst : curRoom.redTeam) {
			cout << "red : " << inst.user_index << endl;
			if (stoi(inst.user_index) == index) {
				clients_info[socket]->user_name = inst.user_name;
				clients_info[socket]->team = 1;
				team = 1;
			}
			selected_clients.push_back(make_pair(stoi(inst.user_index), 1));
		}

		if (team == -1)
			ClientClose(socket); // 나가라 넌ㅋ
		

		auto clientList = GameManager::GetClientListInRoom(chan, room);

		int client_count = selected_clients.size();
		BYTE* packet_data = new BYTE[sizeof(int) * 3 * client_count];
		int packet_size = sizeof(int) * 3 * client_count;

		for (int i = 0; i < client_count; i++)
		{
			int client_socket = -1;
			for (auto& inst : clientList) {
				if (inst->clientindex == selected_clients[i].first) {
					client_socket = inst->socket;
					break;
				}
			}
			memcpy(packet_data + sizeof(int) * (3 * i), &client_socket, sizeof(int));
			memcpy(packet_data + sizeof(int) * (3 * i + 1), &selected_clients[i].first, sizeof(int)); // user_index
			memcpy(packet_data + sizeof(int) * (3 * i + 2), &selected_clients[i].second, sizeof(int)); // user_team
			cout << socket<<":"<<selected_clients[i].first << "의 팀은 " << selected_clients[i].second << endl;
		}
		PacketManger::Send(socket, H_TEAM, packet_data, packet_size);
		cout << socket << "님(" << index << ")은 " << (team == 0 ? "blue" : "red") << "팀입니다." << endl;

	}

	// 픽창 재접속
	else if(curRoom.isGame==0){
		cout<< "픽창 재접속을 시도합니다. " << "code : "<< curRoom.isGame << endl;
		// todo.
		cout << endl;
		cout << " ... 엥 근데 어차피 1분 지나면 나가질텐데 로직 짜야해?" << endl;
	}

	// 게임공간 재접속
	else {
		cout << "게임 공간 재접속을 시도합니다. " << "code : " << curRoom.isGame <<endl;


		
		auto it = client_channel[chan].client_list_room[room].begin();
		while (it != client_channel[chan].client_list_room[room].end()) {
			if ((*it)->clientindex == index && (*it)->socket==-1) {

				clients_info[socket] = (*it);
				clients_info[socket]->socket = socket;
				it = client_channel[chan].client_list_room[room].erase(it);
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

		ReConnection(socket, chan, room);
	}
}

void GameManager::ChampPickTimeOut(int channel, int room) {

	chrono::time_point<chrono::system_clock> currentTime = chrono::system_clock::now();
	chrono::duration<double> elapsed_seconds = currentTime - client_channel[channel].startTime[room];

	while (elapsed_seconds.count() < PICK_TIME) {
		this_thread::sleep_for(chrono::milliseconds(1000));

		// if (client_channel[channel].client_list_room[room].size() != MAX_CLIENT_PER_ROOM) break;

		currentTime = chrono::system_clock::now();
		elapsed_seconds = currentTime - client_channel[channel].startTime[room];

		int curtime = PICK_TIME - elapsed_seconds.count();
		for (auto inst : client_channel[channel].client_list_room[room])
			PacketManger::Send(inst->socket, H_PICK_TIME, &curtime, sizeof(int));

		// cout << "  - room size : "<<client_channel[channel].client_list_room[room].size() << ", count :" << curtime << endl;
	}

	if (AllClientsReady(channel, room)) {
		cout << "모든 사용자들이 Ready 상태입니다. 전투를 시작합니다." << endl;
		// 전투 룸으로 넘어갑니다

		for (auto inst : client_channel[channel].client_list_room[room])
		{
			int client_count = client_channel[channel].client_list_room[room].size();
			BYTE* packet_data = new BYTE[sizeof(int) * 3 * client_count];
			int packet_size = sizeof(int) * 3 * client_count;
			for (int i = 0; i < client_count; i++)
			{
			memcpy(packet_data + sizeof(int) * (3 * i), &inst->socket, sizeof(int));
			memcpy(packet_data + sizeof(int) * (3 * i + 1), &inst->clientindex, sizeof(int)); // user_index
			memcpy(packet_data + sizeof(int) * (3 * i + 2), &inst->team, sizeof(int)); // user_team
			}
			PacketManger::Send(inst->socket, H_BATTLE_START, packet_data, packet_size);
			std::cout << "전투가 시작됩니다" << endl;
			
			for (auto& inst : auth_data) {
				if (inst.channel == channel && inst.room == room) {
					if (inst.isGame == 0)
						inst.isGame = 1;
					else cout << "isGame 오류" << endl;
				}
				
			}

			ClientChampInit(inst->socket);

		}
		// 소켓과 팀 정보를 보내면서 게임 시작을 알린다.
		chrono::time_point<chrono::system_clock>& startTime = client_channel[channel].startTime[room];
		startTime = chrono::system_clock::now();

		for (auto inst : client_channel[channel].client_list_room[room]) // 룸의 클라이언트들을 생성합니다.
		{
			for (auto inst2 : client_channel[channel].client_list_room[room]) // 나 자신을 뺴고 생성해야한다
			{
				if (inst->socket == inst2->socket)
					continue;

				ClientInfo info;
				info.socket = inst->socket;
				info.champindex = inst->champindex;
				PacketManger::Send(inst2->socket, H_NEWBI, &info, sizeof(ClientInfo));
			}

			ClientStat(inst->socket);
		}

		NewStructure(11000, channel, room, 0, 30, 7, 30); //nexsus
		NewStructure(23000, channel, room, 1, 30, 0, -30); //turret
		NewStructure(11300, channel, room, 1, 60, 7, -60); //nexsus
	}
	else {

		list<Client*> dodgeList = client_channel[channel].client_list_room[room];

		MatchResult result;
		result.state = "dodge";

		for (auto& inst : auth_data)
			if (inst.channel == channel && inst.room == room)
				result.spaceId = inst.spaceId;

		for (auto& client : dodgeList)
		{
			if (client == nullptr)
				continue;

			result.loseTeams.push_back(client);
		}
		string json = matchResultToJson(result);

		for (auto& client : dodgeList)
		{
			if (client == nullptr)
				continue;

			ClientClose(client->socket);
		}
		
		cout << " 1분이 경과할 동안 전체 유저들의 픽이 이뤄지지 않았습니다. 종료합니다." << endl;

		auto condition = [channel, room](roomData& roomInfo) {
			return roomInfo.channel == channel && roomInfo.room == room; };
		auth_data.erase(remove_if(auth_data.begin(), auth_data.end(), condition), auth_data.end());

		client_channel[channel].startTime[room] = chrono::time_point<chrono::system_clock>();

		SendToMailslot(json);

		client_channel[channel].client_list_room[room].clear();
		client_channel[channel].structure_list_room[room].clear();
	}

	return;
}

void GameManager::ReConnection(int socket, int chan, int room) {
	//게임중인 녀석들중에서 user_code에 해당하는 녀석이 있다 >> 해당 룸을 동기화

	for (auto inst : client_channel[chan].client_list_room[room]) // 룸의 클라이언트들을 생성합니다.
	{
		if (inst->socket == socket)
			continue;

		ClientInfo info;
		info.socket = inst->socket;
		info.champindex = inst->champindex;
		PacketManger::Send(socket, H_NEWBI, &info, sizeof(ClientInfo));

	}

	for (auto inst : client_channel[chan].client_list_room[room]) // 원래 있던 클라이언트들도 알아야지
	{
		if (inst->socket == socket)
			continue;

		ClientInfo info;
		info.socket = socket;
		info.champindex = inst->champindex;
		PacketManger::Send(inst->socket, H_NEWBI, &info, sizeof(ClientInfo));

	}

	list<Client*> selected_clients = client_channel[chan].client_list_room[room];
	int client_count = selected_clients.size();
	BYTE* packet_data = new BYTE[sizeof(int) * 3 * client_count];
	int packet_size = sizeof(int) * 3 * client_count;
	int index = 0;

	for (auto& client : selected_clients)
	{
		memcpy(packet_data + sizeof(int) * index, &(client->socket), sizeof(int));
		index++;
		memcpy(packet_data + sizeof(int) * index, &(client->clientindex), sizeof(int));
		index++;
		memcpy(packet_data + sizeof(int) * index, &(client->team), sizeof(int));
		index++;
	}

	for (auto& inst : selected_clients)
	{
		PacketManger::Send(inst->socket, H_BATTLE_START, packet_data, packet_size);
		ClientChampReconnectInit(inst->socket);
	}
	for (auto& inst : client_channel[chan].structure_list_room[room])
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

bool GameManager::findEmptyRoom(roomData curRoom) {
	int empty_room = -1, empty_room_channel = -1;

	for (int i = 0; i < MAX_CHANNEL_COUNT; i++) {
		for (int j = 1; j < MAX_ROOM_COUNT_PER_CHANNEL; j++) {
			bool roomFound = false;
			for (auto& room : auth_data) {
				if (room.channel == i && room.room == j) {
					roomFound = true;
					break;
				}
			}
			if (!roomFound && client_channel[i].client_list_room[j].size() == 0) {
				empty_room = j;
				empty_room_channel = i;
				break;
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

	cout << "Ch." << empty_room_channel << " Room #" << empty_room << " Created." << endl;

	for (auto& room : auth_data) {
		if (room.spaceId == curRoom.spaceId) {
			room.channel = empty_room_channel;
			room.room = empty_room;
			break;
		}
	}

	chrono::time_point<chrono::system_clock>& startTime = client_channel[empty_room_channel].startTime[empty_room];
	startTime = chrono::system_clock::now();

	ChampPickTimeOut(empty_room_channel, empty_room);

	return true;
}

string GameManager::matchResultToJson(const MatchResult& result) {
	ostringstream oss;
	oss << "{";
	oss << "\"spaceId\": \"" << result.spaceId << "\",";
	oss << "\"state\": \"" << result.state << "\",";
	oss << "\"winTeamString\": \"" << result.winTeamString << "\",";
	oss << "\"loseTeamString\": \"" << result.loseTeamString << "\",";

	oss << "\"winTeams\": [";
	for (size_t i = 0; i < result.winTeams.size(); ++i) {
		oss << clientToJson(result.winTeams[i]);
		if (i < result.winTeams.size() - 1)
			oss << ",";
	}
	oss << "],";

	oss << "\"loseTeams\": [";
	for (size_t i = 0; i < result.loseTeams.size(); ++i) {
		oss << clientToJson(result.loseTeams[i]);
		if (i < result.loseTeams.size() - 1)
			oss << ",";
	}
	oss << "],";

	oss << "\"dateTime\": \"" << result.dateTime << "\",";
	oss << "\"gameDuration\": " << result.gameDuration;
	oss << "}";
	return oss.str();

}

string GameManager::clientToJson(const Client* client) {
	ostringstream oss;

	oss << "{";
	oss << "\"socket\": " << client->socket << ",";
	oss << "\"champindex\": " << client->champindex << ",";
	oss << "\"user_name\": \"" << client->user_name << "\",";
	oss << "\"out_time\": " << client->out_time << ",";
	oss << "\"channel\": " << client->channel << ",";
	oss << "\"room\": " << client->room << ",";
	oss << "\"code\": \"" << client->code << "\",";
	oss << "\"clientindex\": " << client->clientindex << ",";
	oss << "\"kill\": " << client->kill << ",";
	oss << "\"death\": " << client->death << ",";
	oss << "\"assist\": " << client->assist << ",";
	oss << "\"x\": " << client->x << ",";
	oss << "\"y\": " << client->y << ",";
	oss << "\"z\": " << client->z << ",";
	oss << "\"gold\": " << client->gold << ",";
	oss << "\"rotationX\": " << client->rotationX << ",";
	oss << "\"rotationY\": " << client->rotationY << ",";
	oss << "\"rotationZ\": " << client->rotationZ << ",";
	oss << "\"level\": " << client->level << ",";
	oss << "\"maxexp\": " << client->maxexp << ",";
	oss << "\"exp\": " << client->exp << ",";
	oss << "\"stopped\": " << (client->stopped ? "true" : "false") << ",";
	oss << "\"attacked\": " << (client->attacked ? "true" : "false") << ",";
	oss << "\"curhp\": " << client->curhp << ",";
	oss << "\"maxhp\": " << client->maxhp << ",";
	oss << "\"curmana\": " << client->curmana << ",";
	oss << "\"maxmana\": " << client->maxmana << ",";
	oss << "\"attack\": " << client->attack << ",";
	oss << "\"critical\": " << client->critical << ",";
	oss << "\"criProbability\": " << client->criProbability << ",";
	oss << "\"maxdelay\": " << client->maxdelay << ",";
	oss << "\"curdelay\": " << client->curdelay << ",";
	oss << "\"attrange\": " << client->attrange << ",";
	oss << "\"attspeed\": " << client->attspeed << ",";
	oss << "\"movespeed\": " << client->movespeed << ",";
	oss << "\"growhp\": " << client->growhp << ",";
	oss << "\"growmana\": " << client->growmana << ",";
	oss << "\"growAtt\": " << client->growAtt << ",";
	oss << "\"growCri\": " << client->growCri << ",";
	oss << "\"growCriPro\": " << client->growCriPro << ",";
	oss << "\"team\": " << client->team << ",";
	oss << "\"ready\": " << (client->ready ? "true" : "false") << ",";

	oss << "\"itemList\": [";
	for (size_t i = 0; i < client->itemList.size(); ++i) {
		oss << client->itemList[i];
		if (i < client->itemList.size() - 1)
			oss << ",";
	}
	oss << "]";

	oss << "}";
	return oss.str();
}

