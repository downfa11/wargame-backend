#pragma once

#include<mysql.h>

#include<iostream>
#include <string>
#include <vector>

class ChampionStats {
public:
	int index;
	std::string name;
	int attack;
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

#pragma pack(push,1)
struct Item {
	int id;
	bool isPerchase;

};
#pragma pack(pop)


#pragma pack(push,1)
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
};
#pragma pack(pop)

class ItemSystem {
public:
	static std::vector<itemStats> items;

	static void ItemInit();
	static void getItemData(MYSQL_ROW row);
};

class ChampionSystem {
public:
	static std::vector<ChampionStats> champions;
	static void ChampionInit();
	static void getChampionData(MYSQL_ROW row);
};