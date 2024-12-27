#pragma once

#include "base.h"
#include "Client.h"
#include "Unit.h"

#include <list>

class GameSession;

class UnitManager {
public:
	explicit UnitManager(GameSession* session) : session(session) {}

	void NewUnit(int client_socket, UnitKind unit_kind);
	void UnitStat(int client_socket, int unit_index, UnitKind unit_kind);
	void UnitStat(Unit* unit);
	void UnitDie(int client_socket, int unit_index, UnitKind unit_kind);
	void UnitDie(Unit* unit);
	void UnitMoveStart(int client_socket, UnitMovestart* info);
	void UnitMove(int client_socket, UnitInfo info);
	void UnitMoveStop(int client_socket, UnitInfo info);

private:
	GameSession* session;
	int unit_count = 0;

	Client* FindClientBySocket(int socket, std::list<Client*>& clients);
	Unit* FindUnitByIndex(int socket, int index, std::list<Unit*>& units);
};
