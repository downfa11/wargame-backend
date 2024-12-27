#include "GameSession.h"

#include "GameManager.h"
#include "PacketManager.h"
#include "Utility.h"
#include "Timer.h"


#define MAX_PLAYER_MOVE_SPEED 100

bool GameSession::IsPositionValid(const Client& currentPos, const ClientInfo& newPos) {
	float distSquared = (newPos.x - currentPos.x) * (newPos.x - currentPos.x) +
		(newPos.y - currentPos.y) * (newPos.y - currentPos.y) +
		(newPos.z - currentPos.z) * (newPos.z - currentPos.z);
	return distSquared <= MAX_MOVE_DISTANCE * MAX_MOVE_DISTANCE;
}

void GameSession::ClientMoveStart(int client_socket, ClientMovestart* info)
{
	std::shared_lock<std::shared_mutex> lock(room_mutex);
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
		std::cout << "ClientMove 위치 검증 실패: 비정상적 이동 탐지됨" << std::endl;
		return;
	}



	if (positionValid) {
		std::shared_lock<std::shared_mutex> lock(room_mutex);
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
		std::shared_lock<std::shared_mutex> lock(room_mutex);
		for (auto inst : client_list_room)
		{
			if (inst->socket != client_socket)
				PacketManger::Send(inst->socket, H_MOVESTOP, &info, sizeof(ClientInfo));
		}
	}
}

void GameSession::MouseSearch(Client* client, MouseInfo info)
{
	if (!client)
		return;

	int client_socket = client->socket;
	Target targetKind = static_cast<Target>(info.kind);
	int teamClient = client->team;

	if (teamClient == -1) {
		std::cout << "MouseSearch team error" << std::endl;
		return;
	}

	double minDistance = DBL_MAX;
	void* closestTarget = nullptr;

	std::shared_lock<std::shared_mutex> lock;
	switch (targetKind) {
	case CLIENT: {
		lock = std::shared_lock<std::shared_mutex>(room_mutex);
		FindClosestTarget(client_list_room, teamClient, info, minDistance, closestTarget);
		break;
	}
	case STRUCTURE: {
		lock = std::shared_lock<std::shared_mutex>(structure_mutex);
		FindClosestTarget(structure_list_room, teamClient, info, minDistance, closestTarget);
		break;
	}
	case UNIT: {
		lock = std::shared_lock<std::shared_mutex>(unit_mutex);
		FindClosestTarget(unit_list_room, teamClient, info, minDistance, closestTarget);
		break;
	}
	default:
		std::cout << "Invalid target type. 움직이긴 한거냐;" << "\n";
		return;
	}

	if (closestTarget != nullptr)
	{
		int team = -1, value = -1, object_kind = -1;
		switch (targetKind) {
		case CLIENT: {
			auto target = static_cast<Client*>(closestTarget);
			team = target->team;
			value = target->socket;
			object_kind = -1;
			break;
		}
		case STRUCTURE: {
			auto target = static_cast<Structure*>(closestTarget);
			team = target->team;
			object_kind = target->struct_kind;
			value = target->index;
			break;
		}
		case UNIT: {
			auto target = static_cast<Unit*>(closestTarget);
			team = target->team;
			object_kind = target->unit_kind;
			value = target->index;
			break;
		}
		}

		int kind = static_cast<int>(targetKind);
		std::cout << "kind: " << kind << ", team : " << team << ", object_kind: " << object_kind << ", value: " << value << std::endl;

		if (team == -1 || value == -1) {
			std::cout << "뭔가 잘못되었어. 안보여 team,kind, value" << std::endl;
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
	else std::cout << "closestTarget 없어임마" << std::endl;

}