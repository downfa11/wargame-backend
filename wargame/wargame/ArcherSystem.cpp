#include "GameSession.h"

#include "PacketManager.h"
#include "Timer.h"

#include <vector>
#include <shared_mutex>
#include <unordered_map>


std::unordered_map<int, PassiveStack> clientPassiveStacks;
std::unordered_map<int, PassiveStack> structurePassiveStacks;
std::unordered_map<int, PassiveStack> unitPassiveStacks;
std::mutex passiveStackMutex;

const std::unordered_map<Target, int> TargetHeaderArcher = {
	{CLIENT, H_CHAMP1_PASSIVE_CLIENT},
	{STRUCTURE, H_CHAMP1_PASSIVE_STRUCTURE},
	{UNIT, H_CHAMP1_PASSIVE_UNIT}
};

void GameSession::Champ1Passive(Client* attacker, AttInfo info) {
	if (attacker->champindex != 1) return;

	PassiveStack champ1Passive = { -1, 5000, 0, 3 };

	if (info.kind == Target::CLIENT) {
		HandleClient1PassiveToClient(attacker, info.attacked, champ1Passive);
	}
	else if (info.kind == Target::STRUCTURE) {
		HandleClient1PassiveToStructure(attacker, info.attacked, champ1Passive);
	}
	else if (info.kind == Target::UNIT) {
		HandleClient1PassivetoUnit(attacker, info.attacked, champ1Passive);
	}
}

void GameSession::HandleClient1PassiveToClient(Client* attacker, int attacked_socket, PassiveStack stackConfig) {
	Client* attacked = nullptr;
	{
		std::shared_lock<std::shared_mutex> lock(room_mutex);
		auto attacked_it = std::find_if(client_list_room.begin(), client_list_room.end(),
			[attacked_socket](Client* client) { return client->socket == attacked_socket; });

		if (attacked_it == client_list_room.end() || (*attacked_it)->curhp <= 0) {
			std::cout << "Attacked client not found or already dead" << std::endl;
			return;
		}

		attacked = *attacked_it;
	}

	int uniqueTimerId = attacker->socket * 10000 + attacked_socket;

	std::lock_guard<std::mutex> lock(passiveStackMutex);
	if (clientPassiveStacks.find(attacked_socket) != clientPassiveStacks.end()) {
		auto& stack = clientPassiveStacks[attacked_socket];

		if (stack.passiveId == uniqueTimerId) {
			stack.stackCount++;
			stack.stackTime = stackConfig.stackTime;

			if (clientPassiveStacks[attacked_socket].stackCount >= stackConfig.maxStackCount) {
				int damage = static_cast<int>(attacker->attack * attacker->level);
				attacked->curhp -= damage;
				std::cout << "champ1 passive damage: " << damage << std::endl;

				if (attacked->curhp <= 0) {
					ClientDie(attacked->socket, attacker->socket, Target::CLIENT);
					std::cout << "Client destroyed" << std::endl;
				}

				ClientStat(attacked->socket);
				NotifyPassiveStack(attacker->socket, attacked_socket, stack.maxStackCount, stack.stackCount, Target::CLIENT, stack.stackTime);

				Timer::RemoveTimer(clientPassiveStacks[attacked_socket].passiveId);
				clientPassiveStacks.erase(attacked_socket);
				return;
			}
			
			std::cout << "Passive stack updated: TargetID=" << attacked_socket << ", StackCount=" << stack.stackCount << ". MaxStackCount=" << stack.maxStackCount << std::endl;


			Timer::RemoveTimer(stack.passiveId);
			Timer::AddTimer(stack.passiveId, [this, attacked_socket]() {
				std::lock_guard<std::mutex> lock(passiveStackMutex);
				auto& stack = clientPassiveStacks[attacked_socket];
				if (stack.stackCount < stack.maxStackCount) {
					clientPassiveStacks.erase(attacked_socket);
					std::cout << "Passive stack expired for target: " << attacked_socket << std::endl;
				}
				}, stackConfig.stackTime);

			NotifyPassiveStack(attacker->socket, attacked_socket, stack.maxStackCount, stack.stackCount, Target::CLIENT, stack.stackTime);
			return;
		}
	}

	clientPassiveStacks[attacked_socket] = stackConfig;
	clientPassiveStacks[attacked_socket].passiveId = uniqueTimerId;
	clientPassiveStacks[attacked_socket].stackCount = 1;

	std::cout << "Passive stack created: TargetID=" << attacked_socket << ", StackCount=" << 1 << std::endl;

	Timer::AddTimer(clientPassiveStacks[attacked_socket].passiveId, [this, attacked_socket]() {
		PassiveStack stack;
		{
			std::lock_guard<std::mutex> lock(passiveStackMutex);
			if (clientPassiveStacks.find(attacked_socket) == clientPassiveStacks.end()) return;
			stack = clientPassiveStacks[attacked_socket];
		}

		if (stack.stackCount < stack.maxStackCount) {
			std::lock_guard<std::mutex> lock(passiveStackMutex);
			clientPassiveStacks.erase(attacked_socket);
			std::cout << "Passive stack expired for target: " << attacked_socket << std::endl;
		}

		}, stackConfig.stackTime);

	NotifyPassiveStack(attacker->socket, attacked_socket, stackConfig.maxStackCount, 1, Target::CLIENT, stackConfig.stackTime);
}

