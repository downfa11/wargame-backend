#pragma once
#include "base.h"
#include "Client.h"
#include "Structure.h"

#include <list>
#include <shared_mutex>
#include <memory>


class GameSession;

class StructureManager {
public:
	explicit StructureManager(GameSession* session) : session(session) {}
	
	void NewStructure(int index, int team, int struct_kind, int chan, int room, int x, int y, int z);
	void StructureDie(int index, int team, int struct_kind, int chan, int room);
	void StructureStat(int index, int team, int struct_kind, int chan, int room);

	void TurretSearch(int index, int chan, int room);
	void Well(Client* client, int x, int y, int z);

private:
	GameSession* session;
	int attackCount = 0;


	Client* FindClientBySocket(int socket, std::list<Client*>& clients);
	Structure* FindStructureByIndex(int index, std::list<Structure*>& structures);
	void CreateBullet(Structure* attacker, Client* target);
	void CreateBullet(Structure* attacker, Unit* target);
	void MoveBulletAsync(Bullet* newBullet, Client* attacked, Structure* attacker);
	void MoveBulletAsync(Bullet* newBullet, Unit* attacked, Structure* attacker);

};