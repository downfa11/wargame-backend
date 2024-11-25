#pragma once

#include "base.h"
#include "MatchManager.h"

#include <string>

class UtilityManager {
public:
    static std::string clientToJson(const Client* client);
    static std::string matchResultToJson(const MatchResult& result);

    static float DistancePosition(float x1, float y1, float z1, float x2, float y2, float z2);
    static RoomData matchParsing(const std::string& request);
    static std::vector<UserData> parseTeamData(const std::string& teamData);
};
