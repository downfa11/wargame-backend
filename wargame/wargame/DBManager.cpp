#include "DBManager.h"

DatabaseManager::DatabaseManager() {
    mysql_init(&mysql);
    if (!mysql_real_connect(&mysql, HOST, DB_USER, DB_PASSWORD, DB_NAME, PORT, NULL, 0)) {
        std::cerr << "Database connection error: " << mysql_error(&mysql) << std::endl;
    }
}

DatabaseManager::~DatabaseManager() {
    mysql_close(&mysql);
}

bool DatabaseManager::ExecuteQuery(const std::string& query, void (*processRow)(MYSQL_ROW)) {
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
    }
    else {
        std::cerr << "Failed to execute query: " << mysql_error(&mysql) << std::endl;
    }
    return false;
}