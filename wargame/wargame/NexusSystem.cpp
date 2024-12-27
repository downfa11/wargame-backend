#include "StructureManager.h"

#include "GameSession.h"
#include "Utility.h"

#include<mutex>

void StructureManager::Well(Client* client, int x, int y, int z) {
	if (!client) {
		std::cout << "Invalid client pointer" << std::endl;
		return;
	}

	Structure* target_structure = nullptr;
	{
		std::shared_lock<std::shared_mutex> lock(session->structure_mutex);
		for (auto& structure : session->structure_list_room) {
			if (structure->struct_kind == 0 && structure->team == client->team) {
				target_structure = structure;
				break;
			}
		}
	}

	if (target_structure == nullptr) {
		std::cout << "lock target_structure error" << std::endl;
		return;
	}

	float distance = UtilityManager::DistancePosition(target_structure->x, target_structure->y, target_structure->z, x, y, z);
	int minDistance = 18;
	if (distance <= minDistance && client->curhp < client->maxhp)
	{
		client->curhp += 1;
		session->ClientStat(client->socket);
	}
	else return;
}