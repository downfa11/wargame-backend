#include "StructureManager.h"

#include "GameSession.h"
#include "GameManager.h"
#include "PacketManager.h"
#include "Utility.h"
#include "Timer.h"
#include "Unit.h"

#include<mutex>

#define COLLISION_BULLET 3
#define TURRET_ATTACK_TIMER 100

void StructureManager::TurretSearch(int index) {
	std::list<Client*>& clients_in_room = session->client_list_room;
	std::list<Unit*>& unitss_in_room = session->unit_list_room;
	std::list<Structure*>& structures_in_room = session->structure_list_room;

	auto attacker = FindStructureByIndex(index, structures_in_room);
	if (!attacker) {
		std::cout << "Turret not found: " << index << std::endl;
		return;
	}

	for (auto client : clients_in_room) {
		if (client->team == attacker->team) continue;
		if (!client) continue;

		float distance = UtilityManager::DistancePosition(attacker->x, attacker->y, attacker->z, client->x, client->y, client->z);

		if (distance <= attacker->attrange) {
			int attacked_ = client->socket;

			if (GameManager::clients_info[attacked_] == nullptr) {
				GameManager::ClientClose(attacked_);
				continue;
			}

			auto currentTime = std::chrono::high_resolution_clock::now();
			auto elapsedTime = std::chrono::duration_cast<std::chrono::milliseconds>(currentTime - attacker->lastUpdateTime).count();

			if (elapsedTime >= attacker->maxdelay) {
				CreateBullet(attacker, client);
				attacker->lastUpdateTime = currentTime;
			}
		}
	}

	// todo. Unit도 탐색해야한다.

	Timer::AddTimer([this, index]() { TurretSearch(index); }, attacker->maxdelay * 1000);
}

Structure* StructureManager::FindStructureByIndex(int index, std::list<Structure*>& structures) {
	auto it = std::find_if(structures.begin(), structures.end(), [index](Structure* struc) {
		return struc->index == index;
		});
	return (it != structures.end()) ? *it : nullptr;
}

Client* StructureManager::FindClientBySocket(int socket, std::list<Client*>& clients) {
	auto it = std::find_if(clients.begin(), clients.end(), [socket](Client* client) {
		return client->socket == socket;
		});
	return (it != clients.end()) ? *it : nullptr;
}

void StructureManager::CreateBullet(Structure* attacker, Client* target) {
	Bullet* newBullet = new Bullet;
	newBullet->index = attackCount++;
	newBullet->type = 0;
	newBullet->targetIndex = target->socket;
	newBullet->targetType = 0; // todo. 이 경우에는 포탑이 클라이언트를 향하고 있기에 type이 0이다.
	newBullet->speed = attacker->bulletspeed;
	newBullet->demage = attacker->bulletdmg;

	float directionX = target->x - attacker->x;
	float directionY = target->y - (attacker->y + (2 * 8));
	float directionZ = target->z - attacker->z;

	float distance = std::sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
	directionX /= distance;
	directionY /= distance;
	directionZ /= distance;

	newBullet->x = attacker->x + (directionX * 0.5 * 8);
	newBullet->y = attacker->y + (2 * 8);
	newBullet->z = attacker->z + (directionZ * 0.5 * 8);

	{
		std::unique_lock<std::shared_mutex> lock(session->bullet_mutex);
		session->bullet_list_room.push_back(newBullet);
	}

	{
		std::shared_lock<std::shared_mutex> lock(session->room_mutex);
		for (Client* inst : session->client_list_room)
			PacketManger::Send(inst->socket, H_BULLET_CREATE, newBullet, sizeof(Bullet));
	}

	StructureManager::MoveBulletAsync(newBullet, target, attacker);
}

void StructureManager::CreateBullet(Structure* attacker, Unit* target) {
	Bullet* newBullet = new Bullet;
	newBullet->index = attackCount++;
	newBullet->type = 0;
	newBullet->targetIndex = target->index;
	newBullet->targetType = 2; // todo. 이 경우에는 포탑이 클라이언트를 향하고 있기에 type이 2이다.
	newBullet->speed = attacker->bulletspeed;
	newBullet->demage = attacker->bulletdmg;

	float directionX = target->x - attacker->x;
	float directionY = target->y - (attacker->y + (2 * 8));
	float directionZ = target->z - attacker->z;

	float distance = std::sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
	directionX /= distance;
	directionY /= distance;
	directionZ /= distance;

	newBullet->x = attacker->x + (directionX * 0.5 * 8);
	newBullet->y = attacker->y + (2 * 8);
	newBullet->z = attacker->z + (directionZ * 0.5 * 8);

	{
		std::unique_lock<std::shared_mutex> lock(session->bullet_mutex);
		session->bullet_list_room.push_back(newBullet);
	}

	{
		std::shared_lock<std::shared_mutex> lock(session->room_mutex);
		for (Client* inst : session->client_list_room)
			PacketManger::Send(inst->socket, H_BULLET_CREATE, newBullet, sizeof(Bullet));
	}

	StructureManager::MoveBulletAsync(newBullet, target, attacker);
}

