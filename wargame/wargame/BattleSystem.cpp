#include "GameSession.h"

#include "GameManager.h"
#include "PacketManager.h"
#include "Utility.h"
#include "Timer.h"

#include<shared_mutex>


void GameSession::AttackClient(Client* client, AttInfo info) {
	if (!client) {
		return;
	}

	int client_socket = client->socket;
	int chan = client->channel;
	int room = client->room;
	int team = client->team;

	if (team == -1) {
		std::cout << "AttackClient team error" << std::endl;
		return;
	}

	Client* attacker = nullptr;
	Client* attacked = nullptr;

	{
		std::shared_lock<std::shared_mutex> lock(room_mutex);
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

		Champ1Passive(attacker, info);

		int damage = CalculateDamage(attacker);
		ApplyDamage(attacked, damage);
		ApplyAbsorption(attacker, damage);

		attacker->curdelay = 0;
		NotifyAttackResult(client->socket, attacked->socket, Target::CLIENT);
	}

	if (!attacker || !attacked) {
		std::cout << "AttackClient lock error" << std::endl;
		return;
	}

	if (attacked->curhp <= 0) {
		ClientDie(attacked->socket, client->socket, Target::CLIENT);
	}

	ClientStat(attacked->socket);
}

//todo
void GameSession::AttackStructure(Client* client, AttInfo info) {
	if (!client || client->team == -1) {
		std::cout << "Invalid client or team error in AttackStructure" << std::endl;
		return;
	}

	int client_socket = client->socket;
	int chan = client->channel;
	int room = client->room;
	int team = client->team;


	Client* attacker = nullptr;
	Structure* attacked = nullptr;

	{
		std::shared_lock<std::shared_mutex> lock(room_mutex);

		auto& clients_in_room = client_list_room;
		auto& structures_in_room = structure_list_room;

		auto attacker_it = find_if(clients_in_room.begin(), clients_in_room.end(),
			[client_socket](Client* client) { return client->socket == client_socket; });

		auto attacked_it = find_if(structures_in_room.begin(), structures_in_room.end(),
			[&info, &team](Structure* struc) { return (struc->team != team && struc->index == info.attacked && struc->struct_kind == info.object_kind); });

		if (attacker_it == clients_in_room.end() || attacked_it == structures_in_room.end() || (*attacked_it)->curhp <= 0) {
			std::cout << "none struc :" << info.attacked << ", struc kind :" << info.object_kind << std::endl;
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

		Champ1Passive(attacker, info);

		int damage = CalculateDamage(attacker);
		attacked->curhp -= damage;


		structureManager->StructureStat(attacked);
		attacker->curdelay = 0;
		NotifyAttackResult(client->socket, attacked->index, Target::STRUCTURE);
	}
}

void GameSession::AttackUnit(Client* client, AttInfo info) {
	if (!client || client->team == -1) {
		std::cout << "Invalid client or team error in AttackStructure" << std::endl;
		return;
	}

	int client_socket = client->socket;
	int chan = client->channel;
	int room = client->room;
	int team = client->team;


	Client* attacker = nullptr;
	Unit* attacked = nullptr;

	{
		std::shared_lock<std::shared_mutex> lock(room_mutex);

		auto& clients_in_room = client_list_room;
		auto& units_list_room = unit_list_room;

		auto attacker_it = find_if(clients_in_room.begin(), clients_in_room.end(),
			[client_socket](Client* client) { return client->socket == client_socket; });

		auto attacked_it = find_if(units_list_room.begin(), units_list_room.end(),
			[&info, &team](Unit* unit) { return (unit->team != team && unit->index == info.attacked); });

		if (attacker_it == clients_in_room.end() || attacked_it == units_list_room.end() || (*attacked_it)->curhp <= 0) {
			std::cout << "none unit :" << info.attacked << ", unit kind :" << info.object_kind << std::endl;
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

		Champ1Passive(attacker, info);

		int damage = CalculateDamage(attacker);
		attacked->curhp -= damage;


		unitManager->UnitStat(attacked);
		attacker->curdelay = 0;
		NotifyAttackResult(client->socket, attacked->index, Target::UNIT);
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

bool GameSession::IsValidAttackRange(Client* attacker, const Unit* target) {
	float distance = UtilityManager::DistancePosition(attacker->x, attacker->y, attacker->z, target->x, target->y, target->z);
	return distance <= attacker->attrange + 1;
}

void GameSession::MouseSearchToTarget(Client* attacker, Client* attacked) {
	MouseInfo info{};
	info.x = attacked->x;
	info.y = attacked->y;
	info.z = attacked->z;
	info.kind = Target::CLIENT;
	MouseSearch(attacker, info);
}

void GameSession::MouseSearchToTarget(Client* attacker, Structure* attacked) {
	MouseInfo info{};
	info.x = attacked->x;
	info.y = attacked->y;
	info.z = attacked->z;
	info.kind = Target::STRUCTURE;
	MouseSearch(attacker, info);
}

void GameSession::MouseSearchToTarget(Client* attacker, Unit* attacked) {
	MouseInfo info{};
	info.x = attacked->x;
	info.y = attacked->y;
	info.z = attacked->z;
	info.kind = Target::UNIT;
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
		std::cout << "Warning: absorptionRate greater than 1 for " << attacker->socket << std::endl;
		attacker->absorptionRate = 1.0;
	}

	int health = static_cast<int>(damage * attacker->absorptionRate);
	attacker->curhp = ((attacker->curhp + health) < (attacker->maxhp)) ? (attacker->curhp + health) : (attacker->maxhp);
	ClientStat(attacker->socket);
}

void GameSession::UpdateClientDelay(Client* client)
{
	auto currentTime = std::chrono::high_resolution_clock::now();
	client->curdelay += std::chrono::duration<float>(currentTime - client->lastUpdateTime).count();
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

void GameSession::NotifyAttackResult(int client_socket, int attacked, Target targetType)
{
	auto it = TargetHeader.find(targetType);
	if (it == TargetHeader.end()) {
		std::cerr << "Invalid target type: " << static_cast<int>(targetType) << std::endl;
		return;
	}

	for (auto client : client_list_room)
	{
		std::vector<BYTE> packet_data(sizeof(int) * 2);
		memcpy(packet_data.data(), &client_socket, sizeof(int));
		memcpy(packet_data.data() + sizeof(int), &attacked, sizeof(int));
		PacketManger::Send(client->socket, it->second, packet_data.data(), packet_data.size());
	}
}

// todo
void GameSession::ClientDie(int client_socket, int killer, Target kind) {
	// kind : killer's kind ( client:0, structure:1 )
	int chan = GameManager::clients_info[client_socket]->channel;
	int room = GameManager::clients_info[client_socket]->room;

	GameManager::clients_info[client_socket]->death += 1;
	ClientStat(client_socket);

	if (kind == Target::CLIENT)
	{
		GameManager::clients_info[killer]->kill += 1;
		std::cout << GameManager::clients_info[killer]->socket << "의 킬 : " << GameManager::clients_info[killer]->kill << std::endl;
		ClientStat(killer);
	}
	else if (kind == Target::STRUCTURE) {
		std::list<Structure*>& structures_in_room = structure_list_room;
		auto attacker = std::find_if(structures_in_room.begin(), structures_in_room.end(), [killer](Structure* struc) {
			return struc->index == killer;
			});
		std::cout << (*attacker)->index << "의 킬 : 포탑의 킬 수는 카운트하지 않습니다." << std::endl;
	}
	else {
		std::cout << "이게머노" << std::endl;
		return;
	}

	std::cout << GameManager::clients_info[client_socket]->socket << "의 데스 : " << GameManager::clients_info[client_socket]->death << std::endl;

	if (kind == Target::CLIENT && GameManager::clients_info[killer]->kill >= 1) // TEST: 승리 조건
	{
		SendVictory(GameManager::clients_info[killer]->team);
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

				std::cout << "어시스트에서 제거됨: " << assistSocket << std::endl;
			}, 60000);

		tempStack.push({ assistSocket, assistTime });
		// 1min이 지나면 어시스트가 사라진다
	}

	while (!tempStack.empty()) {
		assistStack.push(tempStack.top());
		tempStack.pop();
	}


	std::chrono::time_point<std::chrono::system_clock> starttime = startTime;
	std::chrono::time_point<std::chrono::system_clock> currentTime = std::chrono::system_clock::now();
	std::chrono::duration<double> elapsed_seconds = currentTime - starttime;

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

	std::cout << client_socket << " 님이 사망 대기 시간은 " << respawnTime << "초입니다." << std::endl;

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
	std::cout << "level up " << client->socket << std::endl;
	ClientStat(client->socket);
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
