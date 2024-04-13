#include"Resource.h"



vector<itemStats> ItemSystem::items;
vector<ChampionStats> ChampionSystem::champions;


void ChampionSystem::ChampionInit() {
	MYSQL mysql;
	MYSQL_RES* result;
	MYSQL_ROW row;

	mysql_init(&mysql);
	if (mysql_real_connect(&mysql, "127.0.0.1", "mysqluser", "mysqlpw", "wargame", 3306, NULL, 0)) {
		const char* query = "SELECT * FROM champion_stats";
		if (mysql_query(&mysql, query) == 0) {
			result = mysql_store_result(&mysql);
			if (result) {
				while ((row = mysql_fetch_row(result))) {
					ChampionStats champion;

					champion.index = stoi(row[0]);
					champion.name = row[1];
					champion.maxhp = stoi(row[2]);
					champion.maxmana = stoi(row[3]);
					champion.attack = stoi(row[4]);
					champion.movespeed = stof(row[5]);
					champion.maxdelay = stof(row[6]);
					champion.attspeed = stof(row[7]);
					champion.attrange = stoi(row[8]);
					champion.critical = stof(row[9]);
					champion.criProbability = stof(row[10]);
					champion.growHp = stoi(row[11]);
					champion.growMana = stoi(row[12]);
					champion.growAtt = stoi(row[13]);
					champion.growCri = stoi(row[14]);
					champion.growCriPob = stoi(row[15]);
					champions.push_back(champion);
				}
				mysql_free_result(result);
				cout << "Champs init." << endl;
			}
		}
		else {
			cerr << "Failed to execute query: " << mysql_error(&mysql) << endl;
		}
		mysql_close(&mysql);
	}
	else {
		cerr << "mysql error: " << mysql_error(&mysql) << endl;
	}
}


void ItemSystem::ItemInit() {
	MYSQL mysql;
	MYSQL_RES* result;
	MYSQL_ROW row;

	mysql_init(&mysql);
	if (mysql_real_connect(&mysql, "127.0.0.1", "mysqluser", "mysqlpw", "wargame", 3306, NULL, 0)) {
		const char* query = "SELECT * FROM item_stats";
		if (mysql_query(&mysql, query) == 0) {
			result = mysql_store_result(&mysql);
			if (result) {
				while ((row = mysql_fetch_row(result))) {
					itemStats item;

					item.id = std::stoi(row[0]);
					item.name = row[1];
					item.gold = std::stoi(row[2]);
					item.maxhp = std::stoi(row[3]);
					item.attack = std::stoi(row[4]);
					item.movespeed = std::stof(row[5]);
					item.maxdelay = std::stof(row[6]);
					item.attspeed = std::stof(row[7]);
					item.criProbability = std::stoi(row[8]);

					// critical은 150%
					items.push_back(item);
				}
				mysql_free_result(result);
				cout << "item init." << endl;
			}
		}
		else {
			cerr << "Failed to execute query: " << mysql_error(&mysql) << endl;
		}
		mysql_close(&mysql);
	}
	else {
		cerr << "mysql error: " << mysql_error(&mysql) << endl;
	}
}