void GameSession::HandleClient1PassiveToStructure(Client* attacker, int structure_id, PassiveStack stackConfig) {
	Structure* attacked = nullptr;
	{
		std::shared_lock<std::shared_mutex> lock(room_mutex);
		auto attacked_it = std::find_if(structure_list_room.begin(), structure_list_room.end(),
			[structure_id](Structure* structure) { return structure->index == structure_id; });

		if (attacked_it == structure_list_room.end() || (*attacked_it)->curhp <= 0) {
			std::cout << "Structure not found or already destroyed" << std::endl;
			return;
		}

		attacked = *attacked_it;
	}

	int uniqueTimerId = attacker->socket * 100000 + structure_id;

	std::lock_guard<std::mutex> lock(passiveStackMutex);

	if (structurePassiveStacks.find(structure_id) != structurePassiveStacks.end()) {
		auto& stack = structurePassiveStacks[structure_id];
		if (stack.passiveId == uniqueTimerId) {
			stack.stackCount++;
			stack.stackTime = stackConfig.stackTime;

			if (structurePassiveStacks[structure_id].stackCount >= stackConfig.maxStackCount) {
				int damage = static_cast<int>(attacker->attack * attacker->level * 5);
				attacked->curhp -= damage;
				std::cout << "champ1 passive damage: " << damage << std::endl;

				if (attacked->curhp <= 0) {
					structureManager->StructureDie(attacked);
					std::cout << "Client destroyed" << std::endl;
				}

				structureManager->StructureStat(attacked);
				NotifyPassiveStack(attacker->socket, structure_id, stack.maxStackCount, stack.stackCount, Target::STRUCTURE, stack.stackTime);
				Timer::RemoveTimer(structurePassiveStacks[structure_id].passiveId);
				structurePassiveStacks.erase(structure_id);
				return;
			}

			std::cout << "Passive stack updated: TargetID=" << structure_id << ", StackCount=" << stack.stackCount << ". MaxStackCount=" << stack.maxStackCount << std::endl;

			Timer::RemoveTimer(stack.passiveId);
			Timer::AddTimer(stack.passiveId, [this, structure_id]() {
				std::lock_guard<std::mutex> lock(passiveStackMutex);
				auto& stack = structurePassiveStacks[structure_id];
				if (stack.stackCount < stack.maxStackCount) {
					structurePassiveStacks.erase(structure_id);
					std::cout << "Passive stack expired for target: " << structure_id << std::endl;
				}
				}, stackConfig.stackTime);

			NotifyPassiveStack(attacker->socket, structure_id, stack.maxStackCount, stack.stackCount, Target::STRUCTURE, stack.stackTime);
			return;
		}
	}

	structurePassiveStacks[structure_id] = stackConfig;
	structurePassiveStacks[structure_id].passiveId = uniqueTimerId;
	structurePassiveStacks[structure_id].stackCount = 1;

	std::cout << "Passive stack created: TargetID=" << structure_id << ", StackCount=" << 1 << std::endl;

	Timer::AddTimer(structurePassiveStacks[structure_id].passiveId, [this, structure_id]() {
		PassiveStack stack;
		{
			std::lock_guard<std::mutex> lock(passiveStackMutex);
			if (structurePassiveStacks.find(structure_id) == structurePassiveStacks.end()) return;
			stack = structurePassiveStacks[structure_id];
		}

		if (stack.stackCount < stack.maxStackCount) {
			std::lock_guard<std::mutex> lock(passiveStackMutex);
			structurePassiveStacks.erase(structure_id);
			std::cout << "Passive stack expired for target: " << structure_id << std::endl;
		}

		}, stackConfig.stackTime);

	NotifyPassiveStack(attacker->socket, structure_id, stackConfig.maxStackCount, 1, Target::STRUCTURE, stackConfig.stackTime);
}

