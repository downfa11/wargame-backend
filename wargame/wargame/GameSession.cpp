#include "GameSession.h"

#include "GameManager.h"

#include "PacketManager.h"
#include "Resource.h"
#include "Utility.h"
#include "Timer.h"

#include<shared_mutex>

using namespace std;


std::vector<ChatEntry> GameSession::chat_log;
std::list<Client*> GameSession::client_list_room;
std::list<Structure*> GameSession::structure_list_room;
std::list<Unit*> GameSession::unit_list_room;

std::shared_mutex GameSession::chat_mutex;
std::shared_mutex GameSession::room_mutex;
std::shared_mutex GameSession::structure_mutex;
std::shared_mutex GameSession::unit_mutex;

std::chrono::time_point<std::chrono::system_clock> GameSession::startTime;


std::vector<ChampionStats> ChampionSystem::champions;
std::vector<itemStats> ItemSystem::items;

void GameSession::ClientChat(int client_socket, int size, void* data)
{
	BYTE* packet_data = new BYTE[size + sizeof(int)];
	memcpy(packet_data, &client_socket, sizeof(int));
	memcpy(&packet_data[sizeof(int)], data, size);

	int chan = -1, room = -1;
	string name = "";
	{
		shared_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		Client* sender = GameManager::clients_info[client_socket];
		if (sender == nullptr) {
			cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << endl;
			return;
		}
		chan = sender->channel;
		room = sender->room;
		name = sender->user_name;
	}

	if (chan == -1 || room == -1 || name == "") {
		cout << "lock error" << endl;
		return;
	}


	if (chan < 0 || chan >= MAX_CHANNEL_COUNT || room < 0 || room >= MAX_ROOM_COUNT_PER_CHANNEL) {
		cout << "Invalid channel or room index." << endl;
		delete[] packet_data;
		return;
	}

	auto curtime = chrono::system_clock::now();
	if (chat_log.size() == 0)
		chat_log.push_back({ "host", {curtime, "Game Start - 게임 시작을 알립니다."} });

	string chat(reinterpret_cast<char*>(&packet_data[sizeof(int)]), size);

	chat_log.push_back({ name, {curtime, chat} });


	{
		shared_lock<shared_mutex> lock(room_mutex);
		for (auto cli : GameSession::client_list_room)
			PacketManger::Send(cli->socket, H_CHAT, packet_data, size + sizeof(int));
	}

	delete[] packet_data;
}

void GameSession::ClientMoveStart(int client_socket, void* data)
{
	ClientMovestart info;
	memcpy(&info, data, sizeof(ClientMovestart));

	int chan = -1, room = -1;

	{
		unique_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << endl;
			return;
		}
		chan = client_info->channel;
		room = client_info->room;

		if (chan == -1 || room == -1) {
			cout << "lock error" << endl;
			return;
		}

		client_info->rotationX = info.rotationX;
		client_info->rotationY = info.rotationY;
		client_info->rotationZ = info.rotationZ;
	}

	{
		shared_lock<shared_mutex> lock(room_mutex);

		for (auto inst : client_list_room)
		{
			if (inst->socket != client_socket)
				PacketManger::Send(inst->socket, H_MOVESTART, &info, sizeof(ClientMovestart));
		}
	}
}

bool GameSession::IsPositionValid(const Client& currentPos, const ClientInfo& newPos) {
	float distSquared = (newPos.x - currentPos.x) * (newPos.x - currentPos.x) +
		(newPos.y - currentPos.y) * (newPos.y - currentPos.y) +
		(newPos.z - currentPos.z) * (newPos.z - currentPos.z);
	return distSquared <= MAX_MOVE_DISTANCE * MAX_MOVE_DISTANCE;
}

void GameSession::ClientMove(int client_socket, void* data)
{
	ClientInfo info;
	memcpy(&info, data, sizeof(ClientInfo));


	int chan = -1, room = -1;
	bool positionValid = false;
	{
		unique_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << endl;
			return;
		}
		chan = client_info->channel;
		room = client_info->room;

		if (chan == -1 || room == -1) {
			cout << "lock error" << endl;
			return;
		}

		if (IsPositionValid(*client_info, info)) {
			client_info->x = info.x;
			client_info->y = info.y;
			client_info->z = info.z;
			positionValid = true;
		}
		else {
			cout << "ClientMove 위치 검증 실패: 비정상적 이동 탐지됨" << endl;
			return;
		}
	}


	if (positionValid) {
		shared_lock<shared_mutex> lock(room_mutex);
		for (auto inst : GameSession::client_list_room)
		{
			if (inst->socket != client_socket)
				PacketManger::Send(inst->socket, H_MOVE, &info, sizeof(ClientInfo));
		}
	}
}

