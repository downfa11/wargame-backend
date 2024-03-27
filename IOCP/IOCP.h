#pragma once
#include "base.h"
#include "PacketManager.h"
#include "GameManager.h"


unsigned WINAPI EchoThreadMain(LPVOID pComPort); //DWORD
void ErrorHandling(const char* message);
void IOCPInit();
void CommendInput();
void AcceptThread();
void TimeOutCheckThread();
void LobbyLogic();
void MatchMaking();
using namespace std;