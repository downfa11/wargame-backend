#include "base.h"
#include "PacketManager.h"
#include "GameManager.h"
#include "Resource.h"

#include <kafka/KafkaConsumer.h>

#include <tchar.h>
#include <DbgHelp.h> 

using namespace kafka;
using namespace kafka::clients::consumer;

SOCKET hServSock;
HANDLE hComPort;


VOID ShowDumpLastError(DWORD error = GetLastError()) {
	TCHAR* lpOSMsg;
	// TCHAR은 자동으로 char와 유니코드의 wchar_t로 변환해주는 매크로
	FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
		NULL, error, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), (LPWSTR)&lpOSMsg, 0, NULL);
	// FORMAT_MESSAGE_ALLOCATE_BUFFER : 함수에서 할당된 버퍼를 사용해서 문자열을 채운다. LocalFree로 해제해야함
	// FORMAT_MESSAGE_FROM_SYSTEM : 시스템 에러 코드 테이블에서 메시지를 가져온다.
	// FORMAT_MESSAGE_IGNORE_INSERTS : 문자열에 삽입 문자열을 무시합니다.

	// Long Pointer to String, Wide String
	// LPSTR은 ansi 문자열. lpwstr은 Wide 문자열을 말하는데, 유니코드 문자 집합(각 2bytes)로 표현
	// const wchar_t* wideString = L"Hello";      L : wide 문자열임을 나타냅니다.


	_tprintf(_T("[ERROR] [%lu] %s\n"), error, lpOSMsg);
	LocalFree(lpOSMsg);
	// 주로 LocalAlloc로 할당한 메모리를 해제할때 사용한다. 
}

LPTOP_LEVEL_EXCEPTION_FILTER PreviousExceptionFilter = NULL;

typedef BOOL(WINAPI* MINIDUMPWRITEDUMP) (
	HANDLE hProcess,
	DWORD dwPid,
	HANDLE hFile,
	MINIDUMP_TYPE DumpType,
	// MiniDumpNormal : 기본적인 미니덤프를 생성, 스레드 및 모듈 정보, 스택, 힙의 일부 등 기본적인 시스템 정보를 포함
	// MiniDumpWithFullMemory : 전체 메모리 덤프를 생성. 오래걸려
	//  MiniDumpWithHandleData : 핸들 정보를 포함하는 덤프를 생성. 프로세스에서 열린 핸들의 목록을 포함
	// ex) MINIDUMP_TYPE dumpType = MiniDumpNormal | MiniDumpWithFullMemory;
	CONST PMINIDUMP_EXCEPTION_INFORMATION ExceptionParam,
	CONST PMINIDUMP_USER_STREAM_INFORMATION UserStreamParam,
	CONST PMINIDUMP_CALLBACK_INFORMATION CallbackParam
	);


