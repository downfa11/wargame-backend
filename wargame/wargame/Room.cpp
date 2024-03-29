#include "Room.h"

mutex mutex_client;
mutex mutex_structure;
ChatLog Room::chat_log = vector<ChatEntry>();

list<Client*> Room::client_list_room;
list<structure*> Room::structure_list_room;
Client* Room::clients_room_info[MAX_CLIENT];

chrono::time_point<chrono::system_clock> Room::startTime;
chrono::high_resolution_clock::time_point Room::lastUpdateTime = chrono::high_resolution_clock::now();

unordered_map<int, atomic<bool>> turretSearchStopMap;


using namespace std;

void Room::ClientChampInit(int client_socket) {
	ClientInfo info;

	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;

	int champIndex = clients_room_info[client_socket]->champindex;
	ChampionStats* champ = nullptr;

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

	clients_room_info[client_socket]->level = 1;
	clients_room_info[client_socket]->curhp = (*champ).maxhp;
	clients_room_info[client_socket]->maxhp = (*champ).maxhp;
	clients_room_info[client_socket]->curmana = (*champ).maxmana;
	clients_room_info[client_socket]->maxmana = (*champ).maxmana;
	clients_room_info[client_socket]->attack = (*champ).attack;
	clients_room_info[client_socket]->maxdelay = (*champ).maxdelay;
	clients_room_info[client_socket]->attrange = (*champ).attrange;
	clients_room_info[client_socket]->attspeed = (*champ).attspeed;
	clients_room_info[client_socket]->movespeed = (*champ).movespeed;
	clients_room_info[client_socket]->critical = (*champ).critical;
	clients_room_info[client_socket]->criProbability = (*champ).criProbability;

	clients_room_info[client_socket]->growhp = (*champ).growHp;
	clients_room_info[client_socket]->growmana = (*champ).growMana;
	clients_room_info[client_socket]->growAtt = (*champ).growAtt;
	clients_room_info[client_socket]->growCri = (*champ).growCri;
	clients_room_info[client_socket]->growCriPro = (*champ).growCriPob;

	info.socket = client_socket;
	info.champindex = clients_room_info[client_socket]->champindex;
	info.gold = clients_room_info[client_socket]->gold;
	info.level = clients_room_info[client_socket]->level;
	info.curhp = clients_room_info[client_socket]->curhp;
	info.maxhp = clients_room_info[client_socket]->maxhp;
	info.curmana = clients_room_info[client_socket]->curmana;
	info.maxmana = clients_room_info[client_socket]->maxmana;
	info.attack = clients_room_info[client_socket]->attack;
	info.critical = clients_room_info[client_socket]->critical;
	info.criProbability = clients_room_info[client_socket]->criProbability;
	info.attspeed = clients_room_info[client_socket]->attspeed;
	info.attrange = clients_room_info[client_socket]->attrange;
	info.movespeed = clients_room_info[client_socket]->movespeed;

	itemSlots slots;


	slots.id_0 = clients_room_info[client_socket]->itemList[0];
	slots.id_1 = clients_room_info[client_socket]->itemList[1];
	slots.id_2 = clients_room_info[client_socket]->itemList[2];
	slots.id_3 = clients_room_info[client_socket]->itemList[3];
	slots.id_4 = clients_room_info[client_socket]->itemList[4];
	slots.id_5 = clients_room_info[client_socket]->itemList[5];

	for (auto inst : client_list_room)
	{
		PacketManger::Send(inst->socket, H_CHAMPION_INIT, &info, sizeof(ClientInfo));
		PacketManger::Send(inst->socket, H_ITEM_STAT, &slots, sizeof(itemSlots));
	}
}

