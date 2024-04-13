#pragma once

#include<iostream>
#include <string>
#include <vector>
#include<mysql.h>

#pragma comment(lib,"libmysql.lib")

using namespace std;

class ChampionStats {
public:
	int index;
	string name;
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
	string name;
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
	static vector<itemStats> items;

	static void ItemInit();
};

class ChampionSystem {
public:
	static vector<ChampionStats> champions;
	static void ChampionInit();
};