void GameSession::ClientMoveStop(int client_socket, void* data)
{
	//움직인다
	ClientInfo info;
	memcpy(&info, data, sizeof(ClientInfo));

	int chan = -1, room = -1;
	bool positionValid = false;
	{
		unique_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << endl;
			return;
		}
		chan = client_info->channel;
		room = client_info->room;

		if (chan == -1 || room == -1) {
			cout << "lock error" << endl;
			return;
		}

		if (IsPositionValid(*client_info, info)) {
			client_info->x = info.x;
			client_info->y = info.y;
			client_info->z = info.z;
			positionValid = true;
		}
		else {
			cout << "ClientMoveStop 위치 검증 실패: 비정상적 이동 탐지됨" << endl;
			return;
		}
	}


	if (positionValid) {
		shared_lock<shared_mutex> lock(room_mutex);
		for (auto inst : GameSession::client_list_room)
		{
			if (inst->socket != client_socket)
				PacketManger::Send(inst->socket, H_MOVESTOP, &info, sizeof(ClientInfo));
		}
	}
}

std::vector<ChatEntry> GameSession::GetChatLog()
{
	return chat_log;
}

void GameSession::ClientReady(int client_socket, int size, void* data)
{
	int champindex;
	memcpy(&champindex, data, sizeof(int));

	int chan = -1, room = -1;

	{
		shared_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << endl;
			return;
		}
		chan = client_info->channel;
		room = client_info->room;
	}

	if (chan == -1 || room == -1) {
		cout << "lock error" << endl;

		return;
	}


	{
		unique_lock<shared_mutex> lock(room_mutex);
		for (auto inst : GameSession::client_list_room)
		{
			if (inst->socket == client_socket)
			{
				inst->ready = !inst->ready;
				inst->champindex = champindex;

				BYTE* packet_data = new BYTE[sizeof(int) + sizeof(bool) + sizeof(int)];
				memcpy(packet_data, &client_socket, sizeof(int));
				memcpy(packet_data + sizeof(int), &inst->ready, sizeof(bool));
				memcpy(packet_data + sizeof(int) + sizeof(bool), &champindex, sizeof(int));

				for (auto client : GameSession::client_list_room)
					PacketManger::Send(client->socket, H_IS_READY, packet_data, sizeof(int) + sizeof(bool) + sizeof(int));


				if (inst->ready)
					cout << "ready to " << inst->socket << ":" << champindex << endl;
				else
					cout << "disready to " << inst->socket << endl;


				delete[] packet_data;
				break;
			}
		}
	}
}

bool GameSession::AllClientsReady(int chan, int room) {

	{
		shared_lock<shared_mutex> lock(room_mutex);

		if (GameSession::client_list_room.size() != MAX_CLIENT_PER_ROOM)
			return false;

		for (auto inst : GameSession::client_list_room) {
			if (!inst->ready)
				return false;
		}
	}

	return true;
}

// todo.
void GameSession::SendVictory(int winTeam, int channel, int room)
{
	auto structurelist = GameSession::structure_list_room;
	auto clientist = GameSession::client_list_room;

	for (auto inst : structurelist)
		StructureManager::StopTurretSearch(inst->index);
	structurelist.clear();

	BYTE packet_data[sizeof(int)];
	memcpy(packet_data, &winTeam, sizeof(int));

	MatchResult result;
	result.state = "success";
	result.channel = channel;
	result.room = room;

	auto now = chrono::system_clock::now();
	auto now_c = chrono::system_clock::to_time_t(now);

	tm tm;
	localtime_s(&tm, &now_c);

	stringstream ss;
	ss << put_time(&tm, "%Y-%m-%d %H:%M:%S");

	result.dateTime = ss.str();
	cout << "dateTime : " << result.dateTime << endl;


	RoomData curRoom;

	{
		for (auto& inst : GameManager::auth_data) {

			if (inst.channel == channel && inst.room == room) {
				result.spaceId = inst.spaceId;
				curRoom = inst;
				break;
			}
		}

		for (auto& user : curRoom.blueTeam) {
			for (auto client : clientist) {
				if (client->clientindex == stoi(user.user_index)) {
					PacketManger::Send(client->socket, H_VICTORY, packet_data, sizeof(int));

					result.blueTeams.push_back(client);
				}

			}
		}

		for (auto& user : curRoom.redTeam) {
			for (auto client : clientist) {
				if (client->clientindex == stoi(user.user_index)) {
					PacketManger::Send(client->socket, H_VICTORY, packet_data, sizeof(int));

					result.redTeams.push_back(client);
				}

			}
		}
	}

	chrono::duration<double> elapsed_seconds;

	{
		elapsed_seconds = now - GameSession::startTime;
		GameSession::startTime = chrono::time_point<chrono::system_clock>();
	}

	int gameMinutes = static_cast<int>(elapsed_seconds.count() / 60);

	result.gameDuration = gameMinutes;

	result.winTeam = (winTeam == 0 ? "blue" : "red");
	result.loseTeam = (winTeam == 0 ? "red" : "blue");

	MatchManager::SaveMatchResult(result);

	//clear
	auto condition = [channel, room](RoomData& roomInfo) {
		return roomInfo.channel == channel && roomInfo.room == room; };
	GameManager::auth_data.erase(std::remove_if(GameManager::auth_data.begin(), GameManager::auth_data.end(), condition), GameManager::auth_data.end());


	for (auto inst : clientist)
		GameManager::ClientClose(inst->socket);

	GameSession::client_list_room.clear();
	GameSession::structure_list_room.clear();
}

