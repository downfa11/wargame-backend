#include "Resource.h"
#include "DBManager.h"

DatabaseManager dbManager;

void ChampionSystem::ChampionInit() {
    const std::string query = "SELECT * FROM champion_stats";
    dbManager.ExecuteQuery(query, ChampionSystem::getChampionData);
    std::cout << "Champion init." << std::endl;
}

void ItemSystem::ItemInit() {
    const std::string query = "SELECT * FROM item_stats";
    dbManager.ExecuteQuery(query, ItemSystem::getItemData);
    std::cout << "Item init." << std::endl;
}

void ChampionSystem::getChampionData(MYSQL_ROW row) {
    ChampionStats champion;
    champion.index = std::stoi(row[0]);
    champion.name = row[1];
    champion.maxhp = std::stoi(row[2]);
    champion.maxmana = std::stoi(row[3]);
    champion.attack = std::stoi(row[4]);
    champion.movespeed = std::stof(row[5]);
    champion.maxdelay = std::stof(row[6]);
    champion.attspeed = std::stof(row[7]);
    champion.attrange = std::stoi(row[8]);
    champion.critical = std::stof(row[9]);
    champion.criProbability = std::stof(row[10]);
    champion.growHp = std::stoi(row[11]);
    champion.growMana = std::stoi(row[12]);
    champion.growAtt = std::stoi(row[13]);
    champion.growCri = std::stoi(row[14]);
    champion.growCriPob = std::stoi(row[15]);
    champions.push_back(champion);
}

void ItemSystem::getItemData(MYSQL_ROW row) {
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
    items.push_back(item);
}