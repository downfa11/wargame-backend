#pragma once

#include<chrono>

class Structure
{
public:
	int index = 0;
	int struct_kind = -1; // nexus:0, turret:1, gate:2
	float x = 0;
	float y = 0;
	float z = 0;
	int curhp;
	int maxhp;
	float maxdelay;
	int attrange;
	int bulletdmg;
	float bulletspeed;
	int defense;
	int team = -1; // 0 for blue team, 1 for red team
	std::chrono::high_resolution_clock::time_point lastUpdateTime;
};

#pragma pack(push,1)
struct StructureInfo
{
	int index;
	int struct_kind;
	int curhp;
	int maxhp;
	float x;
	float y;
	float z;
	int attrange;
	int bulletdmg;
	float bulletspeed;
	int defense;
	int team;
};