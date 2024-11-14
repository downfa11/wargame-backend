#pragma once

#include <winsock2.h>
#include <windows.h>
#include <process.h>
#include <ws2tcpip.h>
#include <iostream>
#include <string>
#include <list>
#include <chrono>
#include <unordered_map>
#include <thread>
#include <atomic>
#include <stack>
#include <iomanip>
#include <vector>
#include<algorithm>
#include <random>
#include <sstream>
#include<shared_mutex>
#include <asio.hpp>

#pragma comment(lib,"ws2_32.lib")


using namespace std;


typedef unsigned int       UINT;
typedef unsigned long       DWORD;
typedef unsigned short      WORD;
typedef unsigned char       BYTE;

#define BUF_SIZE 1024
#define READ 3
#define WRITE 5

#define H_CHAT 5877
#define H_START 1000
#define H_MOVESTART 8281
#define H_MOVE 8282
#define H_MOVESTOP 8283
#define H_TIMEOUT_SET 9425
#define H_NEWBI 1111
#define H_USER_DISCON 4444
#define H_CHANNEL_MOVE 1212
#define H_ROOM_MOVE 3434

#define H_CHAMPION_INIT 1240
#define H_CLIENT_STAT 1048
#define H_ATTACK_CLIENT 8888
#define H_ATTACK_STRUCT 8889
#define H_ATTACK_TARGET 1824

#define H_IS_READY 1214
#define H_TEAM 3493
#define H_BATTLE_START 1648


#define H_VICTORY 1934

#define H_STRUCTURE_CREATE 1924
#define H_STRUCTURE_DIE 1925
#define H_STRUCTURE_STAT 1926

#define H_CLIENT_DIE 1294
#define H_CLIENT_RESPAWN 1592
#define H_KILL_LOG 1818

#define H_CLIENT_STOP 4928
#define H_SEND_ME_AGAIN_CONNECT 9999

#define H_BUY_ITEM 2886
#define H_ITEM_STAT 9141

#define H_WELL 8313
#define H_NOTICE 4829

#define H_RAUTHORIZATION 1525
#define H_CAUTHORIZATION 1530
#define H_PICK_TIME 1084

#define H_CHAMP1_PASSIVE 1611

typedef struct socketf
{
	SOCKET hClntSock;
	SOCKADDR_IN clntAdr;
} PER_HANDLE_DATA, * LPPER_HANDLE_DATA;

typedef struct OverlappedEx
{
	OVERLAPPED overlapped;
	WSABUF wsaBuf;
	int rwMode; //READ or WRITE
	BYTE* broken_data = new BYTE[1024];
	int broken_data_size = 0;
	bool header_recv = false;
	bool header_broken = false;
	bool data_broken = false;
} PER_IO_DATA, * LPPER_IO_DATA;



class Client
{
public:
	int socket = 0;
	int champindex = -1;
	string user_name = "";

	time_t out_time = 0;
	int channel = 0;
	int room = 0;
	string code = "";
	int clientindex = -1;

	int kill = 0;
	int death = 0;
	int assist = 0;
	float x = 0;
	float y = 0;
	float z = 0;
	int gold = 1000;
	float rotationX = 0;
	float rotationY = 0;
	float rotationZ = 0;

	int level=0;
	int maxexp=100;
	int exp=0;

	chrono::high_resolution_clock::time_point lastUpdateTime;

	int curhp=0;
	int maxhp=0;
	int curmana=0;
	int maxmana=0;
	int attack=0;
	int critical=0;
	int criProbability=0;
	float maxdelay=0;
	float curdelay=0;
	int attrange=0;
	float attspeed=0;
	float movespeed=0;

	int growhp=0;
	int growmana=0;
	int growAtt=0;
	int growCri=0;
	int growCriPro=0;

	int team = -1; // 0 for blue team, 1 for red team
	bool ready;
	
	vector<int> itemList{ 0,0,0,0,0,0 };


	stack<pair<int, int>> assistList;
	LPPER_HANDLE_DATA handle;
	LPPER_IO_DATA ioinfo;

