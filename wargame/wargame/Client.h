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