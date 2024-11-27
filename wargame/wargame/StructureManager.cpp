#include "StructureManager.h"
#include "PacketManager.h"
#include "Utility.h"
#include "GameSession.h"
#include "GameManager.h"
#include "Timer.h"

#include<unordered_map>
#include<atomic>
#include<mutex>
#include <thread>

std::unordered_map<int, std::atomic<bool>> turretSearchStopMap;
std::shared_mutex StructureManager::structure_mutex;


void StructureManager::NewStructure(int index, int team, int struct_kind, int chan, int room, int x, int y, int z)
{
	Structure* temp_ = new Structure;
	temp_->index = index;
	temp_->struct_kind = struct_kind;
	temp_->x = x;
	temp_->y = y;
	temp_->z = z;
	temp_->team = team;

	if (temp_->struct_kind == 2) {//gate
		temp_->maxhp = 1500;
		temp_->curhp = temp_->maxhp;
		temp_->attrange = 0;
		temp_->bulletspeed = 0;
		temp_->bulletdmg = 0;
	}
	else if (temp_->struct_kind == 1) { //turret
		temp_->maxhp = 1500;
		temp_->curhp = temp_->maxhp;
		temp_->attrange = 30;
		temp_->bulletspeed = 20;
		temp_->bulletdmg = 50;
		temp_->curdelay = 0;
		temp_->maxdelay = 2;
	}
	else if (temp_->struct_kind == 0) {//nexus
		temp_->maxhp = 2000;
		temp_->curhp = temp_->maxhp;
		temp_->attrange = 0;
		temp_->bulletspeed = 0;
		temp_->bulletdmg = 0;
	}

	{
		std::unique_lock<std::shared_mutex> lock(structure_mutex);
		GameSession::structure_list_room.push_back(temp_);
	}

	StructureInfo info;
	info.index = temp_->index;
	info.struct_kind = temp_->struct_kind;
	info.team = temp_->team;
	info.x = temp_->x;
	info.y = temp_->y;
	info.z = temp_->z;

	info.curhp = temp_->curhp;
	info.maxhp = temp_->maxhp;
	info.attrange = temp_->attrange;
	info.bulletspeed = temp_->bulletspeed;
	info.bulletdmg = temp_->bulletdmg;

	{
		std::shared_lock<std::shared_mutex> lock(GameSession::room_mutex);
		for (Client* inst : GameSession::client_list_room)
			PacketManger::Send(inst->socket, H_STRUCTURE_CREATE, &info, sizeof(StructureInfo));
	}

	if (temp_->struct_kind == 1) {
		TurretSearch(index, chan, room);
	}
}

void StructureManager::StructureDie(int index, int team, int struct_kind, int chan, int room)
{

	auto structure_list = GameSession::structure_list_room;
	for (auto inst : structure_list)
	{
		if (inst->team == team && inst->struct_kind == struct_kind && inst->index == index)
			GameSession::structure_list_room.remove(inst);
	}


	{
		std::shared_lock<std::shared_mutex> lock(GameSession::room_mutex);
		for (auto inst : GameSession::client_list_room)
			PacketManger::Send(inst->socket, H_STRUCTURE_DIE, &index, sizeof(int));
	}

	if (struct_kind == 1)
		StopTurretSearch(index);
}