void Room::ClientClose(int client_socket)
{

	Client* client = nullptr;
	for (Client* inst : client_list_room) {
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

		//auth_data.erase(remove_if(auth_data.begin(), auth_data.end(), condition), auth_data.end());
		{
			if (client->channel != 0 || client->room != 0) {

				for (auto& inst : client_list_room)
				{
					if (inst == nullptr)
						continue;

					closesocket(inst->socket);

				}
				client_list_room.clear();
			}

			client_list_room.remove(client);
			delete client;
		}
	}
	else
	{
		clients_room_info[client->socket] = nullptr;
		client->socket = -1;
	}


	//auto it = find(client_match.begin(), client_match.end(), client);
	//if (it != client_match.end())
	//	client_match.erase(it);
	lock_guard<std::mutex> lock(mutex_client);
	for (Client* inst : client_list_room)
		PacketManger::Send(inst->socket, H_USER_DISCON, &client_socket, sizeof(int));


	cout << "Disconnected " << client_socket << endl;

}
void Room::ClientMoveStart(int client_socket, void* data)
{
	ClientMovestart info;
	memcpy(&info, data, sizeof(ClientMovestart));

	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;

	clients_room_info[client_socket]->rotationX = info.rotationX;
	clients_room_info[client_socket]->rotationY = info.rotationY;
	clients_room_info[client_socket]->rotationZ = info.rotationZ;
	clients_room_info[client_socket]->stopped = false;

	lock_guard<std::mutex> lock(mutex_client);
	for (auto inst : client_list_room)
	{
		if (inst->socket != client_socket)
			PacketManger::Send(inst->socket, H_MOVESTART, &info, sizeof(ClientMovestart));
	}
}

void Room::ClientMove(int client_socket, void* data)
{
	ClientInfo info;
	memcpy(&info, data, sizeof(ClientInfo));

	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;

	clients_room_info[client_socket]->x = info.x;
	clients_room_info[client_socket]->y = info.y;
	clients_room_info[client_socket]->z = info.z;

	lock_guard<std::mutex> lock(mutex_client);
	for (auto inst : client_list_room)
	{
		if (inst->socket != client_socket)
			PacketManger::Send(inst->socket, H_MOVE, &info, sizeof(ClientInfo));
	}
}

void Room::ClientMoveStop(int client_socket, void* data)
{
	//움직인다
	ClientInfo info;
	memcpy(&info, data, sizeof(ClientInfo));

	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;

	clients_room_info[client_socket]->x = info.x;
	clients_room_info[client_socket]->y = info.y;
	clients_room_info[client_socket]->z = info.z;
	clients_room_info[client_socket]->stopped = true;

	lock_guard<std::mutex> lock(mutex_client);
	for (auto inst : client_list_room)
	{
		if (inst->socket != client_socket)
			PacketManger::Send(inst->socket, H_MOVESTOP, &info, sizeof(ClientInfo));
	}
}

void Room::ClientChat(int client_socket, int size, void* data)
{
	BYTE* packet_data = new BYTE[size + sizeof(int)];
	memcpy(packet_data, &client_socket, sizeof(int));
	memcpy(&packet_data[sizeof(int)], data, size);

	Client* sender = clients_room_info[client_socket];
	int chan = sender->channel;
	int room = sender->room;

	if (chan < 0 || chan >= 100 || room < 0 || room >= chat_log.size()) {
		cout << "Invalid channel or room index." << endl;
		delete[] packet_data;
		return;
	}
	vector<ChatEntry> chatLog = chat_log;

	auto curtime = chrono::system_clock::now();
	if (chatLog.size() == 0)
		chatLog.push_back({ "host", {curtime, "Game Start - 게임 시작을 알립니다."} });

	string chat(reinterpret_cast<char*>(&packet_data[sizeof(int)]), size);

	chatLog.push_back({ sender->user_name, {curtime, chat} });
	chat_log = chatLog;

	lock_guard<std::mutex> lock(mutex_client);
	for (auto cli : client_list_room)
		PacketManger::Send(cli->socket, H_CHAT, packet_data, size + sizeof(int));
	
	delete[] packet_data;
}

bool Room::AllClientsReady(int channel, int room) {

	if (client_list_room.size() != MAX_CLIENT_PER_ROOM)
		return false;

	lock_guard<std::mutex> lock(mutex_client);
	for (auto inst : client_list_room) {
		if (!inst->ready)
			return false;
	}

	return true;
}