void StructureManager::MoveBulletAsync(Bullet* newBullet, Client* attacked, Structure* attacker) {
	if (attacked->curhp <= 0) {
		std::cout << "Target already dead: Stopping bullet." << std::endl;
		delete newBullet;
		return;
	}

	float dx = attacked->x - newBullet->x;
	float dy = attacked->y - newBullet->y;
	float dz = attacked->z - newBullet->z;
	float currentDistance = UtilityManager::DistancePosition(attacked->x, attacked->y, attacked->z, newBullet->x, newBullet->y, newBullet->z);
	// std::cout << newBullet->index << " : " << currentDistance << std::endl;


	if (currentDistance <= COLLISION_BULLET) {
		{
			std::shared_lock<std::shared_mutex> lock(session->room_mutex);
			for (Client* inst : session->client_list_room)
				PacketManger::Send(inst->socket, H_BULLET_DIE, newBullet, sizeof(Bullet));
		}


		int prev_hp = attacked->curhp;

		double defenseMultiplier = (attacked->defense >= 100) ? 0.0 : (1.0 - attacked->defense / 100.0);
		int changedDamage = static_cast<int>(newBullet->demage * defenseMultiplier);
		attacked->curhp -= changedDamage;

		std::cout << "Structure의 Bullet 원래 데미지는 " << newBullet->demage << "인데, 방어력이 " << attacked->defense << "이므로 깎여서 " << changedDamage << "만 깎였어." << std::endl;

		session->ClientStat(attacked->socket);

		std::cout << "Turret " << attacker->index << " attacks: Target " << attacked->socket << " HP: " << prev_hp << " -> " << attacked->curhp << std::endl;

		if (attacked->curhp <= 0) {
			session->ClientDie(attacked->socket, attacker->index, Target::STRUCTURE);
		}

		delete newBullet;
		return;
	}

	float distance = currentDistance;
	if (distance > COLLISION_BULLET) {
		float directionX = dx / distance;
		float directionY = dy / distance;
		float directionZ = dz / distance;

		float moveDistance = newBullet->speed;// *TURRET_ATTACK_TIMEER / 1000.0f;
		if (moveDistance >= distance) {
			newBullet->x = attacked->x;
			newBullet->y = attacked->y;
			newBullet->z = attacked->z;
		}
		else {
			newBullet->x += directionX * moveDistance;
			newBullet->y += directionY * moveDistance;
			newBullet->z += directionZ * moveDistance;
		}

		Timer::AddTimer(reinterpret_cast<intptr_t>(newBullet), [this, newBullet, attacked, attacker]() {
			MoveBulletAsync(newBullet, attacked, attacker);
			}, TURRET_ATTACK_TIMER);
	}
}

void StructureManager::MoveBulletAsync(Bullet* newBullet, Unit* attacked, Structure* attacker) {
	if (attacked->curhp <= 0) {
		std::cout << "Target already dead: Stopping bullet." << std::endl;
		delete newBullet;
		return;
	}

	float dx = attacked->x - newBullet->x;
	float dy = attacked->y - newBullet->y;
	float dz = attacked->z - newBullet->z;
	float currentDistance = UtilityManager::DistancePosition(attacked->x, attacked->y, attacked->z, newBullet->x, newBullet->y, newBullet->z);
	// std::cout << newBullet->index << " : " << currentDistance << std::endl;


	if (currentDistance <= COLLISION_BULLET) {
		{
			std::shared_lock<std::shared_mutex> lock(session->room_mutex);
			for (Client* inst : session->client_list_room)
				PacketManger::Send(inst->socket, H_BULLET_DIE, newBullet, sizeof(Bullet));
		}

		int prev_hp = attacked->curhp;
		attacked->curhp -= newBullet->demage;
		session->unitManager->UnitStat(attacked->client_socket, attacked->index, attacked->unit_kind);

		std::cout << "Turret " << attacker->index << " attacks: Target " << attacked->index << " HP: " << prev_hp << " -> " << attacked->curhp << std::endl;

		if (attacked->curhp <= 0) {
			session->unitManager->UnitDie(attacked->client_socket, attacker->index, UnitKind::Archer);
		}

		delete newBullet;
		return;
	}

	float distance = currentDistance;
	if (distance > COLLISION_BULLET) {
		float directionX = dx / distance;
		float directionY = dy / distance;
		float directionZ = dz / distance;

		float moveDistance = newBullet->speed;// *TURRET_ATTACK_TIMEER / 1000.0f;
		if (moveDistance >= distance) {
			newBullet->x = attacked->x;
			newBullet->y = attacked->y;
			newBullet->z = attacked->z;
		}
		else {
			newBullet->x += directionX * moveDistance;
			newBullet->y += directionY * moveDistance;
			newBullet->z += directionZ * moveDistance;
		}

		Timer::AddTimer(reinterpret_cast<intptr_t>(newBullet), [this, newBullet, attacked, attacker]() {
			MoveBulletAsync(newBullet, attacked, attacker);
			}, TURRET_ATTACK_TIMER);
	}
}