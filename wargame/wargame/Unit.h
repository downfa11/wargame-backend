#pragma once

#include <unordered_map>

enum UnitKind { Infantry, Archer };

struct Unit {
	int index;
	int client_socket;
	UnitKind unit_kind;
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
	int socket = -1;
	int index = -1;
	float rotationX = 0, rotationY = 0, rotationZ = 0;
};

struct unitinfo
{
    int socket;
    int index;
    float x, y, z;
    int unitindex;
    int curhp;
    int maxhp;
    int attack;
    int defense;
    float attspeed;
    int attrange;
    float movespeed;
};