void Room::SendVictory(int client_socket, int winTeam, int channel, int room)
{
	auto list = structure_list_room;
	for (auto inst : list)
	{

		StopTurretSearch(inst->index);
		structure_list_room.remove(inst);
	}

	auto condition = [channel, room](roomData& roomInfo) {  //게임이 끝나면 auth_data 를 지웁니다.
		return roomInfo.channel == channel && roomInfo.room == room; };
	
	//auth_data.erase(std::remove_if(auth_data.begin(), auth_data.end(), condition), auth_data.end());

	int win = (clients_room_info[client_socket]->team == winTeam) ? 1 : 0;
	BYTE* packet_data = new BYTE[sizeof(int) + sizeof(int)];
	memcpy(packet_data, &client_socket, sizeof(int));
	memcpy(packet_data + sizeof(int), &win, sizeof(int));

	vector<Client> clientsToMove;
	cout << endl;

	lock_guard<std::mutex> lock(mutex_client);
	for (auto inst : client_list_room)
	{



		clientsToMove.push_back(*inst);
		PacketManger::Send(inst->socket, H_VICTORY, packet_data, sizeof(int) + sizeof(int));
		//int new_MMR = UpdateMMR(inst->elo, get_opposing_team_MMR(client_list_room), (inst->team == winTeam));

		cout << inst->socket << ": " << inst->elo << " -> " << "2000" << endl;
		inst->elo = 2000;  //Todo


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
	chrono::duration<double> elapsed_seconds = endTime - startTime;
	int gameMinutes = static_cast<int>(elapsed_seconds.count() / 60);

	result.gameDuration = gameMinutes;

	SaveMatchResult(result);

	startTime = chrono::time_point<chrono::system_clock>(); //init
}

double Distance(double x1, double y1, double z1, double x2, double y2, double z2)
{
	double dx = x1 - x2;
	double dy = y1 - y2;
	double dz = z1 - z2;

	return sqrt(dx * dx + dy * dy + dz * dz);
}

void Room::SaveMatchResult(const MatchResult& result) //Todo 
{ 
	string message = result.toString();
	kafkaIPC::KafkaSend(kafkaIPC::resultTopic, message);

}

void Room::GotoLobby(int client_socket) {

	if (clients_room_info[client_socket] == nullptr)
		return;

	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;

	vector<int> clientsToMove;

	lock_guard<std::mutex> lock(mutex_client);
	for (auto inst : client_list_room)
	{
		inst->isGame = false;
		clientsToMove.push_back(inst->socket);
	}

	for (auto client_socket : clientsToMove) {
		closesocket(client_socket);
		cout << "로비로 보내야죠. 그냥.. 접속 끊고 로비로 보내면 되는거 아니뇨?" << endl;

	}

	lock_guard<std::mutex> lock(mutex_client);
	client_list_room.clear();

	structure_list_room.clear();
}

void Room::ClientStat(int client_socket) {

	if (clients_room_info[client_socket] == nullptr)
		return;


	ClientInfo info;
	info.socket = client_socket;
	info.champindex = clients_room_info[client_socket]->champindex;
	info.gold = clients_room_info[client_socket]->gold;
	info.x = clients_room_info[client_socket]->x;
	info.y = clients_room_info[client_socket]->y;
	info.z = clients_room_info[client_socket]->z;
	info.kill = clients_room_info[client_socket]->kill;
	info.death = clients_room_info[client_socket]->death;
	info.assist = clients_room_info[client_socket]->assist;
	info.level = clients_room_info[client_socket]->level;
	info.curhp = clients_room_info[client_socket]->curhp;
	info.maxhp = clients_room_info[client_socket]->maxhp;
	info.curmana = clients_room_info[client_socket]->curmana;
	info.maxmana = clients_room_info[client_socket]->maxmana;
	info.attack = clients_room_info[client_socket]->attack;
	info.critical = clients_room_info[client_socket]->critical;
	info.criProbability = clients_room_info[client_socket]->criProbability;
	info.attrange = clients_room_info[client_socket]->attrange;
	info.attspeed = clients_room_info[client_socket]->attspeed;
	info.movespeed = clients_room_info[client_socket]->movespeed;

	itemSlots slots;
	slots.socket = client_socket;
	size_t itemCount = clients_room_info[client_socket]->itemList.size();
	size_t maxItemCount = min(itemCount, size_t(6));
	for (size_t i = 0; i < maxItemCount; ++i) {
		if (i == 0) slots.id_0 = clients_room_info[client_socket]->itemList[i];
		if (i == 1) slots.id_1 = clients_room_info[client_socket]->itemList[i];
		if (i == 2) slots.id_2 = clients_room_info[client_socket]->itemList[i];
		if (i == 3) slots.id_3 = clients_room_info[client_socket]->itemList[i];
		if (i == 4) slots.id_4 = clients_room_info[client_socket]->itemList[i];
		if (i == 5) slots.id_5 = clients_room_info[client_socket]->itemList[i];
	}
	for (size_t i = maxItemCount; i < 6; ++i) {
		if (i == 0) slots.id_0 = 0;
		if (i == 1) slots.id_1 = 0;
		if (i == 2) slots.id_2 = 0;
		if (i == 3) slots.id_3 = 0;
		if (i == 4) slots.id_4 = 0;
		if (i == 5) slots.id_5 = 0;
	}

	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;

	// lock_guard<std::mutex> lock(mutex_client);
	for (auto inst : client_list_room)
	{
		PacketManger::Send(inst->socket, H_CLIENT_STAT, &info, sizeof(ClientInfo));
		PacketManger::Send(inst->socket, H_ITEM_STAT, &slots, sizeof(itemSlots));
	}
}

void Room::MouseSearch(int client_socket, void* data)
{

	mouseInfo info;
	memcpy(&info, data, sizeof(mouseInfo));

	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;
	int team = clients_room_info[client_socket]->team;

	double minDistance = FLT_MAX;
	Client* closestClient = nullptr;
	structure* closestStruct = nullptr;

	for (auto inst : structure_list_room)
	{
		if (team != inst->team)
		{
			double distance = Distance(info.x, info.y, info.z, inst->x, inst->y, inst->z);
			if (distance < minDistance)
			{
				closestStruct = inst;
				minDistance = distance;
				closestClient = nullptr;
			}
		}
	}

	for (auto inst : client_list_room)
	{
		if (inst->socket != client_socket && team != inst->team)
		{
			double distance = Distance(info.x, info.y, info.z, inst->x, inst->y, inst->z);
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

		for (auto inst : client_list_room)
		{
			if (inst->socket == client_socket)
			{
				for (auto struc : structure_list_room)
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
		for (auto inst : client_list_room)
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

void Room::AttackClient(int client_socket, void* data)
{
	attinfo info;
	memcpy(&info, data, sizeof(attinfo));
	int attackedsocket = info.attacked;

	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;

	list<Client*> clients_in_room = client_list_room;

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

	double distance = Distance((*attacker)->x, (*attacker)->y, (*attacker)->z, (*attacked)->x, (*attacked)->y, (*attacked)->z);
	if (distance <= (*attacker)->attrange)
	{

		if (!clients_room_info[client_socket]->stopped)
		{

			int i = 0;
			PacketManger::Send(client_socket, H_CLIENT_STOP, &i, sizeof(int));
			clients_room_info[client_socket]->stopped = true;
		}


		if (clients_room_info[client_socket]->curdelay >= clients_room_info[client_socket]->maxdelay) {
			if (!clients_room_info[client_socket]->attacked) {

				for (auto inst : client_list_room)
				{
					BYTE* packet_data = new BYTE[sizeof(int) + sizeof(int)];
					memcpy(packet_data, &((*attacker)->socket), sizeof(int));
					memcpy(packet_data + sizeof(int), &((*attacked)->socket), sizeof(int));
					PacketManger::Send(inst->socket, H_ATTACK_CLIENT, packet_data, sizeof(int) + sizeof(int));
				}
				clients_room_info[client_socket]->attacked = true;
			}

			if (clients_room_info[client_socket]->curdelay >= clients_room_info[client_socket]->maxdelay + clients_room_info[client_socket]->attspeed) {

				random_device rd;
				mt19937 gen(rd());
				uniform_real_distribution<float> dis(0, 1); //균일한 분포;를 형성
				double chance = (*attacker)->criProbability / 100.0;
				double cridmg = (*attacker)->critical / 100.0;
				int att = (*attacker)->attack;

				if (dis(gen) < chance)
				{
					double critDamage = (*attacker)->attack * cridmg;
					att = (*attacker)->attack + (int)critDamage;
				}

				//cout << (*attacker)->socket << " 의 공격 :: " << (*attacked)->socket << " " << clients_room_info[client_socket]->curdelay << " " << endl;;
				(*attacked)->curhp -= att;
				clients_room_info[client_socket]->curdelay = 0;
				clients_room_info[client_socket]->attacked = false;
				chrono::time_point<chrono::system_clock> currentTime = chrono::system_clock::now();
				chrono::duration<double> newTime = currentTime - startTime;
				

				while ((*attacked)->assistList.size() >= MAX_TEAM_PER_ROOM) {
					(*attacked)->assistList.pop();
				}

				while (!(*attacked)->assistList.empty() && (*attacked)->assistList.top().first == clients_room_info[client_socket]->socket) {
					(*attacked)->assistList.pop();
				}

				while (!(*attacked)->assistList.empty() && newTime.count() - (*attacked)->assistList.top().second > 30) {
					(*attacked)->assistList.pop();
				}

				(*attacked)->assistList.push(make_pair(clients_room_info[client_socket]->socket, newTime.count()));
				//cout << (*attacked)->assistList.top().first << " 의 시간 : " << newTime.count() << endl;


				for (auto inst : client_list_room)
				{
					if (inst->socket == (*attacked)->socket)
						ClientStat((*attacked)->socket);

					if ((*attacked)->curhp <= 0) { 
						//승리조건 상대를 죽일시
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
				clients_room_info[client_socket]->stopped = true;


				auto currentTime = chrono::high_resolution_clock::now();
				auto deltaTime = currentTime - lastUpdateTime;
				float deltaTimeInSeconds = chrono::duration<float>(deltaTime).count();
				clients_room_info[client_socket]->curdelay += deltaTimeInSeconds;
				lastUpdateTime = currentTime;
			}
		}
		else {
			clients_room_info[client_socket]->stopped = true;


			auto currentTime = chrono::high_resolution_clock::now();
			auto deltaTime = currentTime - lastUpdateTime;
			float deltaTimeInSeconds = chrono::duration<float>(deltaTime).count();
			clients_room_info[client_socket]->curdelay += deltaTimeInSeconds;
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

void Room::AttackStruct(int client_socket, void* data)
{


	attinfo info;
	memcpy(&info, data, sizeof(attinfo));

	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;

	list<Client*> clients_in_room = client_list_room;
	list<structure*> structure_in_room = structure_list_room;

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


	double distance = Distance((*attacker)->x, (*attacker)->y, (*attacker)->z, (*attacked)->x, (*attacked)->y, (*attacked)->z);

	if (distance <= (*attacker)->attrange)
	{
		if (clients_room_info[client_socket]->curdelay >= clients_room_info[client_socket]->maxdelay) {


			random_device rd;
			mt19937 gen(rd());
			uniform_real_distribution<float> dis(0, 1); //균일한 분포;를 형성
			double chance = (*attacker)->criProbability / 100.0;
			double cridmg = (*attacker)->critical / 100.0;
			int att = (*attacker)->attack;

			if (dis(gen) < chance)
			{
				double critDamage = (*attacker)->attack * cridmg;
				att = (*attacker)->attack + (int)critDamage;
			}

			//cout << (*attacker)->socket << " 의 공격 :: " << (*attacked)->index << "'s Hp : " << (*attacked)->curhp;
			(*attacked)->curhp -= att;
			clients_room_info[client_socket]->curdelay = 0;

			for (auto inst : client_list_room)
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
			clients_room_info[client_socket]->curdelay += deltaTimeInSeconds;
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

void Room::NewStructure(int index, int chan, int room, int team, float x, float y, float z)
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

	structure_list_room.push_back(temp_);


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

	lock_guard<std::mutex> lock(mutex_client);
	for (Client* inst : client_list_room)
		PacketManger::Send(inst->socket, H_STRUCTURE_CREATE, &info, sizeof(structureInfo));


	if (30000 > temp_->index && temp_->index > 20000) {
		thread turretSearchThread(&TurretSearchWorker, index, chan, room);
		turretSearchThread.detach();
		//TurretSearch
	}
}

void Room::StructureDie(int index, int chan, int room)
{
	lock_guard<std::mutex> lock(mutex_client);
	for (auto inst : client_list_room)
		PacketManger::Send(inst->socket, H_STRUCTURE_DIE, &index, sizeof(int));

	auto list = structure_list_room;
	for (auto inst : list)
	{
		if (inst->index == index)
			structure_list_room.remove(inst);
	}

	if (30000 > index && index > 20000)
		StopTurretSearch(index);
}

void Room::StructureStat(int index, int chan, int room) {
	structure* stru_ = nullptr;
	for (auto inst : structure_list_room)
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

	for (Client* inst : client_list_room)
		PacketManger::Send(inst->socket, H_STRUCTURE_STAT, &info, sizeof(structureInfo));
	
}

void Room::TurretSearch(int index, int chan, int room) {
	list<Client*>& clients_in_room = client_list_room;
	list<structure*>& structures_in_room = structure_list_room;
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

			double distance = Distance((*attacker)->x, (*attacker)->y, (*attacker)->z, client->x, client->y, client->z);

			if (distance <= (*attacker)->attrange) {


				int attacked_ = client->socket;

				if (clients_room_info[attacked_] == nullptr)
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

void Room::TurretShot(int index, bullet* newBullet, int attacked_, int chan, int room) {
	list<Client*>& clients_in_room = client_list_room;
	list<structure*>& structures_in_room = structure_list_room;

	auto attacked = find_if(clients_in_room.begin(), clients_in_room.end(), [attacked_](Client* client) {
		return client->socket == attacked_;
		});

	auto attacker = find_if(structures_in_room.begin(), structures_in_room.end(), [index](structure* struc) {
		return struc->index == index;
		});

	if ((*attacked)->curhp <= 0)
		return;

	if (attacked != clients_in_room.end() && attacker != structures_in_room.end()) {
		double targetX = (*attacked)->x;
		double targetY = (*attacked)->y;
		double targetZ = (*attacked)->z;

		double directionX = targetX - newBullet->x;
		double directionY = targetY - newBullet->y;
		double directionZ = targetZ - newBullet->z;

		double distance = sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
		directionX /= distance;
		directionY /= distance;
		directionZ /= distance;

		double moveDistance = (*attacker)->bulletspeed / 500.0;  // 프레임 시간 간격 (단위: s)

		if (clients_room_info[attacked_] == nullptr)
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

void Room::ClientDie(int client_socket, int killer) {



	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;

	clients_room_info[client_socket]->death += 1;
	ClientStat(client_socket);
	if (killer < 1000)
	{
		cout << clients_room_info[killer]->socket << "의 킬 : " << clients_room_info[killer]->kill << endl;
		clients_room_info[killer]->kill += 1;
		ClientStat(killer);
	}
	else {
		list<structure*>& structures_in_room = structure_list_room;
		auto attacker = find_if(structures_in_room.begin(), structures_in_room.end(), [killer](structure* struc) {
			return struc->index == killer;
			});
		cout << (*attacker)->index << "의 킬 : " << clients_room_info[killer]->kill << endl;
	}
	cout << clients_room_info[client_socket]->socket << "의 데스 : " << clients_room_info[client_socket]->death << endl;

	if (clients_room_info[killer]->kill >= 2)
	{
		SendVictory(clients_room_info[killer]->socket, clients_room_info[killer]->team, chan, room);
		return;
	}
	attinfo info;
	info.attacked = client_socket;
	info.attacker = killer;



	int secondValue = -1; // top 바로 아래 값 (default는 -1)

	if (clients_room_info[client_socket]->assistList.size() >= MAX_TEAM_PER_ROOM)
	{
		stack<pair<int, float>> tempStack = clients_room_info[client_socket]->assistList;

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

			lock_guard<std::mutex> lock(mutex_client);
			for (auto inst : client_list_room)
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


	chrono::time_point<chrono::system_clock> currentTime = chrono::system_clock::now();
	chrono::duration<double> elapsed_seconds = currentTime - startTime;

	int gameMinutes = static_cast<int>(elapsed_seconds.count() / 60);

	int respawnTime = 3 + (gameMinutes * 3); //min 3s

	lock_guard<std::mutex> lock(mutex_client);
	for (auto inst : client_list_room) {
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

void Room::WaitAndRespawn(int respawnTime, const chrono::time_point<chrono::system_clock>& diedtTime, int client_socket) {
	chrono::time_point<chrono::system_clock> currentTime = chrono::system_clock::now();
	chrono::duration<double> elapsed_seconds = currentTime - diedtTime;

	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;

	while (elapsed_seconds.count() < respawnTime) {


		if (clients_room_info[client_socket] == nullptr)
			return;

		currentTime = chrono::system_clock::now();
		elapsed_seconds = currentTime - diedtTime;
	}
	ClientRespawn(client_socket);
}

void Room::TurretSearchWorker(int index, int chan, int room) {
	while (!turretSearchStopMap[index]) {
		this_thread::sleep_for(chrono::seconds(1));

		TurretSearch(index, chan, room);
	}
}

void Room::StopTurretSearch(int index) {

	turretSearchStopMap[index] = true;
}

void Room::ClientRespawn(int client_socket) {

	if (clients_room_info[client_socket] == nullptr)
		return;

	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;

	lock_guard<std::mutex> lock(mutex_client);
	for (auto inst : client_list_room)
	{
		if (inst->socket == client_socket) {
			inst->x = 0;
			inst->y = 0;
			inst->z = 0;
			inst->curhp = inst->maxhp;
			ClientStat(client_socket);
		}
		BYTE* packet_data = new BYTE[sizeof(int)];
		memcpy(packet_data, &client_socket, sizeof(int));
		PacketManger::Send(inst->socket, H_CLIENT_RESPAWN, packet_data, sizeof(int));

		delete[] packet_data;
	}
}

void Room::ItemStat(int client_socket, void* data)
{
	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;

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

	size_t index = 0;
	auto nonZeroIt = find_if(clients_room_info[client_socket]->itemList.begin(), clients_room_info[client_socket]->itemList.end(),
		[](int item) { return item == 0; });

	if (nonZeroIt != clients_room_info[client_socket]->itemList.end())
		index = distance(clients_room_info[client_socket]->itemList.begin(), nonZeroIt);
	else index = clients_room_info[client_socket]->itemList.size() - 1;

	if (isPerchase)
	{
		if (index >= clients_room_info[client_socket]->itemList.size()) // 더 이상 추가 아이템을 구매할 수 없을 때
		{
			cout << "꽉 찼어." << endl;
			return;
		}
		else if (clients_room_info[client_socket]->gold >= NeedGold)
		{
			clients_room_info[client_socket]->gold -= NeedGold;
			clients_room_info[client_socket]->maxhp += (*curItem).maxhp;
			clients_room_info[client_socket]->attack += (*curItem).attack;
			clients_room_info[client_socket]->maxdelay += (*curItem).maxdelay;
			clients_room_info[client_socket]->attspeed += (*curItem).attspeed;
			clients_room_info[client_socket]->movespeed += (*curItem).movespeed;
			clients_room_info[client_socket]->criProbability += (*curItem).criProbability;

			cout << client_socket << "님이 " << (*curItem).name << " 를 " << NeedGold << "에 구매하는데 성공했습니다." << endl;

			clients_room_info[client_socket]->itemList[index] = ((*curItem).id); // 아이템 추가
			/*
			for (const int& itemID : clients_room_info[client_socket]->itemList)
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
		for (int i = 0; i < clients_room_info[client_socket]->itemList.size(); i++)
		{
			if (clients_room_info[client_socket]->itemList[i] == id)
			{
				clients_room_info[client_socket]->gold += static_cast<int>(round(NeedGold * 0.8f));
				clients_room_info[client_socket]->maxhp -= (*curItem).maxhp;
				clients_room_info[client_socket]->attack -= (*curItem).attack;
				clients_room_info[client_socket]->maxdelay -= (*curItem).maxdelay;
				clients_room_info[client_socket]->attspeed -= (*curItem).attspeed;
				clients_room_info[client_socket]->movespeed -= (*curItem).movespeed;
				clients_room_info[client_socket]->criProbability -= (*curItem).criProbability;

				cout << client_socket << "님이 " << (*curItem).name << " 를 " << NeedGold * 0.8f << "에 판매하는데 성공했습니다." << endl;

				clients_room_info[client_socket]->itemList[i] = 0; // 아이템 삭제
				/*for (const int& itemID : clients_room_info[client_socket]->itemList)
				{
					cout << itemID << endl;
				}*/

				ClientStat(client_socket);
				break;
			}
		}
	}
}

void Room::Well(int client_socket, void* data) {
	int chan = clients_room_info[client_socket]->channel;
	int room = clients_room_info[client_socket]->room;

	ClientInfo info;
	memcpy(&info, data, sizeof(ClientInfo));

	structure* stru_ = nullptr;
	for (auto inst : structure_list_room)
	{
		if (inst->index > 10000 && inst->index < 20000 && inst->team == clients_room_info[client_socket]->team)
			stru_ = inst;
	}

	if (stru_ == nullptr)
		return;

	double distance = Distance(stru_->x, stru_->y, stru_->z, info.x, info.y, info.z);
	int minDistance = 18;
	if (distance <= minDistance && clients_room_info[client_socket]->curhp < clients_room_info[client_socket]->maxhp)
	{
		clients_room_info[client_socket]->curhp += 1;
		ClientStat(client_socket);
	}
	else return;
}

void Room::CharLevelUp(Client* client) {
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

void Room::ChampPickTimeOut(int timemin, int channel, int room) {

	chrono::time_point<chrono::system_clock> currentTime = chrono::system_clock::now();
	chrono::duration<double> elapsed_seconds = currentTime - startTime;


	while (elapsed_seconds.count() < timemin) {
		this_thread::sleep_for(chrono::milliseconds(2000));

		if (client_list_room.size() != MAX_CLIENT_PER_ROOM)
			break;

		currentTime = chrono::system_clock::now();
		elapsed_seconds = currentTime - startTime;

		// cout << "room size : "<<client_channel[channel].client_list_room[room].size() << ", count :" << elapsed_seconds.count() << endl;
	}

	if (AllClientsReady(channel, room)) {
		cout << "모든 사용자들이 Ready 상태입니다. 전투를 시작합니다." << endl;
		// 전투 룸으로 넘어갑니다

		for (auto inst : client_list_room)
		{
			size_t client_count = client_list_room.size();
			BYTE* packet_data = new BYTE[sizeof(int) * 2 * client_count];
			size_t packet_size = sizeof(int) * 2 * client_count;
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

		startTime = chrono::system_clock::now();

		for (auto inst : client_list_room) // 룸의 클라이언트들을 생성합니다.
		{
			for (auto inst2 : client_list_room) // 나 자신을 뺴고 생성해야한다
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

		for (auto& client : client_list_room)
		{
			if (client == nullptr)
				continue;

			closesocket(client->socket);

		}
		cout << " 1분이 경과할 동안 전체 유저들의 픽이 이뤄지지 않았습니다. 종료합니다." << endl;
	}

	return;
}