void GameSession::ClientStat(int client_socket) {
	int chan = -1, room = -1;
	ClientInfo info{};
	ItemSlots slots{};

	{
		shared_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr)
			return;


		info.socket = client_socket;
		info.champindex = client_info->champindex;
		info.gold = client_info->gold;

		info.kill = client_info->kill;
		info.death = client_info->death;
		info.assist = client_info->assist;

		info.level = client_info->level;
		info.curhp = client_info->curhp;
		info.maxhp = client_info->maxhp;
		info.curmana = client_info->curmana;
		info.maxmana = client_info->maxmana;
		info.attack = client_info->attack;
		info.critical = client_info->critical;
		info.criProbability = client_info->criProbability;
		info.attrange = client_info->attrange;
		info.attspeed = client_info->attspeed;
		info.movespeed = client_info->movespeed;

		slots.socket = client_socket;
		size_t itemCount = client_info->itemList.size();
		for (size_t i = 0; i < itemCount; ++i) {
			int itemID = i < itemCount ? client_info->itemList[i] : 0;

			switch (i) {
			case 0: slots.id_0 = itemID; break;
			case 1: slots.id_1 = itemID; break;
			case 2: slots.id_2 = itemID; break;
			case 3: slots.id_3 = itemID; break;
			case 4: slots.id_4 = itemID; break;
			case 5: slots.id_5 = itemID; break;
			}
		}

		chan = client_info->channel;
		room = client_info->room;
	}

	if (chan == -1 || room == -1) {
		cout << "lock error" << endl;
		return;
	}

	{
		shared_lock<shared_mutex> lock(room_mutex);
		for (auto inst : GameSession::client_list_room)
		{
			PacketManger::Send(inst->socket, H_CLIENT_STAT, &info, sizeof(ClientInfo));
			PacketManger::Send(inst->socket, H_ITEM_STAT, &slots, sizeof(ItemSlots));
		}
	}
}

void GameSession::ClientChampInit(int client_socket) {

	int chan = -1, room = -1, champIndex = -1;

	{
		shared_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];
		if (client_info == nullptr) {
			cout << client_socket << " 소켓의 사용자는 픽 전에 종료하셨습니다." << endl;
			return;
		}

		chan = client_info->channel;
		room = client_info->room;
		champIndex = client_info->champindex;
	}

	if (chan == -1 || room == -1 || champIndex == -1) {
		cout << "lock error" << endl;
		return;
	}

	ChampionStats* champ = nullptr;
	{

		cout << client_socket << " 사용자의 챔피언은 " << champIndex << "입니다." << endl;
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

		unique_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];

		client_info->level = 1;
		client_info->curhp = champ->maxhp;
		client_info->maxhp = champ->maxhp;
		client_info->curmana = champ->maxmana;
		client_info->maxmana = champ->maxmana;
		client_info->attack = champ->attack;
		client_info->maxdelay = champ->maxdelay;
		client_info->attrange = champ->attrange;
		client_info->attspeed = champ->attspeed;
		client_info->movespeed = champ->movespeed;
		client_info->critical = champ->critical;
		client_info->criProbability = champ->criProbability;

		client_info->growhp = champ->growHp;
		client_info->growmana = champ->growMana;
		client_info->growAtt = champ->growAtt;
		client_info->growCri = champ->growCri;
		client_info->growCriPro = champ->growCriPob;
	}


	ClientInfo info;
	ItemSlots slots;
	{
		unique_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		auto& client_info = GameManager::clients_info[client_socket];

		info.socket = client_socket;
		info.x = client_info->x;
		info.y = client_info->y;
		info.z = client_info->z;

		info.champindex = client_info->champindex;
		info.gold = client_info->gold;
		info.level = client_info->level;
		info.curhp = client_info->curhp;
		info.maxhp = client_info->maxhp;
		info.curmana = client_info->curmana;
		info.maxmana = client_info->maxmana;
		info.attack = client_info->attack;
		info.critical = client_info->critical;
		info.criProbability = client_info->criProbability;
		info.attspeed = client_info->attspeed;
		info.attrange = client_info->attrange;
		info.movespeed = client_info->movespeed;

		slots = { client_socket, client_info->itemList[0], client_info->itemList[1], client_info->itemList[2],
				 client_info->itemList[3], client_info->itemList[4], client_info->itemList[5] };
	}


	{
		shared_lock<shared_mutex> lock(room_mutex);
		for (auto inst : GameSession::client_list_room)
		{
			PacketManger::Send(inst->socket, H_CHAMPION_INIT, &info, sizeof(ClientInfo));
			PacketManger::Send(inst->socket, H_ITEM_STAT, &slots, sizeof(ItemSlots));
		}
	}
}

