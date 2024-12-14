#pragma once

#include<mysql.h>

#include<iostream>
#include <string>
#include <vector>

#pragma comment(lib,"libmysql.lib")

#pragma pack(push,1)
class ChampionStats {
public:
	int index;
	std::string name;
	int attack;
	float absorptionRate;
	int defense;
	int maxhp;
	int maxmana;
	float movespeed;
	float maxdelay;
	float attspeed;
	int attrange;
	float critical;
	float criProbability;

	int growHp;
	int growMana;
	int growAtt;
	int growCri;
	int growCriPob;
};

struct Item {
	int id;
	bool isPerchase;
};

struct itemStats {
	int id;
	std::string name;
	int gold;
	int attack;
	int maxhp;
	float movespeed;
	float maxdelay;
	float attspeed;
	int criProbability;
	float absorptionRate;
	int defense;
};

struct ChampionStatsInfo {
	int index;
	int attack;
	float absorptionRate;
	int defense;
	int maxhp;
	int maxmana;
	float movespeed;
	float maxdelay;
	float attspeed;
	int attrange;
	float critical;
	float criProbability;
};

struct ItemStatsInfo {
	int id;
	int gold;
	int attack;
	int maxhp;
	float movespeed;
	float maxdelay;
	float attspeed;
	int criProbability;
	float absorptionRate;
	int defense;
};

#pragma pack(pop)

class ItemSystem {
public:
	static std::vector<itemStats> items;

	static void ItemInit();
	static void GetItemData(MYSQL_ROW row);
	static ItemStatsInfo GetItemInfo(const itemStats& item);
};

class ChampionSystem {
public:
	static std::vector<ChampionStats> champions;
	static void ChampionInit();
	static void GetChampionData(MYSQL_ROW row);
	static ChampionStatsInfo GetChampionInfo(const ChampionStats& champion);
};