void StructureManager::StructureStat(int index, int team, int struct_kind, int chan, int room) {
	Structure* stru_ = nullptr;

	{
		std::unique_lock<std::shared_mutex> lock(structure_mutex);
		for (auto inst : GameSession::structure_list_room)
		{
			if (inst->team == team && inst->struct_kind == struct_kind && inst->index == index)
			{

				stru_ = inst;

				if (inst->curhp <= 0)
				{
					inst->curhp = 0;
					inst->index = -1;
					StructureDie(index, inst->team, inst->struct_kind, chan, room);
					return;
				}

				break;
			}
		}
	}

	if (stru_ == nullptr) {
		std::cout << "stru_ nullptr" << std::endl;
		return;
	}

	StructureInfo info;
	info.index = stru_->index;
	info.team = stru_->team;
	info.struct_kind = stru_->struct_kind;
	info.curhp = stru_->curhp;
	info.maxhp = stru_->maxhp;
	info.attrange = stru_->attrange;
	info.bulletdmg = stru_->bulletdmg;
	info.bulletspeed = stru_->bulletspeed;


	{
		std::shared_lock<std::shared_mutex> lock(GameSession::room_mutex);
		for (Client* inst : GameSession::client_list_room)
			PacketManger::Send(inst->socket, H_STRUCTURE_STAT, &info, sizeof(StructureInfo));
	}
}
//-----------------------------------------------------------------------------------
void StructureManager::TurretSearch(int index, int chan, int room) {
	std::list<Client*>& clients_in_room = GameSession::client_list_room;
	std::list<Structure*>& structures_in_room = GameSession::structure_list_room;

	auto attacker = std::find_if(structures_in_room.begin(), structures_in_room.end(), [index](Structure* struc) {
		return struc->index == index;
		});

	if (attacker != structures_in_room.end()) {
		for (auto client : clients_in_room) {
			if (client->team == (*attacker)->team) {
				// cout << "Turret's team in client." << endl;
				continue;
			}

			if (turretSearchStopMap[index]) {
				std::cout << "turretSearchStopMap[index] is none." << std::endl;
				break;
			}

			if (client == nullptr) {
				std::cout << "StopTurretSearch" << std::endl;
				StopTurretSearch(index);
				break;
			}

			float distance = UtilityManager::DistancePosition((*attacker)->x, (*attacker)->y, (*attacker)->z, client->x, client->y, client->z);

			if (distance <= (*attacker)->attrange) {
				int attacked_ = client->socket;

				if (GameManager::clients_info[attacked_] == nullptr) {
					GameManager::ClientClose(attacked_);
					return;
				}

				auto currentTime = std::chrono::high_resolution_clock::now();
				auto elapsedTime = std::chrono::duration_cast<std::chrono::milliseconds>(currentTime - (*attacker)->lastUpdateTime).count();

				if (elapsedTime >= (*attacker)->maxdelay) {
					TurretBullet* newBullet = new TurretBullet;
					newBullet->x = (*attacker)->x;
					newBullet->y = (*attacker)->y;
					newBullet->z = (*attacker)->z;
					newBullet->dmg = (*attacker)->bulletdmg;
					StructureManager::TurretShot(index, newBullet, attacked_, chan, room);
					(*attacker)->lastUpdateTime = currentTime;
				}
			}
		}
	}

	Timer::AddTimer(index, [index, chan, room]() {
		TurretSearch(index, chan, room);
	}, 1000);
}

void StructureManager::TurretShot(int index, TurretBullet* newBullet, int attacked_, int chan, int room) {
	std::list<Client*>& clients_in_room = GameSession::client_list_room;
	std::list<Structure*>& structures_in_room = GameSession::structure_list_room;

	auto attacked = std::find_if(clients_in_room.begin(), clients_in_room.end(), [attacked_](Client* client) {
		return client->socket == attacked_;
		});

	auto attacker = std::find_if(structures_in_room.begin(), structures_in_room.end(), [index](Structure* struc) {
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

		float distance = std::sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
		directionX /= distance;
		directionY /= distance;
		directionZ /= distance;

		float moveDistance = (*attacker)->bulletspeed;

		if (GameManager::clients_info[attacked_] == nullptr) {
			GameManager::ClientClose(attacked_);
			return;
		}

		std::cout << "x: " << targetX << ", y: " << targetY << ", z: " << targetZ << ", dirX: " << directionX << ", dirY: " << directionY << ", dirZ: " << directionZ << std::endl;
		BulletInfo bulletInfo{ targetX, targetY, targetZ, directionX, directionY, directionZ, moveDistance };
		StructureManager::MoveBulletAsync(newBullet, bulletInfo, *attacked, *attacker);
	}
}

void StructureManager::MoveBulletAsync(TurretBullet* newBullet, BulletInfo bulletInfo, Client* attacked, Structure* attacker) {
	std::cout << "distance : " << UtilityManager::DistancePosition(newBullet->x, newBullet->y, newBullet->z, attacked->x, attacked->y, attacked->z) << std::endl;
	if (UtilityManager::DistancePosition(newBullet->x, newBullet->y, newBullet->z, attacked->x, attacked->y, attacked->z) <= 10) { //23error
		int prev_curhp = attacked->curhp;
		attacked->curhp -= newBullet->dmg;
		GameSession::ClientStat(attacked->socket);

		std::cout << attacker->index << "번 포탑의 공격 :: " << attacked->socket << "'s Hp : " << prev_curhp << " -> " << attacked->curhp << std::endl;
		attacker->curdelay = 0;

		if (attacked->curhp <= 0) {
			GameSession::ClientDie(attacked->socket, attacker->index, 1);
		}
		delete newBullet;
	}
	else {
		std::cout << "Shot Async - x:" << newBullet->x << ", y: " << newBullet->y << ", z: " << newBullet->z << std::endl;
		newBullet->x += bulletInfo.directionX * bulletInfo.moveDistance;
		newBullet->y += bulletInfo.directionY * bulletInfo.moveDistance;
		newBullet->z += bulletInfo.directionZ * bulletInfo.moveDistance;
		std::cout << "next Position - x:" << newBullet->x << ", y: " << newBullet->y << ", z: " << newBullet->z << std::endl;


		Timer::AddTimer(reinterpret_cast<intptr_t>(newBullet), [newBullet, bulletInfo, attacked, attacker]() mutable {
			StructureManager::MoveBulletAsync(newBullet, bulletInfo, attacked, attacker);
			}, 20);
	}
}

void StructureManager::StopTurretSearch(int index) {
	turretSearchStopMap[index] = true;
}