#pragma once
#include "base.h"

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

class PacketManger
{
public:
	static void Send(int cli_sock, int number, void* data, int size);
	static void Send(int cli_sock, int number);
};