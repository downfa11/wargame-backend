#pragma once
#include "base.h"

#include <shared_mutex>
#include <memory>
#include <chrono>


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
	float curdelay;
	int attrange;
	int bulletdmg;
	float bulletspeed;
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
	int team;
};
#pragma pack(pop)

#pragma pack(push,1)
struct TurretBullet {
	double x;
	double y;
	double z;
	int dmg;
};
#pragma pack(pop)

#pragma pack(push,1)
struct BulletInfo {
	float targetX;
	float targetY;
	float targetZ;

	float directionX;
	float directionY;
	float directionZ;

	float moveDistance;
};
#pragma pack(pop)

class StructureManager {
public:
	static std::shared_mutex structure_mutex;

	static void NewStructure(int index, int team, int struct_kind, int chan, int room, int x, int y, int z);
	static void StructureDie(int index, int team, int struct_kind, int chan, int room);
	static void StructureStat(int index, int team, int struct_kind, int chan, int room);

	static void TurretSearch(int index, int chan, int room);
	static void TurretShot(int index, TurretBullet* newBullet, int attacked, int chan, int room);
	static void StopTurretSearch(int index);

private:
	static void MoveBulletAsync(TurretBullet* newBullet, BulletInfo bulletInfo, Client* attacked, Structure* attacker);

};