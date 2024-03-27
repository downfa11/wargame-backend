#include "GameManager.h"
#include "IOCP.h"

list<Client*> GameManager::client_list_all; //모든 접속자
vector<Client*> GameManager::client_match;
vector<roomData> GameManager::auth_data;

ClientChannel GameManager::client_channel[MAX_CHANNEL_COUNT]; //100개의 채널
Client* GameManager::clients_info[MAX_CLIENT];

vector<ChampionStats> champions;
vector<itemStats> items;

bool GameManager::exit_connect = false;
int GameManager::timeout_check_time = 0;
chrono::high_resolution_clock::time_point GameManager::lastUpdateTime = chrono::high_resolution_clock::now();

unordered_map<int, atomic<bool>> turretSearchStopMap;

mutex nLock;
mutex roomRead;


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
			if (inst->socket!=-1 && time(NULL) > inst->out_time)
				des_cli.push_back(inst);
		}
		for (auto inst : des_cli)
		{
			if (!inst->isGame){
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

	client_channel[chan].client_list_room[room].push_back(temp_cli);
	cout << "Connect " << client_socket << endl;
}

void GameManager::ClientClose(int client_socket)
{

	Client* client = nullptr;
	for (Client* inst : client_list_all) {
		if (inst->socket == client_socket) {
			client = inst;
			break;
		}
	}

	if (client == nullptr)
		return;


		int chan = client->channel;
		int room = client->room;

		if (!client->isGame) {

			auto condition = [client](roomData& roomInfo) {
				auto it = find_if(roomInfo.user_data.begin(), roomInfo.user_data.end(), [client](auto& inst) {
					return inst.user_code == client->user_code;
					});

				return roomInfo.channel == client->channel &&
					roomInfo.room == client->room &&
					it != roomInfo.user_data.end();
				};

			auth_data.erase(remove_if(auth_data.begin(), auth_data.end(), condition), auth_data.end());


			if (client->channel != 0 || client->room != 0) {

				for (auto& inst : client_channel[chan].client_list_room[room])
				{
					if (inst == nullptr)
						continue;

						closesocket(inst->socket);
					
				}
				client_channel[chan].client_list_room[room].clear();
			}

			client_list_all.remove(client);
			client_channel[chan].client_list_room[room].remove(client);
			delete client;

		}
		else
		{
			clients_info[client->socket] = nullptr;
			client->socket = -1;
		}
		

		auto it = find(client_match.begin(), client_match.end(), client);
		if (it != client_match.end())
			client_match.erase(it);

		for (Client* inst : client_channel[chan].client_list_room[room])
			PacketManger::Send(inst->socket, H_USER_DISCON, &client_socket, sizeof(int));
	

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

int GameManager::UpdateMMR(int MMR_A, int MMR_B, bool A_win) {
	const int K = 16;
	const double EA = 1.0 / (1.0 + pow(10, (MMR_B - MMR_A) / 400.0));
	const double EB = 1.0 / (1.0 + pow(10, (MMR_A - MMR_B) / 400.0));
	// E는 점수차이를 계산한 예상 승률. A가 B보다 400점 높은 경우 이길 확률이 10배 높아진다는 의미.
	int SA = A_win ? 1 : 0;
	int SB = A_win ? 0 : 1;
	int new_MMR_A = MMR_A + K * (SA - EA);
	int new_MMR_B = MMR_B + K * (SB - EB);
	return new_MMR_A;
}

int GameManager::get_opposing_team_MMR(list<Client*>& clients_in_room) {
	int sum_MMR = 0;
	int opposing_team_size = 0;
	for (auto inst : clients_in_room) {
		if (inst->team != 1) {
			sum_MMR += inst->elo;
			opposing_team_size++;
		}
	}
	if (opposing_team_size == 0) {
		return 0;
	}
	return sum_MMR / opposing_team_size;
}

void GameManager::Assign_teams() {
	if (client_match.size() < MAX_CLIENT_PER_ROOM)
		return;
	/* 무작위로 10명 선택
	srand((unsigned int)time(NULL));
	shuffle(client_ready.begin(), client_ready.end(), std::default_random_engine{ std::random_device{}() });
	vector<Client*> selected_clients;
	for (int i = 0; i < MAX_CLIENT_PER_ROOM; i++)
	{
		if (client_ready[i]->team == -1)
			selected_clients.push_back(client_ready[i]);
	}*/
	// elo 

	cout << "client_match.size = "<<client_match.size() << endl;

	sort(client_match.begin(), client_match.end(), [](const Client* a, const Client* b) {
		return a->elo > b->elo;
		});
	vector<Client*> selected_clients;
	for (auto it = client_match.begin(); it != client_match.end();)
	{
		if ((*it)->team == -1)
		{
			selected_clients.push_back(*it);
			it = client_match.erase(it);
		}
		else
			++it;
		if (selected_clients.size() == MAX_CLIENT_PER_ROOM)
			break;
	}

	int empty_room = -1, empty_room_channel = -1;

	for (int i = 0; i < MAX_CHANNEL_COUNT; i++) {
		for (int j = 0; j < MAX_ROOM_COUNT_PER_CHANNEL; j++) {

			for (auto& room : auth_data)
				if (room.channel == i && room.room == j)
					continue;

			if (client_channel[i].client_list_room[j].size() == 0) {
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
		return;
	}
	// 무작위로 blue팀과 red팀 선택
	shuffle(selected_clients.begin(), selected_clients.end(), default_random_engine{ random_device{}() });
	for (int i = 0; i < MAX_CLIENT_PER_ROOM; i++)
	{
		selected_clients[i]->team = i < MAX_TEAM_PER_ROOM ? 0 : 1;
		// blue팀은 0, red팀은 1로 설정
		// i가 0~4인 경우 blue팀, 5~9인 경우 red팀
	}
	cout << "Ch." << empty_room_channel << " Room #" << empty_room << " Created." << endl;
	for (auto inst : selected_clients)
	{
		if (empty_room_channel == 0)
			ClientRoomMove(inst->socket, (void*)&empty_room);
		else {
			ClientChanMove(inst->socket, (void*)&empty_room_channel);
			ClientRoomMove(inst->socket, (void*)&empty_room);
		}
	}

	for (auto inst : selected_clients)
	{

		for (auto& room : auth_data) {
			for (auto& inst__ : room.user_data) {
				if (inst__.user_code == inst->user_code)
				{
					room.channel = empty_room_channel;
					room.room = empty_room;

				}
			}
		}

		int client_count = selected_clients.size();
		BYTE* packet_data = new BYTE[sizeof(int) * 2 * client_count];
		int packet_size = sizeof(int) * 2 * client_count;
		for (int i = 0; i < client_count; i++)
		{
			memcpy(packet_data + sizeof(int) * (2 * i), &selected_clients[i]->socket, sizeof(int));
			memcpy(packet_data + sizeof(int) * (2 * i + 1), &selected_clients[i]->team, sizeof(int));
		}
		PacketManger::Send(inst->socket, H_TEAM, packet_data, packet_size);
		std::cout << inst->socket << "님은 " << inst->team << "팀입니다." << endl;


	}

	cout << "해당하는 roominfo에 " << empty_room_channel << "채널의 " << empty_room << "번 룸을 등록했습니다." << endl;

	// 소켓과 팀 정보를 보내면서 게임 픽창 시작 시작을 알린다
	chrono::time_point<chrono::system_clock>& startTime = client_channel[empty_room_channel].startTime[empty_room];
	startTime = chrono::system_clock::now();

	ChampPickTimeOut(PICK_TIME, empty_room_channel, empty_room);

}

void GameManager::ClientMatch(int client_socket)
{
	for (auto inst : client_channel[0].client_list_room[0])
	{
		if (inst->socket == client_socket)
		{

			client_match.push_back(inst);
			std::cout << "match to " << inst->socket << endl;
			break;
		}
	}
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
				cout << "ready to " << inst->socket << endl;
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
	auto list = client_channel[channel].structure_list_room[room];
	for (auto inst : list)
	{

		StopTurretSearch(inst->index);
		client_channel[channel].structure_list_room[room].remove(inst);
	}

	auto condition = [channel, room](roomData& roomInfo) {  //게임이 끝나면 auth_data 를 지웁니다.
		return roomInfo.channel == channel && roomInfo.room == room; };
	auth_data.erase(std::remove_if(auth_data.begin(), auth_data.end(), condition), auth_data.end());

	int win = (clients_info[client_socket]->team == winTeam) ? 1 : 0;
	BYTE* packet_data = new BYTE[sizeof(int) + sizeof(int)];
	memcpy(packet_data, &client_socket, sizeof(int));
	memcpy(packet_data + sizeof(int), &win, sizeof(int));

	vector<Client> clientsToMove;
	cout << endl;
	for (auto inst : client_channel[channel].client_list_room[room])
	{



		clientsToMove.push_back(*inst);
		PacketManger::Send(inst->socket, H_VICTORY, packet_data, sizeof(int) + sizeof(int));
		int new_MMR = UpdateMMR(inst->elo, get_opposing_team_MMR(client_channel[channel].client_list_room[room]), (inst->team == winTeam));

		cout << inst->socket << ": " << inst->elo << " -> " << new_MMR << endl;
		inst->elo = new_MMR;


		inst->champindex = -1;
		inst->ready = false;
		inst->curhp = inst->maxhp;
		inst->team = -1;
		inst->level = 1;
		inst->exp = 0;
		inst->maxexp = 100;

		inst->curhp = 0;
		inst->maxhp = 0;
		inst->attack = 0;
		inst->maxdelay = 0;
		inst->curdelay = 0;
		inst->attrange = 0;
		inst->attspeed = 0;
		inst->movespeed = 0;
		inst->x = 0;
		inst->y = 0;
		inst->z = 0;
		inst->gold = 0;
		inst->kill = 0;
		inst->assist = 0;
		inst->death = 0;

		inst->itemList.clear();

		while (!inst->assistList.empty())
			inst->assistList.pop();

		if (!inst->assistList.empty())
			cout << "assisList is not empty." << endl;
		// 이때 여기서 다시 로비로 돌아가기문에, 게임안의 정보들을 모두 초기화해줘야한다.
	}
	delete[] packet_data;

	time_t now = time(nullptr);
	tm currentTime;
	localtime_s(&currentTime, &now);

	char dateTimeFormat[] = "%Y-%m-%d %H:%M:%S";
	char buffer[100];
	strftime(buffer, sizeof(buffer), dateTimeFormat, &currentTime);

	MatchResult result;
	result.datetime = buffer;
	result.winTeam = winTeam;
	for (const Client& cli : clientsToMove)
		result.participants.push_back(cli);

	chrono::time_point<chrono::system_clock> endTime = chrono::system_clock::now();
	chrono::time_point<chrono::system_clock>& startTime = client_channel[channel].startTime[room];

	chrono::duration<double> elapsed_seconds = endTime - startTime;
	int gameMinutes = static_cast<int>(elapsed_seconds.count() / 60);

	result.gameDuration = gameMinutes;

	SaveMatchResult(result);

	startTime = chrono::time_point<chrono::system_clock>(); //init
}

string SerializeMatchResult(const MatchResult& result, int matchId) {
	string jsonStr = "{";

	jsonStr += "\"datetime\":\"" + result.datetime + "\",";
	jsonStr += "\"winTeam\":" + to_string(result.winTeam) + ",";
	jsonStr += "\"participants\":[";

	bool firstParticipant = true;
	for (auto participant : result.participants) {
		if (!firstParticipant)
			jsonStr += ",";
		else
			firstParticipant = false;

		jsonStr += "{";

		jsonStr += "\"match_id\":" + to_string(matchId) + ",";
		jsonStr += "\"socket\":" + to_string(participant.socket) + ",";
		jsonStr += "\"champindex\":" + to_string(participant.champindex) + ",";
		jsonStr += "\"channel\":" + to_string(participant.channel) + ",";
		jsonStr += "\"room\":" + to_string(participant.room) + ",";
		jsonStr += "\"elo\":" + to_string(participant.elo) + ",";
		jsonStr += "\"level\":" + to_string(participant.level) + ",";
		jsonStr += "\"curhp\":" + to_string(participant.curhp) + ",";
		jsonStr += "\"maxhp\":" + to_string(participant.maxhp) + ",";
		jsonStr += "\"attack\":" + to_string(participant.attack) + ",";
		jsonStr += "\"critical\":" + to_string(participant.critical) + ",";
		jsonStr += "\"criProbability\":" + to_string(participant.criProbability) + ",";
		jsonStr += "\"maxdelay\:" + to_string(participant.maxdelay) + ",";
		jsonStr += "\"curdelay\":" + to_string(participant.curdelay) + ",";
		jsonStr += "\"attrange\":" + to_string(participant.attrange) + ",";
		jsonStr += "\"attspeed\":" + to_string(participant.attspeed) + ",";
		jsonStr += "\"movespeed\":" + to_string(participant.movespeed) + ",";

		string team = participant.team == 0 ? "Win" : "Lose";
		jsonStr += "\"team\":\"" + team + "\"";


		jsonStr.pop_back();
		jsonStr += "}";
	}

	jsonStr += "}";

	return jsonStr;
}

void GameManager::SaveMatchResult(const MatchResult& result) {
	MYSQL mysql;
	int matchId = 0;
	mysql_init(&mysql);
	// if (mysql_real_connect(&mysql, "your-rds-endpoint", "your-rds-username", "your-rds-password", "your-rds-database", 3306, NULL, 0))
	if (mysql_real_connect(&mysql, "127.0.0.1", "root", "namsuck1!", "wargame", 3307, NULL, 0))
	{
		string query = "INSERT INTO match_results (datetime, win_team, game_duration) VALUES ('" +
			result.datetime + "', " + to_string(result.winTeam) + ", " + to_string(result.gameDuration) + ")";

		if (mysql_query(&mysql, query.c_str()) != 0) {
			cerr << "Failed to execute query: " << mysql_error(&mysql) << endl;
		}
		else {
			matchId = mysql_insert_id(&mysql);

			for (const auto& participant : result.participants) {
				string teamStr = participant.team == 0 ? "Win" : "Lose";

				query = "INSERT INTO participants (match_id, socket, champindex, channel, room, elo, level, curhp, maxhp, attack, critical, criProbability, maxdelay, curdelay, attrange, attspeed, movespeed, team) VALUES ("
					+ to_string(matchId) + ", '" + to_string(participant.socket) + "', " + to_string(participant.champindex) + ", " + to_string(participant.channel) + ", '"
					+ to_string(participant.room) + "', " + to_string(participant.elo) + ", " + to_string(participant.level) + ", "
					+ to_string(participant.curhp) + ", " + to_string(participant.maxhp) + ", " + to_string(participant.attack) + ", "
					+ to_string(participant.critical) + ", " + to_string(participant.criProbability) + ", " + to_string(participant.maxdelay) + ", "
					+ to_string(participant.curdelay) + ", " + to_string(participant.attrange) + ", " + to_string(participant.attspeed) + ", "
					+ to_string(participant.movespeed) + ", '" + teamStr + "')";

				if (mysql_query(&mysql, query.c_str()) != 0) {
					cerr << "Failed to execute query: " << mysql_error(&mysql) << endl;
				}
			}

			cout << "Match result saved MySQL DB successfully." << std::endl;
		}

		mysql_close(&mysql);
	}
	else
		cerr << "mysql error: " << mysql_error(&mysql) << std::endl;

	string data = SerializeMatchResult(result, matchId);

	// Lobby::SendToWebServer("/save_match_result", data, data.length());
}

void GameManager::GotoLobby(int client_socket) {

	if (clients_info[client_socket] == nullptr)
		return;

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;

	vector<int> clientsToMove;

	for (auto inst : client_channel[chan].client_list_room[room])
	{
		inst->isGame = false;
		clientsToMove.push_back(inst->socket);
	}

	for (auto client_socket : clientsToMove) {
		closesocket(client_socket);
		cout << "로비로 보내야죠. 그냥.. 접속 끊고 로비로 보내면 되는거 아니뇨?" << endl;

	}

	client_channel[chan].client_list_room[room].clear(); // 방 클리어
	client_channel[chan].structure_list_room[room].clear();
}

void GameManager::LoadChampions() {
	MYSQL mysql;
	MYSQL_RES* result;
	MYSQL_ROW row;

	mysql_init(&mysql);
	if (mysql_real_connect(&mysql, "127.0.0.1", "root", "namsuck1!", "wargame", 3307, NULL, 0)) {
		const char* query = "SELECT * FROM champion_stats";
		if (mysql_query(&mysql, query) == 0) {
			result = mysql_store_result(&mysql);
			if (result) {
				while ((row = mysql_fetch_row(result))) {
					ChampionStats champion;

					champion.index = stoi(row[0]);
					champion.name = row[1];
					champion.maxhp = stoi(row[2]);
					champion.maxmana = stoi(row[3]);
					champion.attack = stoi(row[4]);
					champion.movespeed = stof(row[5]);
					champion.maxdelay = stof(row[6]);
					champion.attspeed = stof(row[7]);
					champion.attrange = stoi(row[8]);
					champion.critical = stoi(row[9]);
					champion.criProbability = stoi(row[10]);
					champion.growHp = stoi(row[11]);
					champion.growMana = stoi(row[12]);
					champion.growAtt = stoi(row[13]);
					champion.growCri = stoi(row[14]);
					champion.growCriPob = stoi(row[15]);
					champions.push_back(champion);
				}
				mysql_free_result(result);
			}
		}
		else {
			cerr << "Failed to execute query: " << mysql_error(&mysql) << endl;
		}
		mysql_close(&mysql);
	}
	else {
		cerr << "mysql error: " << mysql_error(&mysql) << endl;
	}

	cout << "Champs init." << endl;

}

void GameManager::ClientStat(int client_socket) {

	if (clients_info[client_socket] == nullptr)
		return;


	ClientInfo info;
	info.socket = client_socket;
	info.champindex = clients_info[client_socket]->champindex;
	info.gold = clients_info[client_socket]->gold;
	info.x = clients_info[client_socket]->x;
	info.y = clients_info[client_socket]->y;
	info.z = clients_info[client_socket]->z;
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

	for (auto& champion : champions) {
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

void GameManager::MouseSearch(int client_socket, void* data)
{

	mouseInfo info;
	memcpy(&info, data, sizeof(mouseInfo));

	int chan = clients_info[client_socket]->channel;
	int room = clients_info[client_socket]->room;
	int team = clients_info[client_socket]->team;

	float minDistance = FLT_MAX * 2;
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

				while ((*attacked)->assistList.size() >= MAX_TEAM_PER_ROOM) {
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

	if (clients_info[client_socket]->assistList.size() >= MAX_TEAM_PER_ROOM)
	{
		stack<pair<int, float>> tempStack = clients_info[client_socket]->assistList;

		// 스택이 MAX_TEAM_PER_ROOM 크기보다 크면 맨 아래 값을 제거
		vector<int> assistTargets;
		while (tempStack.size() > MAX_TEAM_PER_ROOM) {
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

void GameManager::ItemInit() {
	MYSQL mysql;
	MYSQL_RES* result;
	MYSQL_ROW row;

	mysql_init(&mysql);
	if (mysql_real_connect(&mysql, "127.0.0.1", "root", "namsuck1!", "wargame", 3307, NULL, 0)) {
		const char* query = "SELECT * FROM item_stats";
		if (mysql_query(&mysql, query) == 0) {
			result = mysql_store_result(&mysql);
			if (result) {
				while ((row = mysql_fetch_row(result))) {
					itemStats item;

					item.id = std::stoi(row[0]);
					item.name = row[1];
					item.gold = std::stoi(row[2]);
					item.maxhp = std::stoi(row[3]);
					item.attack = std::stoi(row[4]);
					item.movespeed = std::stoi(row[5]);
					item.maxdelay = std::stoi(row[6]);
					item.attspeed = std::stoi(row[7]);
					item.criProbability = std::stoi(row[8]);

					// critical은 150%
					items.push_back(item);
				}
				mysql_free_result(result);
			}
		}
		else {
			cerr << "Failed to execute query: " << mysql_error(&mysql) << endl;
		}
		mysql_close(&mysql);
	}
	else {
		cerr << "mysql error: " << mysql_error(&mysql) << endl;
	}

	cout << "item init." << endl;

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

	for (auto& item : items) {
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
	else index = clients_info[client_socket]->itemList.size()-1;

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

				clients_info[client_socket]->itemList[i]=0; // 아이템 삭제
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

//  level 마다 지정된 maxexp (전체 공통)
//  성장 능력치 상승치 grow* (챔프 개인)

void GameManager::ClientAuth(int socket, void* data) {

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


			for (auto& roominfo : auth_data) {
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
		}
	}
	else
	{
		cout << socket << "는 웹서버 비인가 사용자입니다." << endl;
		closesocket(socket);
	}

}

bool GameManager::isExistClientCode(string user_code) {

	for (auto& roomInfo : auth_data) {
		for (auto& userData : roomInfo.user_data) {
			if (userData.user_code == user_code)
				return true;
		}
	}
	return false;
}

void GameManager::ChampPickTimeOut(int timemin, int channel, int room) {

	chrono::time_point<chrono::system_clock> currentTime = chrono::system_clock::now();
	chrono::duration<double> elapsed_seconds = currentTime - client_channel[channel].startTime[room];


	while (elapsed_seconds.count() < timemin) {
		this_thread::sleep_for(chrono::milliseconds(2000));

		if (client_channel[channel].client_list_room[room].size() != MAX_CLIENT_PER_ROOM)
			break;

		currentTime = chrono::system_clock::now();
		elapsed_seconds = currentTime - client_channel[channel].startTime[room];

		// cout << "room size : "<<client_channel[channel].client_list_room[room].size() << ", count :" << elapsed_seconds.count() << endl;
	}

	if (AllClientsReady(channel, room)) {
		cout << "모든 사용자들이 Ready 상태입니다. 전투를 시작합니다." << endl;
		// 전투 룸으로 넘어갑니다

		for (auto inst : client_channel[channel].client_list_room[room])
		{
			int client_count = client_channel[channel].client_list_room[room].size();
			BYTE* packet_data = new BYTE[sizeof(int) * 2 * client_count];
			int packet_size = sizeof(int) * 2 * client_count;
			for (int i = 0; i < client_count; i++)
			{
				memcpy(packet_data + sizeof(int) * (2 * i), &inst->socket, sizeof(int));
				memcpy(packet_data + sizeof(int) * (2 * i + 1), &inst->team, sizeof(int));
			}
			PacketManger::Send(inst->socket, H_BATTLE_START, packet_data, packet_size);
			std::cout << "전투가 시작됩니다" << endl;
			inst->isGame = true;

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

		for (auto& client : client_channel[channel].client_list_room[room])
		{
			if (client == nullptr)
				continue;

			closesocket(client->socket);

		}
		cout << " 1분이 경과할 동안 전체 유저들의 픽이 이뤄지지 않았습니다. 종료합니다." << endl;
	}

	return;
}

void GameManager::ReConnection(Client* client,int socket, int chan,int room) {
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
	BYTE* packet_data = new BYTE[sizeof(int) * 2 * client_count];
	int packet_size = sizeof(int) * 2 * client_count;
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
		ClientChampInit(inst->socket);
		ClientStat(inst->socket);
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