void GameSession::ClientChampInit(Client* client) {

	if (client == nullptr) {
		cout << client->clientindex << " 인덱스의 사용자는 픽 전에 종료하셨습니다." << endl;
		return;
	}

	int chan = client->channel;
	int room = client->room;

	int champIndex = client->champindex;
	ChampionStats* champ = nullptr;
	cout << client->socket << " 사용자의 챔피언은 " << champIndex << "입니다." << endl;
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

	client->level = 1;
	client->curhp = (*champ).maxhp;
	client->maxhp = (*champ).maxhp;
	client->curmana = (*champ).maxmana;
	client->maxmana = (*champ).maxmana;
	client->attack = (*champ).attack;
	client->maxdelay = (*champ).maxdelay;
	client->attrange = (*champ).attrange;
	client->attspeed = (*champ).attspeed;
	client->movespeed = (*champ).movespeed;
	client->critical = (*champ).critical;
	client->criProbability = (*champ).criProbability;

	client->growhp = (*champ).growHp;
	client->growmana = (*champ).growMana;
	client->growAtt = (*champ).growAtt;
	client->growCri = (*champ).growCri;
	client->growCriPro = (*champ).growCriPob;

	cout << "clients maxhp : " << client->maxhp << endl;
}

void GameSession::MouseSearch(int client_socket, void* data)
{
	MouseInfo info;
	memcpy(&info, data, sizeof(MouseInfo));

	int chan = -1, room = -1, teamClient = -1, kind = info.kind;

	{
		shared_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		chan = GameManager::clients_info[client_socket]->channel;
		room = GameManager::clients_info[client_socket]->room;
		teamClient = GameManager::clients_info[client_socket]->team;
	}

	if (chan == -1 || room == -1 || teamClient == -1) {
		cout << "lock error" << endl;
		return;
	}

	double minDistance = FLT_MAX * 2;
	void* closestTarget = nullptr;

	if (kind == 0) // Player
	{
		shared_lock<shared_mutex> lockRoom(room_mutex);

		for (auto& inst : GameSession::client_list_room) {
			if (inst->socket != client_socket && teamClient != inst->team) {
				float distance = UtilityManager::DistancePosition(info.x, info.y, info.z, inst->x, inst->y, inst->z);
				if (distance >= MAX_MOUSE_SEARCH)
				{
					cout << "너무 멀어. 근처에 없어 :" << distance << "\n";
				}
				else if (distance < minDistance) {
					closestTarget = static_cast<void*>(inst);
					minDistance = distance;
				}
			}
		}
	}
	else if (kind == 1) // Structure
	{
		shared_lock<shared_mutex> lockStruct(StructureManager::structure_mutex);
		for (auto& inst : GameSession::structure_list_room) {
			if (teamClient != inst->team) {
				float distance = UtilityManager::DistancePosition(info.x, info.y, info.z, inst->x, inst->y, inst->z);
				if (distance >= MAX_MOUSE_SEARCH)
				{
					cout << "너무 멀어. 근처에 없어 :" << distance << "\n";
				}
				else if (distance < minDistance) {
					closestTarget = static_cast<void*>(inst);
					minDistance = distance;
				}
			}
		}
	}
	else if (kind == 2) { // Unit
		shared_lock<shared_mutex> lockStruct(unit_mutex);
		for (auto& inst : GameSession::unit_list_room) {
			if (teamClient != inst->team) {
				float distance = UtilityManager::DistancePosition(info.x, info.y, info.z, inst->x, inst->y, inst->z);
				if (distance >= MAX_MOUSE_SEARCH)
				{
					cout << "너무 멀어. 근처에 없어 :" << distance << "\n";
				}
				else if (distance < minDistance) {
					closestTarget = static_cast<void*>(inst);
					minDistance = distance;
				}
			}
		}
	}
	else {
		cout << " 어휴.. 넌 또 뭘 클릭한거냐... 이 위치로 이동시켜줘?" << "\n";
		return;
	}

	if (closestTarget != nullptr)
	{
		int team = -1, value = -1, object_kind = -1;
		switch (kind) {
		case 0: // Player
			team = static_cast<Client*>(closestTarget)->team;
			kind = 0;
			object_kind = -1;
			value = static_cast<Client*>(closestTarget)->socket;
			break;
		case 1: // Structure
			team = static_cast<Structure*>(closestTarget)->team;
			kind = 1;
			object_kind = static_cast<Structure*>(closestTarget)->struct_kind;
			value = static_cast<Structure*>(closestTarget)->index;
			break;
		case 2: // Unit
			team = static_cast<Unit*>(closestTarget)->team;
			kind = 2;
			object_kind = static_cast<Unit*>(closestTarget)->unit_kind;
			value = static_cast<Unit*>(closestTarget)->index;
			break;
		}

		cout << "kind: " << kind << ", team : " << team << ", object_kind: " << object_kind << ", value: " << value << endl;

		if (team == -1 || kind == -1 || value == -1) {
			cout << "뭔가 잘못되었어. 안보여 team,kind, value" << endl;
			return;
		}


		BYTE* packet_data = new BYTE[sizeof(int) * 5];
		memcpy(packet_data, &client_socket, sizeof(int));
		memcpy(packet_data + sizeof(int), &team, sizeof(int));
		memcpy(packet_data + sizeof(int) * 2, &object_kind, sizeof(int));
		memcpy(packet_data + sizeof(int) * 3, &kind, sizeof(int));
		memcpy(packet_data + sizeof(int) * 4, &value, sizeof(int));
		PacketManger::Send(client_socket, H_ATTACK_TARGET, packet_data, sizeof(int) * 5);

		delete[] packet_data;

	}
	else cout << "closestTarget 없어임마" << endl;

}

