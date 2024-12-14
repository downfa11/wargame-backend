#include "GameSession.h"

#include "GameManager.h"
#include "PacketManager.h"
#include "Utility.h"
#include "Timer.h"

#include<shared_mutex>

using namespace std;

#define MAX_PLAYER_MOVE_SPEED 100

void GameSession::ClientChat(std::string& name, int size, void* data)
{
	auto curtime = chrono::system_clock::now();
	if (chat_log.size() == 0)
		chat_log.push_back({ "host", {curtime, "Game Start - 게임 시작을 알립니다."} });

	std::string chat(reinterpret_cast<char*>(data) + sizeof(int), size);

	chat_log.push_back({ name, {curtime, chat} });


	{
		shared_lock<shared_mutex> lock(room_mutex);
		for (auto cli : client_list_room)
			PacketManger::Send(cli->socket, H_CHAT, data, size + sizeof(int));
	}

}

bool GameSession::IsPositionValid(const Client& currentPos, const ClientInfo& newPos) {
	float distSquared = (newPos.x - currentPos.x) * (newPos.x - currentPos.x) +
		(newPos.y - currentPos.y) * (newPos.y - currentPos.y) +
		(newPos.z - currentPos.z) * (newPos.z - currentPos.z);
	return distSquared <= MAX_MOVE_DISTANCE * MAX_MOVE_DISTANCE;
}

void GameSession::ClientMoveStart(int client_socket, ClientMovestart* info)
{
	shared_lock<shared_mutex> lock(room_mutex);
	for (auto& inst : client_list_room)
	{
		if (inst->socket != client_socket)
			PacketManger::Send(inst->socket, H_MOVESTART, info, sizeof(ClientMovestart));
	}
}

void GameSession::ClientMove(int client_socket, ClientInfo info)
{
	bool positionValid = false;
	auto& client_info = GameManager::clients_info[client_socket];

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



	if (positionValid) {
		shared_lock<shared_mutex> lock(room_mutex);
		for (auto inst : client_list_room)
		{
			if (inst->socket != client_socket)
				PacketManger::Send(inst->socket, H_MOVE, &info, sizeof(ClientInfo));
		}
	}
}