LONG WINAPI UnHandledExceptionFilter(_EXCEPTION_POINTERS* exceptionInfo) {
	HMODULE DllHandle = LoadLibrary(_T("DBGHELP.DLL"));
	if (DllHandle == NULL) {
		ShowDumpLastError();
		return EXCEPTION_CONTINUE_SEARCH;
	}

	MINIDUMPWRITEDUMP dump = (MINIDUMPWRITEDUMP)GetProcAddress(DllHandle, "MiniDumpWriteDump");
	if (dump == NULL) {
		ShowDumpLastError();
		return EXCEPTION_CONTINUE_SEARCH;
	}

	SYSTEMTIME SystemTime;
	GetLocalTime(&SystemTime);

	TCHAR DumpPath[MAX_PATH] = { 0, };

	_sntprintf_s(DumpPath, MAX_PATH, _TRUNCATE, _T("%d-%d-%d %d_%d_%d.dmp"),
		SystemTime.wYear,
		SystemTime.wMonth,
		SystemTime.wDay,
		SystemTime.wHour,
		SystemTime.wMinute,
		SystemTime.wSecond);

	HANDLE FileHandle = CreateFile(DumpPath, GENERIC_WRITE, FILE_SHARE_WRITE, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
	if (FileHandle == INVALID_HANDLE_VALUE) {
		ShowDumpLastError();
		return EXCEPTION_CONTINUE_SEARCH;
	}

	_MINIDUMP_EXCEPTION_INFORMATION MiniDumpExceptionInfo;
	MiniDumpExceptionInfo.ThreadId = GetCurrentThreadId();
	MiniDumpExceptionInfo.ExceptionPointers = exceptionInfo;
	MiniDumpExceptionInfo.ClientPointers = NULL;

	BOOL Success = dump(GetCurrentProcess(), GetCurrentProcessId(), FileHandle, MiniDumpNormal, &MiniDumpExceptionInfo, NULL, NULL);
	if (Success) {
		CloseHandle(FileHandle);
		return EXCEPTION_EXECUTE_HANDLER;
	}
	CloseHandle(FileHandle);

	return EXCEPTION_CONTINUE_SEARCH;
}

BOOL BeginDump(VOID) // Param VOID는 매개변수 없는거
{
	//잘못된 연산 메시지 창이 나오지 않도록 설정
	SetErrorMode(SEM_FAILCRITICALERRORS);

	//Unhandled Exception 발생시 호출될 CallBack 함수 등록
	//PreviousExceptionFilter는 원래 설정을 담아 프로그램 종료시 복구 용도로 쓰입니다.
	PreviousExceptionFilter = SetUnhandledExceptionFilter((LPTOP_LEVEL_EXCEPTION_FILTER)UnHandledExceptionFilter);

	return TRUE;
}

BOOL EndDump(VOID)
{
	//프로그램 종료 전에 원래 설정으로 복구
	SetUnhandledExceptionFilter(PreviousExceptionFilter);
	return TRUE;
}

void ErrorHandling(const char* message)
{
	cout << message<< " :" << WSAGetLastError()<<endl;
	exit(1);
}

unsigned WINAPI EchoThreadMain(LPVOID pComPort)
{
	HANDLE hComPort = (HANDLE)pComPort;
	SOCKET sock;
	DWORD bytesTrans;
	LPPER_HANDLE_DATA handleInfo;
	LPPER_IO_DATA ioInfo;
	DWORD flags = 0;
	void* data;

	while (true)
	{
		BOOL result = GetQueuedCompletionStatus(hComPort, &bytesTrans, (PULONG_PTR)&handleInfo, (LPOVERLAPPED*)&ioInfo, INFINITE);
		sock = handleInfo->hClntSock;

		if (sock == INVALID_SOCKET)
		{
			cout << "Invalid socket" << endl;
			delete handleInfo;
			delete ioInfo;
			continue;
		}
		if (ioInfo->rwMode == READ)
		{
			if (bytesTrans == 0)
			{
				GameManager::ClientClose(sock);
				delete handleInfo;
				delete ioInfo;
				continue;
			}

			int remain_byte = bytesTrans;
			int bbb = 0;
			while (remain_byte != 0)
			{
				if (ioInfo->header_recv == false)
				{
					if (remain_byte < 8)
					{
						cout << "헤더조각을 다 못모았을경우 " << remain_byte << endl;

						memcpy(&ioInfo->broken_data[ioInfo->broken_data_size], ioInfo->wsaBuf.buf, remain_byte); //조각 채우기
						ioInfo->broken_data_size += remain_byte;
						ioInfo->header_broken = true;

						if (ioInfo->broken_data_size < 8) //덜모음
							break;
						else
							ioInfo->header_recv = true; //다 모음
					}
					else
					{
						ioInfo->header_broken = false;
						ioInfo->header_recv = true;
					}
				}

				int size;
				int number;

				if (ioInfo->header_broken)
				{
					memcpy(&size, ioInfo->broken_data, sizeof(int));
					memcpy(&number, ioInfo->broken_data + sizeof(int), sizeof(int));
				}
				else
				{
					memcpy(&size, ioInfo->wsaBuf.buf + bbb, sizeof(int));
					memcpy(&number, ioInfo->wsaBuf.buf + sizeof(int) + bbb, sizeof(int));
				}

				bbb += sizeof(nsHeader);


				if (ioInfo->data_broken)
				{
					int a = remain_byte < size ? remain_byte : size; //남은게 size보다 적을경우
					memcpy(&ioInfo->broken_data[ioInfo->broken_data_size], ioInfo->wsaBuf.buf + bbb, a);
					ioInfo->broken_data_size += a;

					cout << "Broken packet recv" << endl;
					if (ioInfo->broken_data_size < size + sizeof(nsHeader))
						break;
				}
				else
				{
					if (remain_byte < size + sizeof(nsHeader))
					{
						memcpy(&ioInfo->broken_data[ioInfo->broken_data_size], ioInfo->wsaBuf.buf + bbb, remain_byte);
						ioInfo->broken_data_size += remain_byte;

						ioInfo->data_broken = true;
						cout << "Broken packet" << endl;
						break;
					}
				}

				remain_byte -= 8 + size;

				if (ioInfo->data_broken)
				{
					cout << "Broken packet used" << endl;
					data = ioInfo->broken_data + sizeof(nsHeader);
				}
				else
				{
					data = ioInfo->wsaBuf.buf + sizeof(nsHeader);
				}


				if (number == H_CHAT)
				{
					GameManager::ClientChat(sock, size, data);
				}
				else if (number == H_CHANNEL_MOVE)
				{
					GameManager::ClientChanMove(sock, data);
				}
				else if (number == H_MOVE)
				{
					GameManager::ClientMove(sock, data);
				}
				else if (number == H_MOVESTART)
				{
					GameManager::ClientMoveStart(sock, data);
				}
				else if (number == H_MOVESTOP)
				{
					GameManager::ClientMoveStop(sock, data);
				}
				else if (number == H_RAUTHORIZATION) {
					GameManager::RoomAuth(sock,size, data);
				}
				else if (number == H_CAUTHORIZATION) {
					GameManager::ClientAuth(sock, data);
				}
				else if (number == H_TIMEOUT_SET)
				{
					GameManager::ClientTimeOutSet(sock);
				}
				else if (number == H_ROOM_MOVE) {
					GameManager::ClientRoomMove(sock, data);
				}
				else if (number == H_IS_READY) {
					GameManager::ClientReady(sock, size, data);
				}
				else if (number == H_CLIENT_STAT) {
					GameManager::ClientStat(sock);
				}
				else if (number == H_ATTACK_TARGET) {
					GameManager::MouseSearch(sock, data);
				}
				else if (number == H_ATTACK_CLIENT) {
					GameManager::AttackClient(sock, data);
				}
				else if (number == H_ATTACK_STRUCT) {
					GameManager::AttackStructure(sock, data);
				}
				else if (number == H_CLIENT_STAT) {
					GameManager::ClientChampInit(sock);
				}
				else if (number == H_BUY_ITEM) {
					GameManager::ItemStat(sock, data);
				}
				else if (number == H_WELL) {
					GameManager::Well(sock, data);
				}

				ioInfo->header_broken = false;
				ioInfo->data_broken = false;
				ioInfo->header_recv = false;
				ioInfo->broken_data_size = 0;

				if (remain_byte != 0)
				{
					cout << "Broken packet " << remain_byte << " " << number << endl;
					bbb += size;
				}
				if (remain_byte < 0)
				{
					cout << "remain_byte < 0" << endl;
					break;
				}
			}
			memset(&(ioInfo->overlapped), 0, sizeof(OVERLAPPED));

			WSARecv(sock, &(ioInfo->wsaBuf), 1, NULL, &flags, &(ioInfo->overlapped), NULL);
		}
		else
		{
			delete[] ioInfo->wsaBuf.buf;
			delete ioInfo;
		}
	}
	return 0;
}

void IOCPInit()
{
	WSADATA wsaData;

	if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0)
		ErrorHandling("WSAStartup() error!");

	hComPort = CreateIoCompletionPort(INVALID_HANDLE_VALUE, NULL, 0, 0);

	SYSTEM_INFO sysInfo;
	GetSystemInfo(&sysInfo);

	printf("PROCESSE %d \n", sysInfo.dwNumberOfProcessors);

	for (size_t i = 0; i < sysInfo.dwNumberOfProcessors; i++)
		_beginthreadex(NULL, 0, EchoThreadMain, (LPVOID)hComPort, 0, NULL);

	hServSock = WSASocketW(AF_INET, SOCK_STREAM, 0, NULL, 0, WSA_FLAG_OVERLAPPED);

	if (hServSock == SOCKET_ERROR)
		ErrorHandling("WSASocketW error!");

	SOCKADDR_IN servAdr;
	memset(&servAdr, 0, sizeof(servAdr));

	servAdr.sin_family = AF_INET;
	servAdr.sin_addr.S_un.S_addr = htonl(INADDR_ANY);
	servAdr.sin_port = htons(25565);

	::bind(hServSock, (SOCKADDR*)&servAdr, sizeof(servAdr));

	if (listen(hServSock, 5) == SOCKET_ERROR)
		ErrorHandling("listen error");
}

