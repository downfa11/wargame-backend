#pragma once

#include<mysql.h>

#include <functional>
#include <iostream>
#include <string>

#pragma comment(lib,"libmysql.lib")

class DatabaseManager {
public:
    DatabaseManager();
    ~DatabaseManager();
    bool ExecuteQuery(const std::string& query, void(*processRow) (MYSQL_ROW));

private:
    MYSQL mysql;
    static constexpr const char* HOST = "127.0.0.1";
    static constexpr const char* DB_USER = "mysqluser";
    static constexpr const char* DB_PASSWORD = "mysqlpw";
    static constexpr const char* DB_NAME = "wargame";
    static constexpr unsigned int PORT = 3306;
};