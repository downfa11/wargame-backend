#include "UnitManager.h"

#include "GameManager.h"
#include "PacketManager.h"
#include "GameSession.h"

#include <iostream>

void UnitManager::NewUnit(int client_socket, UnitKind unit_kind) {
	Client* client = GameManager::clients_info[client_socket];

	Unit* temp_ = new Unit;
	temp_->index = unit_count++;
	temp_->client_socket = client_socket;
	temp_->unit_kind = unit_kind;

	temp_->x = client->x;
	temp_->y = client->y;
	temp_->z = client->z;
	temp_->team = client->team;

	switch (temp_->unit_kind) {
	case UnitKind::Infantry:
		temp_->maxhp = 500;
		temp_->curhp = temp_->maxhp;
		temp_->attrange = 0;
		temp_->attack = 10;
		temp_->attRate = 1;
		temp_->attrange = 15;
		temp_->speed = 10;
		break;
	case UnitKind::Archer:
		temp_->maxhp = 300;
		temp_->curhp = temp_->maxhp;
		temp_->attrange = 0;
		temp_->attack = 20;
		temp_->attRate = 1.5f;
		temp_->attrange = 30;
		temp_->speed = 10;
		break;
	}

	{
		std::unique_lock<std::shared_mutex> lock(session->unit_mutex);
		session->unit_list_room.push_back(temp_);
	}

	UnitInfo info;
	info.index = temp_->index;
	info.client_socket = temp_->client_socket;
	info.kind = temp_->unit_kind;
	info.team = temp_->team;
	info.x = temp_->x;
	info.y = temp_->y;
	info.z = temp_->z;

	info.curhp = temp_->curhp;
	info.maxhp = temp_->maxhp;
	info.attack = temp_->attack;
	info.attRate = temp_->attRate;
	info.attrange = temp_->attrange;
	info.speed = temp_->speed;

	{
		std::shared_lock<std::shared_mutex> lock(session->room_mutex);
		for (Client* inst : session->client_list_room)
			PacketManger::Send(inst->socket, H_UNIT_CREATE, &info, sizeof(UnitInfo));
	}
}

void UnitManager::UnitStat(int client_socket, int unit_index, UnitKind unit_kind) {
	std::cout << "Unit Stat" << std::endl;

	Client* client = GameManager::clients_info[client_socket];
	Unit* unit_ = nullptr;
	{
		std::unique_lock<std::shared_mutex> lock(session->unit_mutex);
		for (auto inst : session->unit_list_room)
		{
			if (inst->client_socket == client_socket && inst->team == client->team && inst->unit_kind == unit_kind && inst->index == unit_index)
			{
				unit_ = inst;
				if (inst->curhp <= 0)
				{
					inst->curhp = 0;
					inst->index = -1;
					UnitDie(client_socket, unit_index, unit_kind);
					return;
				}

				break;
			}
		}
	}

	if (unit_ == nullptr) {
		std::cout << "unit_ nullptr" << std::endl;
		return;
	}

	UnitInfo info;
	info.index = unit_->index;
	info.client_socket = unit_->client_socket;
	info.team = unit_->team;
	info.kind = unit_->unit_kind;
	info.curhp = unit_->curhp;
	info.maxhp = unit_->maxhp;
	info.attrange = unit_->attrange;

	{
		std::shared_lock<std::shared_mutex> lock(session->room_mutex);
		for (Client* inst : session->client_list_room)
			PacketManger::Send(inst->socket, H_UNIT_STAT, &info, sizeof(UnitInfo));
	}
}

void UnitManager::UnitStat(Unit* unit) {
	std::cout << "Unit Stat" << std::endl;

	if (unit == nullptr) {
		std::cout << "unit nullptr" << std::endl;
		return;
	}

	UnitInfo info;
	info.index = unit->index;
	info.client_socket = unit->client_socket;
	info.team = unit->team;
	info.kind = unit->unit_kind;
	info.curhp = unit->curhp;
	info.maxhp = unit->maxhp;
	info.attrange = unit->attrange;

	{
		std::shared_lock<std::shared_mutex> lock(session->room_mutex);
		for (Client* inst : session->client_list_room)
			PacketManger::Send(inst->socket, H_UNIT_STAT, &info, sizeof(UnitInfo));
	}
}