void TimeOutCheckThread()
{
	while (true)
	{
		GameManager::TimeOutCheck();
		Sleep(1000);
	}
}

void AcceptThread()
{
	LPPER_HANDLE_DATA handleInfo;

	while (true)
	{
		handleInfo = new PER_HANDLE_DATA;
		int addrlen = sizeof(SOCKADDR_IN);

		SOCKET hClntSock = accept(hServSock, (SOCKADDR*)&handleInfo->clntAdr, &addrlen);
		handleInfo->hClntSock = hClntSock;



		CreateIoCompletionPort((HANDLE)hClntSock, hComPort, (SIZE_T)handleInfo, 0);
		LPPER_IO_DATA ioInfo = new PER_IO_DATA;
		memset(&(ioInfo->overlapped), 0, sizeof(OVERLAPPED));
		ioInfo->wsaBuf.len = BUF_SIZE;
		ioInfo->wsaBuf.buf = new CHAR[BUF_SIZE];
		memset(ioInfo->wsaBuf.buf, 0, BUF_SIZE);
		ioInfo->rwMode = READ;

		DWORD recvBytes = 0, flags = 0;
		WSARecv(handleInfo->hClntSock, &(ioInfo->wsaBuf), 1, &recvBytes, &flags, &(ioInfo->overlapped), NULL);

		GameManager::NewClient(hClntSock, handleInfo, ioInfo);

	}
}

