#pragma once
#include "base.h"

class PacketManger
{
public:
	static void Send(int cli_sock, int number, void* data, int size);
	static void Send(int cli_sock, int number);
};