void UnitManager::UnitDie(int client_socket, int unit_index, UnitKind unit_kind) {
	std::cout << "Unit Die" << std::endl;

	int team = GameManager::clients_info[client_socket]->team;
	auto unit_list = session->unit_list_room;
	for (auto inst : unit_list)
	{
		if (inst->client_socket == client_socket && inst->team == team && inst->unit_kind == unit_kind && inst->index == unit_index)
			session->unit_list_room.remove(inst);
	}


	{
		std::shared_lock<std::shared_mutex> lock(session->room_mutex);
		for (auto inst : session->client_list_room)
			PacketManger::Send(inst->socket, H_UNIT_DIE, &unit_index, sizeof(int));
	}
}

void UnitManager::UnitDie(Unit* unit) {
	int unit_index = unit->index;
	session->unit_list_room.remove(unit);

	{
		std::shared_lock<std::shared_mutex> lock(session->room_mutex);
		for (auto inst : session->client_list_room)
			PacketManger::Send(inst->socket, H_UNIT_DIE, &unit_index, sizeof(int));
	}
}

void UnitManager::UnitMoveStart(int client_socket, UnitMovestart* info)
{
	std::shared_lock<std::shared_mutex> lock(session->room_mutex);

	std::list<Client*>& clients_in_room = session->client_list_room;
	std::list<Unit*>& units_in_room = session->unit_list_room;

	Client* attacked = FindClientBySocket(client_socket, clients_in_room);
	Unit* unit = FindUnitByIndex(info->socket, info->index, units_in_room);

	unit->rotationX = info->rotationX;
	unit->rotationY = info->rotationY;
	unit->rotationZ = info->rotationZ;

	for (auto& inst : session->client_list_room)
	{
		if (inst->socket != client_socket)
			PacketManger::Send(inst->socket, H_UNIT_MOVESTART, info, sizeof(ClientMovestart));
	}
}

void UnitManager::UnitMove(int client_socket, UnitInfo info)
{
	std::list<Client*>& clients_in_room = session->client_list_room;
	std::list<Unit*>& units_in_room = session->unit_list_room;

	Client* attacked = FindClientBySocket(client_socket, clients_in_room);
	Unit* unit = FindUnitByIndex(client_socket, info.index, units_in_room);

	unit->x = info.x;
	unit->y = info.y;
	unit->z = info.z;

	std::cout << unit->index << ": " << unit->x << ", " << unit->y << ", " << unit->z << std::endl;

	std::shared_lock<std::shared_mutex> lock(session->room_mutex);
	for (auto inst : session->client_list_room)
	{
		if (inst->socket != client_socket)
			PacketManger::Send(inst->socket, H_UNIT_MOVE, &info, sizeof(UnitInfo));
	}
}

void UnitManager::UnitMoveStop(int client_socket, UnitInfo info) {
	std::list<Client*>& clients_in_room = session->client_list_room;
	std::list<Unit*>& units_in_room = session->unit_list_room;

	Client* attacked = FindClientBySocket(client_socket, clients_in_room);
	Unit* unit = FindUnitByIndex(client_socket, info.index, units_in_room);

	unit->x = info.x;
	unit->y = info.y;
	unit->z = info.z;

	std::shared_lock<std::shared_mutex> lock(session->room_mutex);

	for (auto inst : session->client_list_room) {
		if (inst->socket != client_socket)
			PacketManger::Send(inst->socket, H_UNIT_MOVESTOP, &info, sizeof(UnitInfo));
	}

}

Unit* UnitManager::FindUnitByIndex(int socket, int index, std::list<Unit*>& units) {
	auto it = std::find_if(units.begin(), units.end(), [index](Unit* struc) {
		return struc->index == index;
		});
	return (it != units.end()) ? *it : nullptr;
}

Client* UnitManager::FindClientBySocket(int socket, std::list<Client*>& clients) {
	auto it = std::find_if(clients.begin(), clients.end(), [socket](Client* client) {
		return client->socket == socket;
		});
	return (it != clients.end()) ? *it : nullptr;
}