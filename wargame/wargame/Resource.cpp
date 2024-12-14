#include "Resource.h"

#include "DBManager.h"


DatabaseManager dbManager;

std::vector<ChampionStats> ChampionSystem::champions;
std::vector<itemStats> ItemSystem::items;

bool DatabaseManager::ExecuteQuery(const std::string& query, void (*processRow)(MYSQL_ROW)) {
    mysql_init(&mysql);
    if (!mysql_real_connect(&mysql, HOST, DB_USER, DB_PASSWORD, DB_NAME, PORT, NULL, 0)) {
        std::cerr << "Database connection error: " << mysql_error(&mysql) << std::endl;
    }

    if (mysql_query(&mysql, query.c_str()) == 0) {
        MYSQL_RES* result = mysql_store_result(&mysql);
        if (result) {
            MYSQL_ROW row;
            while ((row = mysql_fetch_row(result))) {
                processRow(row);
            }
            mysql_free_result(result);
            return true;
        }
        else {
            std::cerr << "Failed to store result: " << mysql_error(&mysql) << std::endl;
        }
        mysql_close(&mysql);
    }
    else {
        std::cerr << "Failed to execute query: " << mysql_error(&mysql) << std::endl;
    }
    return false;
}

void ChampionSystem::ChampionInit() {
    const std::string query = "SELECT * FROM champion_stats";
    dbManager.ExecuteQuery(query, ChampionSystem::GetChampionData);
    std::cout << "Champion init." << std::endl;
}

void ItemSystem::ItemInit() {
    const std::string query = "SELECT * FROM item_stats";
    dbManager.ExecuteQuery(query, ItemSystem::GetItemData);
    std::cout << "Item init." << std::endl;
}

void ChampionSystem::GetChampionData(MYSQL_ROW row) {
    ChampionStats champion;
    champion.index = std::stoi(row[0]);
    champion.name = row[1];
    champion.maxhp = std::stoi(row[2]);
    champion.maxmana = std::stoi(row[3]);
    champion.attack = std::stoi(row[4]);
    champion.absorptionRate = std::stof(row[5]);
    champion.defense = std::stoi(row[6]);
    champion.movespeed = std::stof(row[7]);
    champion.maxdelay = std::stof(row[8]);
    champion.attspeed = std::stof(row[9]);
    champion.attrange = std::stoi(row[10]);
    champion.critical = std::stof(row[11]);
    champion.criProbability = std::stof(row[12]);
    champion.growHp = std::stoi(row[13]);
    champion.growMana = std::stoi(row[14]);
    champion.growAtt = std::stoi(row[15]);
    champion.growCri = std::stoi(row[16]);
    champion.growCriPob = std::stoi(row[17]);
    champions.push_back(champion);
}

void ItemSystem::GetItemData(MYSQL_ROW row) {
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
    item.absorptionRate = std::stof(row[9]);
    item.defense = std::stof(row[10]);
    items.push_back(item);
}


ChampionStatsInfo ChampionSystem::GetChampionInfo(const ChampionStats& champion) {
    return ChampionStatsInfo{
        champion.index,
        champion.attack,
        champion.absorptionRate,
        champion.defense,
        champion.maxhp,
        champion.maxmana,
        champion.movespeed,
        champion.maxdelay,
        champion.attspeed,
        champion.attrange,
        champion.critical,
        champion.criProbability
    };
}

ItemStatsInfo ItemSystem::GetItemInfo(const itemStats& item) {
    return ItemStatsInfo{
        item.id,
        item.gold,
        item.attack,
        item.maxhp,
        item.movespeed,
        item.maxdelay,
        item.attspeed,
        item.criProbability,
        item.absorptionRate,
        item.defense
    };
}