void CommendInput()
{
	while (true)
	{
		char val[100];
		bool what = false;
		cout << "$ ";
		cin >> val;

		string m = "stop";
		if (!strcmp(val, m.c_str()))
		{
			puts("Stopping Server...");
			cout << EndDump() << endl;
			what = true;
			break;
		}
		m = "help";
		if (!strcmp(val, m.c_str()))
		{
			cout << endl;
			cout << "client_list_all : " << GameManager::client_list_all.size() << endl;

			cout << "auth_data : " << GameManager::auth_data.size() << endl;
			cout << endl;
			cout << "stop : Stop server" << endl;
			cout << "clicount : Client count check" << endl;
			cout << "alive : auth_room in alive" << endl;
			cout << "kick {n} : Kick {n}client" << endl;
			cout << "detail {n} : {n}'s info" << endl;
			cout << "roominfo {channel} {room} : space info" << endl;
			cout << "chatlog {channel} {room} : space chat log" << endl;
			cout << "say {channel} {room} {s} : Tell all clients" << endl;
			cout << endl;
			what = true;
		}
		m = "clicount";
		if (!strcmp(val, m.c_str()))
		{
			printf("client_list_all Count : %d \n", GameManager::client_list_all.size());
			what = true;
		}

		m = "kick";
		if (!strcmp(val, m.c_str()))
		{
			int kill_number;
			cin >> kill_number;

			for (auto inst : GameManager::client_list_all)
			{
				if (inst->socket == kill_number)
				{
					cout << "kick " << kill_number << " (" << inst->user_name << ")" << endl;
					GameManager::ClientClose(inst->socket);
					break;
				}

			}



			what = true;
		}

		m = "detail";
		if (!strcmp(val, m.c_str()))
		{
			int client_socket;
			cin >> client_socket;

			bool foundUser = false;

			for (auto inst : GameManager::client_list_all)
			{
				if (inst->socket == client_socket)
				{
					foundUser = true;

					printf("CLIENT socket %d \n", inst->socket);
					printf("       champindex %d \n", inst->champindex);
					printf("       clientindex %d \n", inst->clientindex);
					printf("       name %s \n", inst->user_name.c_str());
					printf("       channel %d \n", inst->channel);
					printf("       room %d \n", inst->room);
					printf("\n");
					printf("       kill %d \n", inst->kill);
					printf("       death %d \n", inst->death);
					printf("       assist %d \n", inst->assist);
					printf("       x %f \n", inst->x);
					printf("       y %f \n", inst->y);
					printf("       z %f \n", inst->z);
					printf("       gold %d \n", inst->gold);
					printf("       level %f \n", inst->level);
					printf("       maxexp %d \n", inst->maxexp);
					printf("       exp %d \n", inst->exp);
					printf("\n");
					printf("       curhp %d \n", inst->curhp);
					printf("       maxhp %d \n", inst->maxhp);
					printf("       curmana %d \n", inst->curmana);
					printf("       maxmana %d \n", inst->maxmana);
					printf("       attack %d \n", inst->attack);
					printf("       critical %d \n", inst->critical);
					printf("       criProbability %d \n", inst->criProbability);
					printf("       maxdelay %f \n", inst->maxdelay);
					printf("       curdelay %f \n", inst->curdelay);
					printf("       attrange %d \n", inst->attrange);
					printf("       attspeed %f \n", inst->attspeed);
					printf("       movespeed %f \n", inst->movespeed);
					printf("\n");
					printf("       growhp %d \n", inst->growhp);
					printf("       growmana %d \n", inst->growmana);
					printf("       growAtt %d \n", inst->growAtt);
					printf("       growCri %d \n", inst->growCri);
					printf("       growCriPro %d \n", inst->growCriPro);
					printf("       team %d \n", inst->team);
					printf("       ready %d \n", inst->ready);
					printf("\n");
					printf("       itemList: ");
					for (const auto& item : inst->itemList)
					{
						printf("%d ", item);
					}
					printf("\n");

					printf("       assistList: ");
					while (!inst->assistList.empty())
					{
						auto pair = inst->assistList.top();
						printf("(%d, %f) ", pair.first, pair.second);
						inst->assistList.pop();
					}

					printf("\n");

				}

			}

			if (!foundUser)
				cout << "User " << client_socket << " not found." << endl;

			what = true;
		}

		m = "roominfo";
		if (!strcmp(val, m.c_str()))
		{
			int channelIndex, roomIndex;
			cin >> channelIndex >> roomIndex;

			auto clientList = GameManager::GetClientListInRoom(channelIndex, roomIndex);
			printf("Client List in channel %d room %d\n", channelIndex, roomIndex);
			cout << "size : " << clientList.size() << endl;



			for (auto& room : GameManager::auth_data) {
				if (room.channel == channelIndex && room.room == roomIndex) {
					cout << " Red Team" << endl;
					for (auto& user : room.redTeam) {

						cout << "  User index: " << user.user_index << ", User Name: " << user.user_name << endl;
						for (auto client : clientList)
						{
							if (client->team == 1)
								cout << "- socket: " << client->socket << endl;
						}
					}
					cout << endl;
					cout << " Blue Team" << endl;
					for (auto& user : room.blueTeam) {

						cout << "  User index: " << user.user_index << ", User Name: " << user.user_name << endl;
						for (auto client : clientList)
						{
							if (client->team == 0)
								cout << "- socket: " << client->socket << endl;
						}
					}
					cout << endl;
				}
			}

			what = true;
		}
		m = "alive";
		if (!strcmp(val, m.c_str())) {

			cout << endl;
			int empty = MAX_CHANNEL_COUNT * MAX_ROOM_COUNT_PER_CHANNEL, pick = 0, inGame = 0;
			for (auto& inst : GameManager::auth_data) {
				if (inst.isGame != -1)
					empty--;

				if (inst.isGame == 0)
					pick++;
				else if(inst.isGame == 1)
					inGame++;
			}
			
			cout << "빈 게임 공간의 개수: "<< empty<<endl;
			cout << "픽중인 게임 공간의 개수: "<< pick<<endl;
			cout << "진행중인 게임 공간의 개수: "<< inGame<<endl;

			cout << endl;
			cout << "현재 진행중인 게임의 정보" << endl;
			int index = -1;
			for (auto& inst : GameManager::auth_data) {

				int chan = inst.channel;
				int room = inst.room;
				cout << " - #" <<++index<< endl;
				cout << "      " << chan << "번 채널 " << room << "번 게임 공간" << endl;
				cout << "      게임 공간의 id : " << inst.spaceId << endl;
				cout << "      게임 공간의 수용량 : " << GameManager::client_channel[chan].client_list_room[room].size() << endl;

				chrono::time_point<chrono::system_clock> endTime = chrono::system_clock::now();
				chrono::time_point<chrono::system_clock>& startTime = GameManager::client_channel[chan].startTime[room];

				chrono::duration<double> elapsed_seconds = endTime - startTime;
				int gameMinutes = static_cast<int>(elapsed_seconds.count());
				cout << "      현재 진행 시간 : " << gameMinutes <<"sec" << endl;
				cout << endl;
			}

			what = true;
		}

		m = "chatlog";
		if (!strcmp(val, m.c_str()))
		{
			int channelIndex, roomIndex;
			cin >> channelIndex >> roomIndex;

			auto chatLog = GameManager::GetChatLog(channelIndex, roomIndex);
			printf("CHAT LOG in channel %d room %d\n", channelIndex, roomIndex);
			cout << "size : " << chatLog.size() << endl;
			for (auto& chat : chatLog)
			{
				auto now = chat.second.first;
				time_t t = chrono::system_clock::to_time_t(now);

				struct tm timeinfo;
				localtime_s(&timeinfo, &t);

				cout << "- " << chat.first << " : " << chat.second.second << "         datetime:" << put_time(&timeinfo, "%F %T ") << endl;
			}

			what = true;
		}

		m = "say";
		if (!strcmp(val, m.c_str()))
		{
			int channelIndex, roomIndex;
			cin >> channelIndex >> roomIndex;

			string input;
			getline(cin, input);

			BYTE* packet_data = new BYTE[input.size()];
			int size = input.size();
			memcpy(packet_data, input.c_str(), size);

			for (auto cli : GameManager::client_channel[channelIndex].client_list_room[roomIndex])
				PacketManger::Send(cli->socket, H_NOTICE, packet_data, size);

			cout << "UTF-8 Encoded Notice ch." << channelIndex << " Room #" << roomIndex << " : " << input << endl;
			what = true;
		}


		if (what == false)
			cout << "You can enter help to know commands." << endl;
	}
}