void GameSession::AttackClient(int client_socket, void* data) {
	AttInfo info;
	memcpy(&info, data, sizeof(AttInfo));

	int chan = -1, room = -1;
	{
		shared_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		chan = GameManager::clients_info[client_socket]->channel;
		room = GameManager::clients_info[client_socket]->room;
	}

	if (chan == -1 || room == -1) {
		cout << "lock error" << endl;
		return;
	}

	Client* attacker = nullptr;
	Client* attacked = nullptr;

	{
		shared_lock<shared_mutex> lock(room_mutex);
		auto& clients_in_room = GameSession::client_list_room;

		auto attacker_it = find_if(clients_in_room.begin(), clients_in_room.end(),
			[client_socket](Client* client) { return client->socket == client_socket; });

		auto attacked_it = find_if(clients_in_room.begin(), clients_in_room.end(),
			[&info](Client* client) { return client->socket == info.attacked; });

		if (attacker_it == clients_in_room.end() || attacked_it == clients_in_room.end() || (*attacked_it)->curhp <= 0)
			return;

		attacker = *attacker_it;
		attacked = *attacked_it;

		float distance = UtilityManager::DistancePosition(attacker->x, attacker->y, attacker->z, attacked->x, attacked->y, attacked->z);

		if (distance > attacker->attrange + 1) {
			MouseInfo info{};
			info.x = attacked->x;
			info.y = attacked->y;
			info.z = attacked->z;
			info.kind = 0;
			MouseSearch(client_socket, &info);
			cout << "range? distance: " << distance << ", attrange: " << attacker->attrange << endl;
			return;
		}

		if (attacker->curdelay < attacker->maxdelay) {
			UpdateClientDelay(attacker);
			return;
		}


		int damage = CalculateDamage(attacker);
		attacked->curhp -= damage;
		attacker->curdelay = 0;

		NotifyAttackResulttoClient(client_socket, chan, room, attacked->socket);
	}

	if (attacker == nullptr || attacked == nullptr) {
		cout << "lock attacker, attacked error" << endl;
		return;
	}

	if (attacked->curhp <= 0)
		ClientDie(attacked->socket, client_socket, 0);

	ClientStat(attacked->socket);
}

//todo
void GameSession::AttackStructure(int client_socket, void* data)
{
	AttInfo info;
	memcpy(&info, data, sizeof(AttInfo));

	if (info.kind != 1) {
		cout << "왜 AttackStructure에 오신건가요? " << info.kind << ", object_kind: " << info.object_kind << endl;
		return;
	}

	int chan = -1, room = -1, team = -1;
	{
		shared_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		chan = GameManager::clients_info[client_socket]->channel;
		room = GameManager::clients_info[client_socket]->room;
		team = GameManager::clients_info[client_socket]->team;
	}

	if (chan == -1 || room == -1 || team == -1) {
		cout << "lock error" << endl;
		return;
	}

	Client* attacker = nullptr;
	Structure* attacked = nullptr;

	{
		shared_lock<shared_mutex> lock(room_mutex);
		// shared_lock<shared_mutex> lock(structure_mutex[chan][room]);

		auto& clients_in_room = GameSession::client_list_room;
		auto& structures_in_room = GameSession::structure_list_room;

		auto attacker_it = find_if(clients_in_room.begin(), clients_in_room.end(),
			[client_socket](Client* client) { return client->socket == client_socket; });

		auto attacked_it = find_if(structures_in_room.begin(), structures_in_room.end(),
			[&info, &team](Structure* struc) { return (struc->team != team && struc->index == info.attacked && struc->struct_kind == info.object_kind); });
		if (attacker_it == clients_in_room.end() || attacked_it == structures_in_room.end()) {
			cout << "none struc :" << info.attacked << ", struc kind :" << info.object_kind << endl;
			return;
		}
		attacked = *attacked_it;
		if (attacked->curhp <= 0)
			return;

		attacker = *attacker_it;
		float distance = UtilityManager::DistancePosition(attacker->x, attacker->y, attacker->z, attacked->x, attacked->y, attacked->z);

		if (distance > attacker->attrange + 1)
		{
			MouseInfo info{};
			info.x = attacked->x;
			info.y = attacked->y;
			info.z = attacked->z;
			info.kind = 1;
			MouseSearch(client_socket, &info);
			cout << "range? distance: " << distance << ", attrange: " << attacker->attrange << endl;
			return;
		}

		if (GameManager::clients_info[client_socket]->curdelay < GameManager::clients_info[client_socket]->maxdelay) {
			UpdateClientDelay(attacker);
			return;
		}

		int damage = CalculateDamage(attacker);
		attacked->curhp -= damage;
		GameManager::clients_info[client_socket]->curdelay = 0;

		NotifyAttackResulttoStructure(client_socket, chan, room, attacked->index);
	}

	if (attacker == nullptr || attacked == nullptr) {
		cout << "lock attacker, attacked error" << endl;
		return;
	}

	StructureManager::StructureStat(attacked->index, attacked->team, attacked->struct_kind, chan, room);
}

