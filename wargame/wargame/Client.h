#pragma once

struct MouseInfo
{
	float x;
	float y;
	float z;
	int kind = -1; /// 0: Player, 1:Structure, 2:Unit

};

struct AttInfo {
	int attacker;
	int attacked;
	int kind;
	int object_kind;
	int assist1 = -1;
	int assist2 = -1;
	int assist3 = -1;
	int assist4 = -1;

};

struct Bullet {
	int index;
	int type; // turret:0, 
	int targetIndex;
	int targetType;
	float x;
	float y;
	float z;
	int demage;
	float speed;
};

struct ItemSlots {
	int socket;
	int id_0;
	int id_1;
	int id_2;
	int id_3;
	int id_4;
	int id_5;
};


struct Unit {
	int index;
	int client_socket;
	int unit_kind = -1; // 병종 종류
	int team = -1; // red, blue
	int curhp = 0;
	int maxhp = 0;
	float x = 0, y = 0, z = 0;
	float rotationX = 0, rotationY = 0, rotationZ = 0;
	int attack = 0;
	int defense = 0;
	float maxdelay = 0;
	float curdelay = 0;
	int attrange = 0;
	int attRate = 0;
	float speed = 0;

};

struct UnitInfo {
	int index;
	int client_socket;
	int kind = -1; // 병종 종류
	int team = -1; // red, blue
	int curhp = 0;
	int maxhp = 0;
	float x = 0;
	float y = 0;
	float z = 0;
	int attack = 0;
	int defense = 0;
	int attrange = 0;
	int attRate = 0;
	float speed = 0;

};

struct UnitMovestart {
	int socket=-1;
	int index=-1;
	float rotationX=0, rotationY=0, rotationZ=0;
};