vector<UserData> parseTeamData(const string& teamData) {
	vector<UserData> team;
	size_t pos = 1;
	size_t end = 0;
	while ((end = teamData.find(",", pos)) != string::npos || (end = teamData.find("]", pos)) != string::npos) {
		string userData = teamData.substr(pos, end - pos);
		size_t separator = userData.find(":");
		if (separator != string::npos) {
			UserData user;
			try {
				user.user_index = userData.substr(0, separator);
				cout << "user_index : " << user.user_index << endl;
			}
			catch (const invalid_argument& e) {
				cerr << "stoi 호출 실패: " << userData.substr(0, separator) << endl;
				pos = end + 1;
				continue;
			}
			user.user_name = userData.substr(separator + 1);
			cout << "user_name : " << user.user_name << endl;
			team.push_back(user);
		}
		pos = end + 1;
	}
	return team;
}

roomData matchParsing(const string& request) {
	size_t jsonStart = request.find("(");
	size_t jsonEnd = request.find_last_of(")");

	if (jsonStart != string::npos && jsonEnd != string::npos) {
		string jsonData = request.substr(jsonStart + 1, jsonEnd - jsonStart - 1);

		try {

			size_t spaceIdStart = jsonData.find("spaceId=") + 8;
			size_t spaceIdEnd = jsonData.find(", ", spaceIdStart);
			string spaceId = jsonData.substr(spaceIdStart, spaceIdEnd - spaceIdStart);

			size_t teamsStart = jsonData.find("teams={") + 7;
			size_t teamsEnd = jsonData.find_last_of("}");
			string teamsData = jsonData.substr(teamsStart, teamsEnd - teamsStart);

			size_t redTeamStart = teamsData.find("red=[") + 5;
			size_t redTeamEnd = teamsData.find("],", redTeamStart);
			string redTeamData = teamsData.substr(redTeamStart - 1, redTeamEnd - redTeamStart + 2);

			size_t blueTeamStart = teamsData.find("blue=[", redTeamEnd) + 6;
			size_t blueTeamEnd = teamsData.find("]", blueTeamStart);
			string blueTeamData = teamsData.substr(blueTeamStart - 1, blueTeamEnd - blueTeamStart + 2);

			roomData curRoom;
			curRoom.spaceId = spaceId;
			curRoom.redTeam = parseTeamData(redTeamData);
			cout << redTeamData << " parsing red team : " << (curRoom.redTeam.size()) << endl;
			curRoom.blueTeam = parseTeamData(blueTeamData);
			cout << blueTeamData << " parsing blue team : " << (curRoom.blueTeam.size()) << endl;

			return curRoom;
		}
		catch (const exception& e) {
			cerr << "Failed to parse data: " << e.what() << endl;
		}
	}
	else cerr << "Data not found in request." << endl;

	return roomData(); // 파싱 실패 시 빈 객체 반환
}


