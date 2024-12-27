#include "GameSession.h"

#include "GameManager.h"
#include "PacketManager.h"
#include "Utility.h"
#include "Timer.h"

#include<shared_mutex>


#define MAX_PLAYER_MOVE_SPEED 100

void GameSession::ClientChat(std::string& name, int size, void* data)
{
	auto curtime = std::chrono::system_clock::now();
	if (chat_log.size() == 0)
		chat_log.push_back({ "host", {curtime, "Game Start - 게임 시작을 알립니다."} });

	std::string chat(reinterpret_cast<char*>(data) + sizeof(int), size);

	chat_log.push_back({ name, {curtime, chat} });


	{
		std::shared_lock<std::shared_mutex> lock(room_mutex);
		for (auto cli : client_list_room)
			PacketManger::Send(cli->socket, H_CHAT, data, size + sizeof(int));
	}

}

std::vector<ChatEntry> GameSession::GetChatLog()
{
	return chat_log;
}

void GameSession::ClientReady(int client_socket, int champindex)
{
	{
		std::unique_lock<std::shared_mutex> lock(room_mutex);
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
					std::cout << "ready to " << inst->socket << ":" << champindex << std::endl;
				else
					std::cout << "disready to " << inst->socket << std::endl;


				delete[] packet_data;
				break;
			}
		}
	}
}

bool GameSession::AllClientsReady() {

	{
		std::shared_lock<std::shared_mutex> lock(room_mutex);

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
void GameSession::SendVictory(int winTeam)
{
	auto structurelist = structure_list_room;
	auto clientist = client_list_room;

	int chan = channelId;
	int room = roomId;

	structurelist.clear();

	BYTE packet_data[sizeof(int)];
	memcpy(packet_data, &winTeam, sizeof(int));

	MatchResult result;
	result.state = "success";
	result.channel = chan;
	result.room = room;

	auto now = std::chrono::system_clock::now();
	auto now_c = std::chrono::system_clock::to_time_t(now);

	tm tm;
	localtime_s(&tm, &now_c);

	std::stringstream ss;
	ss << std::put_time(&tm, "%Y-%m-%d %H:%M:%S");

	result.dateTime = ss.str();
	std::cout << "dateTime : " << result.dateTime << std::endl;


	RoomData curRoom;

	{
		for (auto& inst : GameManager::auth_data) {

			if (inst.channel == channelId && inst.room == roomId) {
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

	std::chrono::duration<double> elapsed_seconds;

	{
		elapsed_seconds = now - startTime;
		startTime = std::chrono::time_point<std::chrono::system_clock>();
	}

	int gameMinutes = static_cast<int>(elapsed_seconds.count() / 60);

	result.gameDuration = gameMinutes;

	result.winTeam = (winTeam == 0 ? "blue" : "red");
	result.loseTeam = (winTeam == 0 ? "red" : "blue");

	MatchManager::SaveMatchResult(result);

	//clear
	auto condition = [chan, room](RoomData& roomInfo) {return roomInfo.channel == chan && roomInfo.room == room; };
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
		std::shared_lock<std::shared_mutex> lock(GameManager::clients_info_mutex);
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
		std::shared_lock<std::shared_mutex> lock(room_mutex);
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

	std::cout << client_socket << " 사용자의 챔피언은 " << champIndex << "입니다." << std::endl;
	for (auto& champion : ChampionSystem::champions) {
		if (champion.index == champIndex) {
			champ = &champion;
			break;
		}
	}

	if (champ == nullptr) {
		std::cout << "champ를 찾을 수 없엉" << std::endl;
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
		std::shared_lock<std::shared_mutex> lock(room_mutex);
		for (auto inst : client_list_room)
		{
			PacketManger::Send(inst->socket, H_CHAMPION_INIT, &info, sizeof(ClientInfo));
			PacketManger::Send(inst->socket, H_ITEM_STAT, &slots, sizeof(ItemSlots));
		}
	}
}

void GameSession::ClientChampInit(Client* client) {

	if (client == nullptr) {
		std::cout << client->clientindex << " 인덱스의 사용자는 픽 전에 종료하셨습니다." << std::endl;
		return;
	}

	int chan = client->channel;
	int room = client->room;

	int champIndex = client->champindex;
	ChampionStats* champ = nullptr;
	std::cout << client->socket << " 사용자의 챔피언은 " << champIndex << "입니다." << std::endl;
	for (auto& champion : ChampionSystem::champions) {
		if (champion.index == champIndex) {
			champ = &champion;
			break;
		}
	}

	if (champ == nullptr) {
		std::cout << "champ를 찾을 수 없엉" << std::endl;
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

	std::cout << "clients maxhp : " << client->maxhp << std::endl;
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
				std::cout << "꽉 찼어." << std::endl;
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

				std::cout << client->socket << "님이 " << (*curItem).name << " 를 " << NeedGold << "에 구매하는데 성공했습니다." << std::endl;

				client->itemList[index] = ((*curItem).id); // 아이템 추가

				ClientStat(client->socket);
			}
			else
			{
				std::cout << "show me the money" << std::endl;
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

					std::cout << client->socket << "님이 " << (*curItem).name << " 를 " << NeedGold * 0.8f << "에 판매하는데 성공했습니다." << std::endl;

					client->itemList[i] = 0; // 아이템 삭제

					ClientStat(client->socket);
					break;
				}
			}
		}
	}
}

void GameSession::GetChampInfo(int client_socket) {
	std::shared_lock<std::shared_mutex> lock(room_mutex);

	for (auto cli : client_list_room) {
		if (cli->socket == client_socket) {
			for (auto& champion : ChampionSystem::champions) {
				ChampionStatsInfo championInfo = ChampionSystem::GetChampionInfo(champion);
				// std::cout << "Champion Sent - Index: " << championInfo.index << ", Attack: " << championInfo.attack << "\n";

				PacketManger::Send(cli->socket, H_CHAMPION_INFO, &championInfo, sizeof(ChampionStatsInfo));
			}
			return;
		}
	}
	std::cout << "Client not found: " << client_socket << "\n";
}

void GameSession::GetItemInfo(int client_socket) {
	for (auto cli : client_list_room) {
		std::shared_lock<std::shared_mutex> lock(room_mutex);
		if (cli->socket == client_socket) {
			for (auto& item : ItemSystem::items) {
				ItemStatsInfo itemInfo = ItemSystem::GetItemInfo(item);
				// std::cout << "Item Sent - ID: " << itemInfo.id << ", Gold: " << itemInfo.gold << "\n";
				PacketManger::Send(cli->socket, H_ITEM_INFO, &itemInfo, sizeof(ItemStatsInfo));
			}
			return;
		}
	}
	std::cout << "Client not found: " << client_socket << "\n";
}