	Client& operator=(const Client& other) {
		if (this != &other) {
			socket = other.socket;
			champindex = other.champindex;
			user_name = other.user_name;
			channel = other.channel;
			room = other.room;
			code = other.code;
			clientindex = other.clientindex;
			kill = other.kill;
			death = other.death;
			assist = other.assist;
			x = other.x;
			y = other.y;
			z = other.z;
			gold = other.gold;
			rotationX = other.rotationX;
			rotationY = other.rotationY;
			rotationZ = other.rotationZ;
			level = other.level;
			maxexp = other.maxexp;
			exp = other.exp;
			curhp = other.curhp;
			maxhp = other.maxhp;
			curmana = other.curmana;
			maxmana = other.maxmana;
			attack = other.attack;
			critical = other.critical;
			criProbability = other.criProbability;
			maxdelay = other.maxdelay;
			curdelay = other.curdelay;
			attrange = other.attrange;
			attspeed = other.attspeed;
			movespeed = other.movespeed;
			growhp = other.growhp;
			growmana = other.growmana;
			growAtt = other.growAtt;
			growCri = other.growCri;
			growCriPro = other.growCriPro;
			team = other.team;
			ready = other.ready;
			itemList = other.itemList;
			assistList = other.assistList;
		}
		return *this;
	}
};

#pragma pack(push,1)
struct ClientInfo
{
	int socket;
	int champindex;
	int gold;
	float x;
	float y;
	float z;
	int kill;
	int death;
	int assist;
	int level;
	int curhp;
	int maxhp;
	int curmana;
	int maxmana;
	int attack;
	int critical;
	int criProbability;
	float attspeed;
	int attrange;
	float movespeed;
};
#pragma pack(pop)

#pragma pack(push,1)
struct ClientMovestart
{
	int socket;
	float rotationX;
	float rotationY;
	float rotationZ;
};
#pragma pack(pop)

#pragma pack(push,1)
struct nsHeader
{
	int size;
	int number;
};
#pragma pack(pop)

#pragma pack(push,1)
struct MouseInfo
{
	float x;
	float y;
	float z;
	int kind=-1; /// 0: Player, 1:Structure, 2:Unit

};
#pragma pack(pop)

#pragma pack(push,1)
struct AttInfo {
	int attacker;
	int attacked;
	int kind;
	int object_kind;
	int assist1=-1;
	int assist2 = -1;
	int assist3 = -1;
	int assist4 = -1;

};
#pragma pack(pop)

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
	chrono::high_resolution_clock::time_point lastUpdateTime;
	std::shared_ptr<asio::steady_timer> timer;
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
struct Bullet {
	double x;
	double y;
	double z;
	int dmg;
};
#pragma pack(pop)

#pragma pack(push,1)
struct MatchResult {
	string spaceId;
	string state; // dodge: 비정상적인 상황, success: 정상적인 상황
	int channel;
	int room;
	string winTeam;
	string loseTeam;

	vector<Client*> blueTeams;
	vector<Client*> redTeams;

	string dateTime;
	int gameDuration;
};
#pragma pack(pop)


#pragma pack(push,1)
struct ItemSlots {
	int socket;
	int id_0;
	int id_1;
	int id_2;
	int id_3;
	int id_4;
	int id_5;
};
#pragma pack(pop)

struct UserData {
	string user_index;
	string user_name;
};

struct RoomData {
	string spaceId;
	int isGame=-1; // -1:empty room, 0:pick room, 1:game room

	int channel;
	int room;

	vector<UserData> redTeam;
	vector<UserData> blueTeam;

};


struct Unit {
	int index;
	int unit_kind=-1; // 병종 종류
	int team=-1; // red, blue
	int curhp=0;
	int maxhp=0;
	float x=0;
	float y=0;
	float z=0;
	float maxdelay=0;
	float curdelay=0;
	int attrange=0;
	int bulletdmg=0;
	float bulletspeed=0;

};

struct UnitInfo {
	int index;
	int kind = -1; // 병종 종류
	int team = -1; // red, blue
	int curhp = 0;
	int maxhp = 0;
	float x = 0;
	float y = 0;
	float z = 0;
	int attrange = 0;
	int bulletdmg = 0;
	float bulletspeed = 0;

};

struct BulletInfo {
	float targetX;
	float targetY;
	float targetZ;

	float directionX;
	float directionY;
	float directionZ;

	float moveDistance;
	std::shared_ptr<asio::steady_timer> timer;
};