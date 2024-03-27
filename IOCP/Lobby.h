#pragma once

#include <iostream>
#include <string>
#include<vector>
#include"base.h"

#include <mutex>
#include <winsock2.h>
#include <ws2tcpip.h>
#pragma comment(lib, "ws2_32.lib")

class Lobby {

public:

    static void start();
    static void SendToWebServer(string path, string request,size_t length);
};