void GameSession::ClientMoveStop(int client_socket, ClientInfo info)
{
	bool positionValid = false;
	auto& client_info = GameManager::clients_info[client_socket];
	if (IsPositionValid(*client_info, info)) {
		client_info->x = info.x;
		client_info->y = info.y;
		client_info->z = info.z;
		positionValid = true;
	}

	else {
		std::cout << "ClientMoveStop 위치 검증 실패: 비정상적 이동 탐지됨" << std::endl;
		return;
	}

	if (positionValid) {
		shared_lock<shared_mutex> lock(room_mutex);
		for (auto inst : client_list_room)
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

void GameSession::ClientReady(int client_socket, int champindex)
{
	{
		unique_lock<shared_mutex> lock(room_mutex);
		for (auto inst : client_list_room)
		{
			if (inst->socket == client_socket)
			{
				inst->ready = !inst->ready;
				inst->champindex = champindex;

				BYTE* packet_data = new BYTE[sizeof(int) + sizeof(bool) + sizeof(int)];
				memcpy(packet_data, &client_socket, sizeof(int));
				memcpy(packet_data + sizeof(int), &inst->ready, sizeof(bool));
				memcpy(packet_data + sizeof(int) + sizeof(bool), &champindex, sizeof(int));

				for (auto client : client_list_room)
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

		if (client_list_room.size() != MAX_CLIENT_PER_ROOM)
			return false;

		for (auto inst : client_list_room) {
			if (!inst->ready)
				return false;
		}
	}

	return true;
}

// todo.
void GameSession::SendVictory(int winTeam, int channel, int room)
{
	auto structurelist = structure_list_room;
	auto clientist = client_list_room;

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
		elapsed_seconds = now - startTime;
		startTime = chrono::time_point<chrono::system_clock>();
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

	client_list_room.clear();
	structure_list_room.clear();
}

void GameSession::ClientStat(int client_socket) {
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
		info.absorptionRate = client_info->absorptionRate;
		info.defense = client_info->defense;
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

	}


	{
		shared_lock<shared_mutex> lock(room_mutex);
		for (auto inst : client_list_room)
		{
			PacketManger::Send(inst->socket, H_CLIENT_STAT, &info, sizeof(ClientInfo));
			PacketManger::Send(inst->socket, H_ITEM_STAT, &slots, sizeof(ItemSlots));
		}
	}
}

void GameSession::ClientChampInit(Client* client, int champIndex) {
	int client_socket = client->socket;

	ChampionStats* champ = nullptr;

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


	client->level = 1;
	client->curhp = champ->maxhp;
	client->maxhp = champ->maxhp;
	client->curmana = champ->maxmana;
	client->maxmana = champ->maxmana;
	client->attack = champ->attack;
	client->absorptionRate = champ->absorptionRate;
	client->defense = champ->defense;
	client->maxdelay = champ->maxdelay;
	client->attrange = champ->attrange;
	client->attspeed = champ->attspeed;
	client->movespeed = champ->movespeed;
	client->critical = champ->critical;
	client->criProbability = champ->criProbability;

	client->growhp = champ->growHp;
	client->growmana = champ->growMana;
	client->growAtt = champ->growAtt;
	client->growCri = champ->growCri;
	client->growCriPro = champ->growCriPob;

	ClientInfo info;
	ItemSlots slots;

	info.socket = client_socket;
	info.x = client->x;
	info.y = client->y;
	info.z = client->z;

	info.champindex = client->champindex;
	info.gold = client->gold;
	info.level = client->level;
	info.curhp = client->curhp;
	info.maxhp = client->maxhp;
	info.curmana = client->curmana;
	info.maxmana = client->maxmana;
	info.attack = client->attack;
	info.absorptionRate = client->absorptionRate;
	info.defense = client->defense;
	info.critical = client->critical;
	info.criProbability = client->criProbability;
	info.attspeed = client->attspeed;
	info.attrange = client->attrange;
	info.movespeed = client->movespeed;

	slots = { client_socket, client->itemList[0], client->itemList[1], client->itemList[2],
			 client->itemList[3], client->itemList[4], client->itemList[5] };

	{
		shared_lock<shared_mutex> lock(room_mutex);
		for (auto inst : client_list_room)
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
	client->absorptionRate = (*champ).absorptionRate;
	client->defense = (*champ).defense;
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

void GameSession::MouseSearch(Client* client, MouseInfo info)
{
	if (!client)
		return;

	int client_socket = client->socket;
	int kind = info.kind;
	int teamClient = client->team;

	if (teamClient == -1) {
		cout << "MouseSearch team error" << endl;
		return;
	}

	double minDistance = FLT_MAX * 2;
	void* closestTarget = nullptr;

	if (kind == 0) // Player
	{
		shared_lock<shared_mutex> lockRoom(room_mutex);

		for (auto& inst : client_list_room) {
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
		shared_lock<shared_mutex> lockStruct(structure_mutex);
		for (auto& inst : structure_list_room) {
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
		for (auto& inst : unit_list_room) {
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

void GameSession::AttackClient(Client* client, AttInfo info) {
	if (!client) {
		return;
	}

	int client_socket = client->socket;
	int chan = client->channel;
	int room = client->room;
	int team = client->team;

	if (team == -1) {
		cout << "AttackClient team error" << endl;
		return;
	}

	Client* attacker = nullptr;
	Client* attacked = nullptr;

	{
		shared_lock<shared_mutex> lock(room_mutex);
		auto& clients_in_room = client_list_room;

		auto attacker_it = find_if(clients_in_room.begin(), clients_in_room.end(),
			[client_socket](Client* client) { return client->socket == client_socket; });

		auto attacked_it = find_if(clients_in_room.begin(), clients_in_room.end(),
			[&info](Client* client) { return client->socket == info.attacked; });

		if (attacker_it == clients_in_room.end() || attacked_it == clients_in_room.end() || (*attacked_it)->curhp <= 0)
			return;

		attacker = *attacker_it;
		attacked = *attacked_it;

		if (!IsValidAttackRange(attacker, attacked)) {
			MouseSearchToTarget(attacker, attacked);
			return;
		}

		if (!IsReadyToAttack(attacker)) {
			UpdateClientDelay(attacker);
			return;
		}


		int damage = CalculateDamage(attacker);
		ApplyDamage(attacked, damage);
		ApplyAbsorption(attacker, damage);

		attacker->curdelay = 0;
		NotifyAttackResulttoClient(client->socket, client->channel, client->room, attacked->socket);
	}

	if (!attacker || !attacked) {
		cout << "AttackClient lock error" << endl;
		return;
	}

	if (attacked->curhp <= 0) {
		ClientDie(attacked->socket, client->socket, 0);
	}

	ClientStat(attacked->socket);
}

//todo
void GameSession::AttackStructure(Client* client, AttInfo info) {
	if (!client || client->team == -1) {
		cout << "Invalid client or team error in AttackStructure" << endl;
		return;
	}

	int client_socket = client->socket;
	int chan = client->channel;
	int room = client->room;
	int team = client->team;


	Client* attacker = nullptr;
	Structure* attacked = nullptr;

	{
		shared_lock<shared_mutex> lock(room_mutex);
		// shared_lock<shared_mutex> lock(structure_mutex[chan][room]);

		auto& clients_in_room = client_list_room;
		auto& structures_in_room = structure_list_room;

		auto attacker_it = find_if(clients_in_room.begin(), clients_in_room.end(),
			[client_socket](Client* client) { return client->socket == client_socket; });

		auto attacked_it = find_if(structures_in_room.begin(), structures_in_room.end(),
			[&info, &team](Structure* struc) { return (struc->team != team && struc->index == info.attacked && struc->struct_kind == info.object_kind); });

		if (attacker_it == clients_in_room.end() || attacked_it == structures_in_room.end() || (*attacked_it)->curhp <= 0) {
			cout << "none struc :" << info.attacked << ", struc kind :" << info.object_kind << endl;
			return;
		}

		attacked = *attacked_it;
		attacker = *attacker_it;

		if (!IsValidAttackRange(attacker, attacked)) {
			MouseSearchToTarget(attacker, attacked);
			return;
		}

		if (!IsReadyToAttack(attacker)) {
			UpdateClientDelay(attacker);
			return;
		}

		int damage = CalculateDamage(attacker);
		attacked->curhp -= damage;

		attacker->curdelay = 0;
		NotifyAttackResulttoStructure(client->socket, client->channel, client->room, attacked->index);
	}
}

bool GameSession::IsValidAttackRange(Client* attacker, const Structure* target) {
	float distance = UtilityManager::DistancePosition(attacker->x, attacker->y, attacker->z, target->x, target->y, target->z);
	return distance <= attacker->attrange + 1;
}

bool GameSession::IsValidAttackRange(Client* attacker, const Client* target) {
	float distance = UtilityManager::DistancePosition(attacker->x, attacker->y, attacker->z, target->x, target->y, target->z);
	return distance <= attacker->attrange + 1;
}

void GameSession::MouseSearchToTarget(Client* attacker, Client* attacked) {
	MouseInfo info{};
	info.x = attacked->x;
	info.y = attacked->y;
	info.z = attacked->z;
	info.kind = 0;
	MouseSearch(attacker, info);
}

void GameSession::MouseSearchToTarget(Client* attacker, Structure* attacked) {
	MouseInfo info{};
	info.x = attacked->x;
	info.y = attacked->y;
	info.z = attacked->z;
	info.kind = 1;
	MouseSearch(attacker, info);
}

bool GameSession::IsReadyToAttack(Client* attacker) {
	return attacker->curdelay >= attacker->maxdelay;
}

void GameSession::ApplyDamage(Client* target, int damage) {
	if (damage < 0) damage = 0;
	double defenseMultiplier = (target->defense >= 100) ? 0.0 : (1.0 - target->defense / 100.0);
	int adjustedDamage = static_cast<int>(damage * defenseMultiplier);
	target->curhp -= adjustedDamage;

	std::cout << "원래 데미지는 " << damage << "인데, 방어력이 " << target->defense << "이므로 깎여서 " << adjustedDamage << "만 깎였어." << std::endl;
}

void GameSession::ApplyAbsorption(Client* attacker, int damage) {
	if (attacker->absorptionRate <= 0) return;

	if (attacker->absorptionRate > 1) {
		cout << "Warning: absorptionRate greater than 1 for " << attacker->socket << endl;
		attacker->absorptionRate = 1.0;
	}

	int health = static_cast<int>(damage * attacker->absorptionRate);
	attacker->curhp = ((attacker->curhp + health) < (attacker->maxhp)) ? (attacker->curhp + health) : (attacker->maxhp);
	ClientStat(attacker->socket);
}

void GameSession::UpdateClientDelay(Client* client)
{
	auto currentTime = chrono::high_resolution_clock::now();
	client->curdelay += chrono::duration<float>(currentTime - client->lastUpdateTime).count();
	client->lastUpdateTime = currentTime;
}

int GameSession::CalculateDamage(Client* attacker)
{
	if (!attacker)
		return 0;

	static thread_local std::random_device rd;
	static thread_local std::mt19937 gen(rd());
	std::uniform_real_distribution<float> dis(0, 1);

	float chance = attacker->criProbability / 100.0f;
	float cridmg = attacker->critical / 100.0f;
	int att = attacker->attack;

	if (!(chance < 0.0f || chance > 1.0f || cridmg < 0.0f) && (dis(gen) < chance))
		att += static_cast<int>(att * cridmg);

	return att;
}

void GameSession::NotifyAttackResulttoClient(int client_socket, int chan, int room, int attacked_socket)
{
	for (auto client : client_list_room)
	{
		vector<BYTE> packet_data(sizeof(int) * 2);
		memcpy(packet_data.data(), &client_socket, sizeof(int));
		memcpy(packet_data.data() + sizeof(int), &attacked_socket, sizeof(int));
		PacketManger::Send(client->socket, H_ATTACK_CLIENT, packet_data.data(), packet_data.size());
	}
}

void GameSession::NotifyAttackResulttoStructure(int client_socket, int chan, int room, int attacked_index)
{
	for (auto client : client_list_room)
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
		list<Structure*>& structures_in_room = structure_list_room;
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

	if (kind == 0 && GameManager::clients_info[killer]->kill >= 1) // TEST: 승리 조건
	{
		SendVictory(GameManager::clients_info[killer]->team, chan, room);
		return;
	}

	AttInfo info;
	info.attacked = client_socket;
	info.attacker = killer;

	auto& assistStack = GameManager::clients_info[client_socket]->assistList;
	std::stack<std::pair<int, int>> tempStack;

	while (!assistStack.empty()) {
		auto assistPair = assistStack.top();
		assistStack.pop();

		int assistSocket = assistPair.first;
		int assistTime = assistPair.second;

		intptr_t timerId = static_cast<intptr_t>(assistSocket);
		Timer::AddTimer(
			timerId,
			[client_socket, assistSocket]() {
				auto& assistStack = GameManager::clients_info[client_socket]->assistList;
				std::stack<std::pair<int, int>> tempStack;

				while (!assistStack.empty()) {
					if (assistStack.top().first != assistSocket) {
						tempStack.push(assistStack.top());
					}
					assistStack.pop();
				}

				while (!tempStack.empty()) {
					assistStack.push(tempStack.top());
					tempStack.pop();
				}

				cout << "어시스트에서 제거됨: " << assistSocket << endl;
			}, 60000);

		tempStack.push({ assistSocket, assistTime });
		// 1min이 지나면 어시스트가 사라진다
	}

	while (!tempStack.empty()) {
		assistStack.push(tempStack.top());
		tempStack.pop();
	}


	chrono::time_point<chrono::system_clock> starttime = startTime;
	chrono::time_point<chrono::system_clock> currentTime = chrono::system_clock::now();
	chrono::duration<double> elapsed_seconds = currentTime - starttime;

	int gameMinutes = static_cast<int>(elapsed_seconds.count() / 60);
	int respawnTime = 3 + (gameMinutes * 3); //min 3s

	for (auto inst : client_list_room) {
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

	std::cout << "Respawning AddTimer: " << client_socket << " " << respawnTime * 1000 << std::endl;

	Timer::AddTimer(timerId, [this, client_socket]() {
		if (GameManager::clients_info[client_socket] == nullptr) {
			std::cout << "Client disconnected during respawn wait: " << client_socket << std::endl;
			Timer::RemoveTimer(static_cast<intptr_t>(client_socket));
			return;
		}

		std::cout << "Respawning client: " << client_socket << std::endl;
		ClientRespawn(client_socket);
		}, respawnTime * 1000);
}

void GameSession::ClientRespawn(int client_socket) {
	{
		// unique_lock<shared_mutex> lock(room_mutex);
		for (auto inst : client_list_room)
		{
			if (inst->socket == client_socket) {
				inst->curhp = inst->maxhp;
				ClientStat(client_socket);
			}

			BYTE* packet_data = new BYTE[sizeof(int)];
			memcpy(packet_data, &client_socket, sizeof(int));
			PacketManger::Send(inst->socket, H_CLIENT_RESPAWN, packet_data, sizeof(int));
			delete[] packet_data;
		}
	}
}


//-----------------------------------------------------------------------------------
void GameSession::ItemStat(Client* client, Item info)
{
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
		auto nonZeroIt = find_if(client->itemList.begin(), client->itemList.end(),
			[](int item) { return item == 0; });

		if (nonZeroIt != client->itemList.end())
			index = std::distance(client->itemList.begin(), nonZeroIt);
		else index = client->itemList.size();

		if (isPerchase)
		{
			// cout << index << " item" << endl;

			if (index >= client->itemList.size()) // 더 이상 추가 아이템을 구매할 수 없을 때
			{
				cout << "꽉 찼어." << endl;
				return;
			}
			else if (client->gold >= NeedGold)
			{
				client->gold -= NeedGold;
				client->maxhp += (*curItem).maxhp;
				client->attack += (*curItem).attack;
				client->maxdelay += (*curItem).maxdelay;
				client->attspeed += (*curItem).attspeed;
				client->movespeed += (*curItem).movespeed;
				client->criProbability += (*curItem).criProbability;
				client->absorptionRate += (*curItem).absorptionRate;
				client->defense += (*curItem).defense;

				cout << client->socket << "님이 " << (*curItem).name << " 를 " << NeedGold << "에 구매하는데 성공했습니다." << endl;

				client->itemList[index] = ((*curItem).id); // 아이템 추가

				ClientStat(client->socket);
			}
			else
			{
				cout << "show me the money" << endl;
			}
		}
		else
		{
			for (int i = 0; i < client->itemList.size(); i++)
			{
				if (client->itemList[i] == id)
				{
					client->gold += NeedGold * 0.8f;
					client->maxhp -= (*curItem).maxhp;
					client->attack -= (*curItem).attack;
					client->maxdelay -= (*curItem).maxdelay;
					client->attspeed -= (*curItem).attspeed;
					client->movespeed -= (*curItem).movespeed;
					client->criProbability -= (*curItem).criProbability;
					client->absorptionRate -= (*curItem).absorptionRate;
					client->defense -= (*curItem).defense;

					cout << client->socket << "님이 " << (*curItem).name << " 를 " << NeedGold * 0.8f << "에 판매하는데 성공했습니다." << endl;

					client->itemList[i] = 0; // 아이템 삭제

					ClientStat(client->socket);
					break;
				}
			}
		}
	}
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

void GameSession::Champ1Passive(int client_socket, AttInfo info, int chan, int room) {
	Client* attacker = nullptr;
	{
		std::shared_lock<std::shared_mutex> lock(room_mutex);
		auto& clients_in_room = client_list_room;

		auto attacker_it = std::find_if(clients_in_room.begin(), clients_in_room.end(),
			[client_socket](Client* client) { return client->socket == client_socket; });

		if (attacker_it == clients_in_room.end()) {
			std::cout << "Attacker not found in room" << std::endl;
			return;
		}

		attacker = *attacker_it;

		if (info.kind == -1) {
			Client* attacked = nullptr;
			auto attacked_it = std::find_if(clients_in_room.begin(), clients_in_room.end(),
				[&info](Client* client) { return client->socket == info.attacked; });

			if (attacked_it == clients_in_room.end() || (*attacked_it)->curhp <= 0) {
				std::cout << "Attacked client not found or already dead" << std::endl;
				return;
			}

			attacked = *attacked_it;
			int damage = static_cast<int>(attacker->attack * attacker->level * 5);
			std::cout << "Client passive damage: " << damage << " attacked.curHp: " << attacked->curhp << std::endl;
			ApplyDamage(attacked, damage);


			NotifyAttackResulttoClient(client_socket, chan, room, attacked->socket);

			if (attacked->curhp <= 0) {
				ClientDie(attacked->socket, client_socket, 0);
			}
			ClientStat(attacked->socket);
		}
		else {
			Structure* attacked = nullptr;
			int team = attacker->team;

			auto& structures_in_room = structure_list_room;
			auto attacked_it = std::find_if(structures_in_room.begin(), structures_in_room.end(),
				[&info, &team](Structure* struc) { return struc->team != team && struc->index == info.attacked && struc->struct_kind == info.kind; });

			if (attacked_it == structures_in_room.end()) {
				std::cout << "Structure not found or mismatched kind" << std::endl;
				return;
			}

			attacked = *attacked_it;

			if (attacked->curhp <= 0) {
				std::cout << "Attacked structure already dead" << std::endl;
				return;
			}

			int damage = static_cast<int>(attacker->attack * attacker->level * 5);
			attacked->curhp -= damage;
			std::cout << "Structure passive damage: " << damage << " attacked.curHp: " << attacked->curhp << std::endl;

			NotifyAttackResulttoStructure(client_socket, chan, room, attacked->index);
			structureManager->StructureStat(attacked->index, attacked->team, attacked->struct_kind, chan, room);
		}
	}
}

void GameSession::ChampStatusEffect(int client_socket, std::string field, int value, int delayTime) {
	Client* client = GameManager::clients_info[client_socket];

	if (!client) {
		std::cerr << "Invalid client socket: " << client_socket << std::endl;
		return;
	}

	intptr_t timerId = static_cast<intptr_t>(client_socket);
	int interval = 1000;
	int totalTicks = delayTime;

	Timer::AddTimer(timerId, [timerId, client, field, value, delayTime, totalTicks, interval]() mutable {
		if (totalTicks <= 0) {
			Timer::RemoveTimer(timerId);

			if (field == "Speed") {
				client->movespeed -= value;
			}
			else if (field == "Stun") {
				// todo.
			}

			return;
		}

		if (field == "Health") {
			client->curhp -= value;
			if (client->curhp < 0) client->curhp = 0;
		}
		else if (field == "Speed") {
			if (totalTicks == delayTime) {
				client->movespeed -= value;
				if (client->movespeed < 0) client->movespeed = 0;
			}
		}
		else if (field == "Stun") {
			if (totalTicks == delayTime) {
				// todo. 
			}
		}

		totalTicks--;
		if (totalTicks > 0) Timer::AddTimer(timerId, [client, field, value, totalTicks, interval, timerId]() mutable {}, interval);
		}, interval);
}

void GameSession::BulletStat(int client_socket, int bullet_index) {
	for (auto bullet : bullet_list_room) {
		if (bullet->index == bullet_index) {
			std::shared_lock<std::shared_mutex> lock(room_mutex);

			for (Client* inst : client_list_room)
				PacketManger::Send(inst->socket, H_BULLET_STAT, bullet, sizeof(Bullet));

			return;
		}
	}
}

void GameSession::GetChampInfo(int client_socket) {
	shared_lock<shared_mutex> lock(room_mutex);

	for (auto cli : client_list_room) {
		if (cli->socket == client_socket) {
			for (auto& champion : ChampionSystem::champions) {
				ChampionStatsInfo championInfo = ChampionSystem::GetChampionInfo(champion);
				std::cout << "Champion Sent - Index: " << championInfo.index << ", Attack: " << championInfo.attack << "\n";

				PacketManger::Send(cli->socket, H_CHAMPION_INFO, &championInfo, sizeof(ChampionStatsInfo));
			}
			return;
		}
	}
	std::cout << "Client not found: " << client_socket << "\n";
}

void GameSession::GetItemInfo(int client_socket) {
	for (auto cli : client_list_room) {
		shared_lock<shared_mutex> lock(room_mutex);
		if (cli->socket == client_socket) {
			for (auto& item : ItemSystem::items) {
				ItemStatsInfo itemInfo = ItemSystem::GetItemInfo(item);
				std::cout << "Item Sent - ID: " << itemInfo.id << ", Gold: " << itemInfo.gold << "\n";
				PacketManger::Send(cli->socket, H_ITEM_INFO, &itemInfo, sizeof(ItemStatsInfo));
			}
			return;
		}
	}
	std::cout << "Client not found: " << client_socket << "\n";
}