#include "StructureManager.h"

#include "GameSession.h"
#include "PacketManager.h"
#include "Utility.h"
#include "Timer.h"

#include<mutex>

#define COLLISION_BULLET 3
#define TURRET_ATTACK_TIMER 100

void StructureManager::NewStructure(int index, int team, StructureKind struct_kind, int x, int y, int z)
{
	Structure* temp_ = new Structure;
	temp_->index = index;
	temp_->struct_kind = struct_kind;
	temp_->x = x;
	temp_->y = y;
	temp_->z = z;
	temp_->team = team;

	switch (struct_kind) {
	case StructureKind::GATE:
		temp_->maxhp = 1500;
		temp_->curhp = temp_->maxhp;
		temp_->attrange = 0;
		temp_->bulletspeed = 0;
		temp_->bulletdmg = 0;
		temp_->defense = 40;
		break;
	case StructureKind::TURRET:
		temp_->maxhp = 1500;
		temp_->curhp = temp_->maxhp;
		temp_->attrange = 30;
		temp_->bulletspeed = 20;
		temp_->bulletdmg = 50;
		temp_->maxdelay = 5;
		temp_->defense = 20;
		break;
	case StructureKind::NEXUS:
		temp_->maxhp = 2000;
		temp_->curhp = temp_->maxhp;
		temp_->attrange = 0;
		temp_->bulletspeed = 0;
		temp_->bulletdmg = 0;
		temp_->defense = 30;
		break;
	}

	{
		std::unique_lock<std::shared_mutex> lock(session->structure_mutex);
		session->structure_list_room.push_back(temp_);
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
	info.defense = temp_->defense;

	{
		std::shared_lock<std::shared_mutex> lock(session->room_mutex);
		for (Client* inst : session->client_list_room)
			PacketManger::Send(inst->socket, H_STRUCTURE_CREATE, &info, sizeof(StructureInfo));
	}

	if (temp_->struct_kind == 1) {
		TurretSearch(index);
	}
}

void StructureManager::StructureDie(int index, int team, StructureKind struct_kind)
{

	auto structure_list = session->structure_list_room;
	for (auto inst : structure_list)
	{
		if (inst->team == team && inst->struct_kind == struct_kind && inst->index == index)
			session->structure_list_room.remove(inst);
	}


	{
		std::shared_lock<std::shared_mutex> lock(session->room_mutex);
		for (auto inst : session->client_list_room)
			PacketManger::Send(inst->socket, H_STRUCTURE_DIE, &index, sizeof(int));
	}

}

void StructureManager::StructureDie(Structure* structure)
{
	int index = structure->index;
	session->structure_list_room.remove(structure);
	
	{
		std::shared_lock<std::shared_mutex> lock(session->room_mutex);
		for (auto inst : session->client_list_room)
			PacketManger::Send(inst->socket, H_STRUCTURE_DIE, &index, sizeof(int));
	}

}

void StructureManager::StructureStat(int index, int team, StructureKind struct_kind)
{
	Structure* stru_ = nullptr;

	{
		std::unique_lock<std::shared_mutex> lock(session->structure_mutex);
		for (auto inst : session->structure_list_room) {
			if (inst->team == team && inst->struct_kind == static_cast<int>(struct_kind) && inst->index == index) {
				stru_ = inst;

				if (inst->curhp <= 0) {
					inst->curhp = 0;
					inst->index = -1;
					StructureDie(index, inst->team, static_cast<StructureKind>(inst->struct_kind));
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
	info.defense = stru_->defense;

	{
		std::shared_lock<std::shared_mutex> lock(session->room_mutex);
		for (Client* inst : session->client_list_room)
			PacketManger::Send(inst->socket, H_STRUCTURE_STAT, &info, sizeof(StructureInfo));
	}
}

void StructureManager::StructureStat(Structure* structure)
{
	if (structure == nullptr) {
		std::cout << "stru_ nullptr" << std::endl;
		return;
	}

	StructureInfo info;
	info.index = structure->index;
	info.team = structure->team;
	info.struct_kind = structure->struct_kind;
	info.curhp = structure->curhp;
	info.maxhp = structure->maxhp;
	info.attrange = structure->attrange;
	info.bulletdmg = structure->bulletdmg;
	info.bulletspeed = structure->bulletspeed;
	info.defense = structure->defense;

	{
		std::shared_lock<std::shared_mutex> lock(session->room_mutex);
		for (Client* inst : session->client_list_room)
			PacketManger::Send(inst->socket, H_STRUCTURE_STAT, &info, sizeof(StructureInfo));
	}
}