void GameSession::UpdateClientDelay(Client* client)
{
	auto currentTime = chrono::high_resolution_clock::now();
	client->curdelay += chrono::duration<float>(currentTime - client->lastUpdateTime).count();
	client->lastUpdateTime = currentTime;
}

int GameSession::CalculateDamage(Client* attacker)
{
	static thread_local random_device rd;
	static thread_local mt19937 gen(rd());
	uniform_real_distribution<float> dis(0, 1);
	float chance = attacker->criProbability / 100.0f;
	float cridmg = attacker->critical / 100.0f;
	int att = attacker->attack;

	if (dis(gen) < chance)
		att += static_cast<int>(att * cridmg);

	return att;
}

void GameSession::NotifyAttackResulttoClient(int client_socket, int chan, int room, int attacked_socket)
{
	for (auto client : GameSession::client_list_room)
	{
		vector<BYTE> packet_data(sizeof(int) * 2);
		memcpy(packet_data.data(), &client_socket, sizeof(int));
		memcpy(packet_data.data() + sizeof(int), &attacked_socket, sizeof(int));
		PacketManger::Send(client->socket, H_ATTACK_CLIENT, packet_data.data(), packet_data.size());
	}
}

void GameSession::NotifyAttackResulttoStructure(int client_socket, int chan, int room, int attacked_index)
{
	for (auto client : GameSession::client_list_room)
	{
		vector<BYTE> packet_data(sizeof(int) * 2);
		memcpy(packet_data.data(), &client_socket, sizeof(int));
		memcpy(packet_data.data() + sizeof(int), &attacked_index, sizeof(int));
		PacketManger::Send(client->socket, H_ATTACK_STRUCT, packet_data.data(), packet_data.size());
	}
}