void KafkaConsumerThread() {
	Properties props({ {"bootstrap.servers", kafkaMessage::brokers} });
	KafkaConsumer consumer(props);
	consumer.subscribe({ kafkaMessage::matchTopic });

	while (1) {
		auto records = consumer.poll(chrono::milliseconds(500));
		for (const auto& record : records) {
			if (!record.error()) {
				cout << endl;
				string message = record.value().toString();

				cout << "kafka message : " << message << endl;
				roomData curRoom = matchParsing(message);
				if (curRoom.spaceId.empty()) {
					cout << "room create fail." << endl;
				}
				else {
					GameManager::auth_data.push_back(curRoom);
					cout << "room create success." << endl;

					if (!GameManager::findEmptyRoom(curRoom))
						cout << "channel and Room is full." << endl;
				}
;
			}
			else cerr << record.toString() << endl;

		}
	}
	consumer.close();
}

int main()
{
	setlocale(LC_ALL, "");
	BeginDump();
	
	IOCPInit();

	ChampionSystem::ChampionInit();
	ItemSystem::ItemInit();

	thread kafka_consumer_thread(KafkaConsumerThread);
	thread accept_thread(AcceptThread);
	thread time_out_thread(TimeOutCheckThread);

	EndDump();
	CommendInput();

	accept_thread.join();
	time_out_thread.join();

	return 0;
}