void GameSession::HandleClient1PassivetoUnit(Client* attacker, int unit_id, PassiveStack stackConfig) {
	Unit* attacked = nullptr;
	{
		std::shared_lock<std::shared_mutex> lock(room_mutex);
		auto attacked_it = std::find_if(unit_list_room.begin(), unit_list_room.end(),
			[unit_id](Unit* unit) { return unit->index == unit_id; });

		if (attacked_it == unit_list_room.end() || (*attacked_it)->curhp <= 0) {
			std::cout << "Unit not found or already destroyed" << std::endl;
			return;
		}

		attacked = *attacked_it;
	}


	int uniqueTimerId = attacker->socket * 100000 + unit_id;

	std::lock_guard<std::mutex> lock(passiveStackMutex);
	if (unitPassiveStacks.find(unit_id) != unitPassiveStacks.end()) {
		auto& stack = unitPassiveStacks[unit_id];
		if (stack.passiveId == uniqueTimerId) {

			stack.stackCount++;
			stack.stackTime = stackConfig.stackTime;

			if (unitPassiveStacks[unit_id].stackCount >= stackConfig.maxStackCount) {
				int damage = static_cast<int>(attacker->attack * attacker->level * 5);
				attacked->curhp -= damage;
				std::cout << "champ1 passive damage: " << damage << std::endl;

				if (attacked->curhp <= 0) {
					unitManager->UnitDie(attacked);
					std::cout << "Unit destroyed" << std::endl;
				}

				unitManager->UnitStat(attacked);
				NotifyPassiveStack(attacker->socket, unit_id, stack.maxStackCount, stack.stackCount, Target::UNIT, stack.stackTime);
				Timer::RemoveTimer(unitPassiveStacks[unit_id].passiveId);
				unitPassiveStacks.erase(unit_id);
				return;
			}

			std::cout << "Passive stack updated: TargetID=" << unit_id << ", StackCount=" << stack.stackCount << ". MaxStackCount=" << stack.maxStackCount << std::endl;


			Timer::RemoveTimer(stack.passiveId);
			Timer::AddTimer(stack.passiveId, [this, unit_id]() {
				PassiveStack stack;
				{
					std::lock_guard<std::mutex> lock(passiveStackMutex);
					if (unitPassiveStacks.find(unit_id) == unitPassiveStacks.end()) return;
					stack = unitPassiveStacks[unit_id];
				}

				if (stack.stackCount < stack.maxStackCount) {
					std::lock_guard<std::mutex> lock(passiveStackMutex);
					unitPassiveStacks.erase(unit_id);
					std::cout << "Passive stack expired for target: " << unit_id << std::endl;
				}

				}, stackConfig.stackTime);

			NotifyPassiveStack(attacker->socket, unit_id, stack.maxStackCount, stack.stackCount, Target::UNIT, stack.stackTime);
			return;
		}
	}

	unitPassiveStacks[unit_id] = stackConfig;
	unitPassiveStacks[unit_id].passiveId = uniqueTimerId;
	unitPassiveStacks[unit_id].stackCount = 1;

	std::cout << "Passive stack created: TargetID=" << unit_id << ", StackCount=" << 1 << std::endl;

	Timer::AddTimer(unitPassiveStacks[unit_id].passiveId, [this, unit_id]() {
		std::lock_guard<std::mutex> lock(passiveStackMutex);
		auto& stack = unitPassiveStacks[unit_id];
		if (stack.stackCount < stack.maxStackCount) {
			unitPassiveStacks.erase(unit_id);
			std::cout << "Passive stack expired for target: " << unit_id << std::endl;
		}
		}, stackConfig.stackTime);

	NotifyPassiveStack(attacker->socket, unit_id, stackConfig.maxStackCount, 1, Target::UNIT, stackConfig.stackTime);
}

void GameSession::NotifyPassiveStack(int client_socket, int target_id, int maxStackCount, int stackCount, Target targetType, int stackTime) {
	auto header = TargetHeaderArcher.at(targetType);

	for (auto client : client_list_room) {
		std::vector<BYTE> packet_data(sizeof(int) * 5);
		memcpy(packet_data.data(), &client_socket, sizeof(int));
		memcpy(packet_data.data() + sizeof(int), &target_id, sizeof(int));
		memcpy(packet_data.data() + 2 * sizeof(int), &maxStackCount, sizeof(int));
		memcpy(packet_data.data() + 3 * sizeof(int), &stackCount, sizeof(int));
		memcpy(packet_data.data() + 4 * sizeof(int), &stackTime, sizeof(int));
		PacketManger::Send(client->socket, header, packet_data.data(), packet_data.size());
	}
}