// todo
void GameSession::ClientDie(int client_socket, int killer, int kind) {
	// kind : killer's kind ( client:0, structure:1 )
	int chan = GameManager::clients_info[client_socket]->channel;
	int room = GameManager::clients_info[client_socket]->room;

	GameManager::clients_info[client_socket]->death += 1;
	ClientStat(client_socket);

	if (kind == 0)
	{
		GameManager::clients_info[killer]->kill += 1;
		cout << GameManager::clients_info[killer]->socket << "의 킬 : " << GameManager::clients_info[killer]->kill << endl;
		ClientStat(killer);
	}
	else if (kind == 1) {
		list<Structure*>& structures_in_room = GameSession::structure_list_room;
		auto attacker = find_if(structures_in_room.begin(), structures_in_room.end(), [killer](Structure* struc) {
			return struc->index == killer;
			});
		cout << (*attacker)->index << "의 킬 : 포탑의 킬 수는 카운트하지 않습니다." << endl;
	}
	else {
		cout << "이게머노" << endl;
		return;
	}

	cout << GameManager::clients_info[client_socket]->socket << "의 데스 : " << GameManager::clients_info[client_socket]->death << endl;

	if (GameManager::clients_info[killer]->kill >= 1)
	{
		SendVictory(GameManager::clients_info[killer]->team, chan, room);
		return;
	}
	AttInfo info;
	info.attacked = client_socket;
	info.attacker = killer;

	int secondValue = -1; // top 바로 아래 값 (default는 -1)

	if (GameManager::clients_info[client_socket]->assistList.size() > MAX_TEAM_PER_ROOM - 1)
	{
		stack<pair<int, int>> tempStack = GameManager::clients_info[client_socket]->assistList;

		// 스택이 MAX_TEAM_PER_ROOM 크기보다 크면 맨 아래 값을 제거
		vector<int> assistTargets;
		while (tempStack.size() > MAX_TEAM_PER_ROOM - 1) {
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

			for (auto inst : GameSession::client_list_room)
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

	else
		cout << "어시스트가 없거나 assistList에 요소가 부족합니다." << endl;


	chrono::time_point<chrono::system_clock> startTime = GameSession::startTime;
	chrono::time_point<chrono::system_clock> currentTime = chrono::system_clock::now();
	chrono::duration<double> elapsed_seconds = currentTime - startTime;

	int gameMinutes = static_cast<int>(elapsed_seconds.count() / 60);

	int respawnTime = 3 + (gameMinutes * 3); //min 3s

	for (auto inst : GameSession::client_list_room) {
		BYTE* packet_data = new BYTE[sizeof(AttInfo)];
		memcpy(packet_data, &info, sizeof(AttInfo));
		PacketManger::Send(inst->socket, H_KILL_LOG, packet_data, sizeof(AttInfo));

		delete[] packet_data;

		BYTE* packet_data2 = new BYTE[sizeof(int) * 2];
		memcpy(packet_data2, &client_socket, sizeof(int));
		memcpy(packet_data2 + sizeof(int), &respawnTime, sizeof(int));
		PacketManger::Send(inst->socket, H_CLIENT_DIE, packet_data2, sizeof(int) * 2);

		delete[] packet_data2;
	}

	cout << client_socket << " 님이 사망 대기 시간은 " << respawnTime << "초입니다." << endl;

	WaitAndRespawn(client_socket, respawnTime);
}

void GameSession::WaitAndRespawn(int client_socket, int respawnTime) {
	if (GameManager::clients_info[client_socket] == nullptr) {
		std::cout << "Client not found: " << client_socket << std::endl;
		return;
	}

	intptr_t timerId = static_cast<intptr_t>(client_socket);

	Timer::AddTimer(timerId, [client_socket]() {
		if (GameManager::clients_info[client_socket] == nullptr) {
			std::cout << "Client disconnected during respawn wait: " << client_socket << std::endl;
			Timer::RemoveTimer(static_cast<intptr_t>(client_socket));
			return;
		}

		std::cout << "Respawning client: " << client_socket << std::endl;
		ClientRespawn(client_socket);

		Timer::RemoveTimer(static_cast<intptr_t>(client_socket));
		}, respawnTime * 1000);
}

void GameSession::ClientRespawn(int client_socket) {

	int chan = -1, room = -1;
	{
		shared_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		if (GameManager::clients_info[client_socket] == nullptr)
			return;

		chan = GameManager::clients_info[client_socket]->channel;
		room = GameManager::clients_info[client_socket]->room;
	}

	{
		unique_lock<shared_mutex> lock(room_mutex);
		for (auto inst : GameSession::client_list_room)
		{
			if (inst->socket == client_socket) {
				inst->x = 0;
				inst->y = 0;
				inst->z = 0;
				inst->curhp = inst->maxhp;
				ClientStat(client_socket);
			}
		}

		for (auto inst : GameSession::client_list_room)
		{
			BYTE* packet_data = new BYTE[sizeof(int)];
			memcpy(packet_data, &client_socket, sizeof(int));
			PacketManger::Send(inst->socket, H_CLIENT_RESPAWN, packet_data, sizeof(int));

			delete[] packet_data;
		}
	}
}

//-----------------------------------------------------------------------------------
void GameSession::ItemStat(int client_socket, void* data)
{
	Item info;
	memcpy(&info, data, sizeof(Item));

	int id = info.id;
	bool isPerchase = info.isPerchase;


	// 인벤토리의 i번 위치임을 받아서 서버의 해당 위치 아이템의 index를 읽어야함. todo
	itemStats* curItem = nullptr;

	for (auto& item : ItemSystem::items) {
		if (item.id == id) {
			curItem = &item;
			break;
		}
	}

	int NeedGold = (*curItem).gold;

	int index = 0;

	{
		// ClientStat에서 데드락 
		// unique_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		auto nonZeroIt = find_if(GameManager::clients_info[client_socket]->itemList.begin(), GameManager::clients_info[client_socket]->itemList.end(),
			[](int item) { return item == 0; });

		if (nonZeroIt != GameManager::clients_info[client_socket]->itemList.end())
			index = std::distance(GameManager::clients_info[client_socket]->itemList.begin(), nonZeroIt);
		else index = GameManager::clients_info[client_socket]->itemList.size();

		if (isPerchase)
		{
			cout << index << " item" << endl;

			if (index >= GameManager::clients_info[client_socket]->itemList.size()) // 더 이상 추가 아이템을 구매할 수 없을 때
			{
				cout << "꽉 찼어." << endl;
				return;
			}
			else if (GameManager::clients_info[client_socket]->gold >= NeedGold)
			{
				GameManager::clients_info[client_socket]->gold -= NeedGold;
				GameManager::clients_info[client_socket]->maxhp += (*curItem).maxhp;
				GameManager::clients_info[client_socket]->attack += (*curItem).attack;
				GameManager::clients_info[client_socket]->maxdelay += (*curItem).maxdelay;
				GameManager::clients_info[client_socket]->attspeed += (*curItem).attspeed;
				GameManager::clients_info[client_socket]->movespeed += (*curItem).movespeed;
				GameManager::clients_info[client_socket]->criProbability += (*curItem).criProbability;

				cout << client_socket << "님이 " << (*curItem).name << " 를 " << NeedGold << "에 구매하는데 성공했습니다." << endl;

				GameManager::clients_info[client_socket]->itemList[index] = ((*curItem).id); // 아이템 추가

				ClientStat(client_socket);
			}
			else
			{
				cout << "show me the money" << endl;
			}
		}
		else
		{
			for (int i = 0; i < GameManager::clients_info[client_socket]->itemList.size(); i++)
			{
				if (GameManager::clients_info[client_socket]->itemList[i] == id)
				{
					GameManager::clients_info[client_socket]->gold += NeedGold * 0.8f;
					GameManager::clients_info[client_socket]->maxhp -= (*curItem).maxhp;
					GameManager::clients_info[client_socket]->attack -= (*curItem).attack;
					GameManager::clients_info[client_socket]->maxdelay -= (*curItem).maxdelay;
					GameManager::clients_info[client_socket]->attspeed -= (*curItem).attspeed;
					GameManager::clients_info[client_socket]->movespeed -= (*curItem).movespeed;
					GameManager::clients_info[client_socket]->criProbability -= (*curItem).criProbability;

					cout << client_socket << "님이 " << (*curItem).name << " 를 " << NeedGold * 0.8f << "에 판매하는데 성공했습니다." << endl;

					GameManager::clients_info[client_socket]->itemList[i] = 0; // 아이템 삭제

					ClientStat(client_socket);
					break;
				}
			}
		}
	}
}

void GameSession::Well(int client_socket, void* data) {
	int chan = -1, room = -1, team = -1, curhp = -1, maxhp = -1;

	{
		shared_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		chan = GameManager::clients_info[client_socket]->channel;
		room = GameManager::clients_info[client_socket]->room;
		team = GameManager::clients_info[client_socket]->team;
		curhp = GameManager::clients_info[client_socket]->curhp;
		maxhp = GameManager::clients_info[client_socket]->maxhp;
	}

	if (chan == -1 || room == -1 || team == -1 || curhp == -1 || maxhp == -1) {
		cout << "lock error" << endl;
		return;
	}

	ClientInfo info;
	memcpy(&info, data, sizeof(ClientInfo));

	Structure* stru_ = nullptr;

	{
		shared_lock<shared_mutex> lock(StructureManager::structure_mutex);
		for (auto inst : GameSession::structure_list_room)
			if (inst->struct_kind == 0 && inst->team == team)
				stru_ = inst;
	}

	if (stru_ == nullptr) {
		cout << "lock stru_ error" << endl;
		return;
	}

	float distance = UtilityManager::DistancePosition(stru_->x, stru_->y, stru_->z, info.x, info.y, info.z);
	int minDistance = 18;
	if (distance <= minDistance && curhp < maxhp)
	{
		GameManager::clients_info[client_socket]->curhp += 1;
		ClientStat(client_socket);
	}
	else return;
}

void GameSession::CharLevelUp(Client* client) {
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

void GameSession::champ1Passive(void* data) {
	AttInfo info;
	memcpy(&info, data, sizeof(AttInfo));
	int attacker_socket = info.attacker;

	int chan = -1, room = -1;
	{
		shared_lock<shared_mutex> lock(GameManager::clients_info_mutex);
		chan = GameManager::clients_info[attacker_socket]->channel;
		room = GameManager::clients_info[attacker_socket]->room;
	}

	if (chan == -1 || room == -1) {
		cout << "lock error" << endl;
		return;
	}

	Client* attacker = nullptr;



	{
		shared_lock<shared_mutex> lock(room_mutex);
		auto& clients_in_room = GameSession::client_list_room;

		auto attacker_it = find_if(clients_in_room.begin(), clients_in_room.end(),
			[attacker_socket](Client* client) { return client->socket == attacker_socket; });

		if (attacker_it == clients_in_room.end())
			return;

		attacker = *attacker_it;

		if (info.kind == -1) {

			Client* attacked = nullptr;
			auto attacked_it = find_if(clients_in_room.begin(), clients_in_room.end(),
				[&info](Client* client) { return client->socket == info.attacked; });

			if (attacked_it == clients_in_room.end() || (*attacked_it)->curhp <= 0)
				return;

			attacked = *attacked_it;


			int damage = 0.2f * attacker->attack * attacker->level;
			attacked->curhp -= damage;
			cout << "client passive demage : " << damage << " attacked.curHp :" << attacked->curhp << endl;
			NotifyAttackResulttoClient(attacker_socket, chan, room, attacked->socket);


			if (attacker == nullptr || attacked == nullptr) {
				cout << "lock attacker, attacked error" << endl;
				return;
			}

			if (attacked->curhp <= 0)
				ClientDie(attacked->socket, attacker_socket, 0);

			ClientStat(attacked->socket);
		}
		else {
			Structure* attacked = nullptr;

			{
				shared_lock<shared_mutex> lock(room_mutex);
				// shared_lock<shared_mutex> lock(structure_mutex[chan][room]);

				auto& structures_in_room = GameSession::structure_list_room;

				int team = attacker->team;

				auto attacked_it = find_if(structures_in_room.begin(), structures_in_room.end(),
					[&info, &team](Structure* struc) { return (struc->team != team && struc->index == info.attacked && struc->struct_kind == info.kind); });
				if (attacker_it == clients_in_room.end() || attacked_it == structures_in_room.end()) {
					cout << "none struc :" << info.attacked << ", struc kind :" << info.kind << endl;
					return;
				}
				attacked = *attacked_it;

				if (attacked->curhp <= 0)
					return;

				attacker = *attacker_it;

				int damage = 0.2f * attacker->attack * attacker->level;
				attacked->curhp -= damage;
				cout << "client passive demage : " << damage << " attacked.curHp :" << attacked->curhp << endl;

				NotifyAttackResulttoStructure(attacker_socket, chan, room, attacked->index);
			}

			if (attacker == nullptr || attacked == nullptr) {
				cout << "lock attacker, attacked error" << endl;
				return;
			}

			StructureManager::StructureStat(attacked->index, attacked->team, attacked->struct_kind, chan, room);
		}
	}
}