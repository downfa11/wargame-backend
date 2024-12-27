#pragma once

#include <winsock2.h>
#include <windows.h>
#include <process.h>
#include <ws2tcpip.h>

#include <stack>
#include <string>
#include <vector>
#include <chrono>

#pragma comment(lib,"ws2_32.lib")

#define MAX_CLIENT 5000
#define MAX_CHANNEL_COUNT 2
#define MAX_ROOM_COUNT_PER_CHANNEL 100
#define MAX_CLIENT_PER_ROOM 2
#define MAX_TEAM_PER_ROOM 5 // MAX_CLIENT_PER_ROOM/2

typedef unsigned int       UINT;
typedef unsigned long       DWORD;
typedef unsigned short      WORD;
typedef unsigned char       BYTE;

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
	std::string user_name = "";

	time_t out_time = 0;
	int channel = 0;
	int room = 0;
	std::string code = "";
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

	std::chrono::high_resolution_clock::time_point lastUpdateTime;

	int curhp=0;
	int maxhp=0;
	int curmana=0;
	int maxmana=0;
	int attack=0;
	float absorptionRate = 0;
	int defense=0;
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
	
	std::vector<int> itemList{ 0,0,0,0,0,0 };


	std::stack<std::pair<int, int>> assistList;
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
			absorptionRate = other.absorptionRate;
			defense = other.defense;
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
	float absorptionRate;
	int defense;
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
struct UserData {
	std::string user_index;
	std::string user_name;
};
#pragma pack(pop)

#pragma pack(push,1)
struct RoomData {
	std::string spaceId;
	int isGame = -1; // -1:empty room, 0:pick room, 1:game room

	int channel;
	int room;

	std::vector<UserData> redTeam;
	std::vector<UserData> blueTeam;

};
#pragma pack(pop)