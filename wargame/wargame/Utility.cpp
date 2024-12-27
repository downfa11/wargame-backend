#include "Utility.h"

#include <sstream>
#include <cmath>
#include <iomanip>

float UtilityManager::DistancePosition(float x1, float y1, float z1, float x2, float y2, float z2)
{
	float dx = x1 - x2;
	float dy = y1 - y2;
	float dz = z1 - z2;

	return sqrt(dx * dx + dy * dy + dz * dz);
}

std::string UtilityManager::matchResultToJson(const MatchResult& result) {
	std::ostringstream oss;
	oss << "{";
	oss << "\"spaceId\": \"" << result.spaceId << "\",";
	oss << "\"state\": \"" << result.state << "\",";
	oss << "\"channel\": \"" << result.channel << "\",";
	oss << "\"room\": \"" << result.room << "\",";

	oss << "\"winTeam\": \"" << result.winTeam << "\",";
	oss << "\"loseTeam\": \"" << result.loseTeam << "\",";

	oss << "\"blueTeams\": [";
	for (size_t i = 0; i < result.blueTeams.size(); ++i) {
		oss << clientToJson(result.blueTeams[i]);
		if (i < result.blueTeams.size() - 1)
			oss << ",";
	}
	oss << "],";

	oss << "\"redTeams\": [";
	for (size_t i = 0; i < result.redTeams.size(); ++i) {
		oss << clientToJson(result.redTeams[i]);
		if (i < result.redTeams.size() - 1)
			oss << ",";
	}
	oss << "],";

	oss << "\"dateTime\": \"" << result.dateTime << "\",";
	oss << "\"gameDuration\": " << result.gameDuration;
	oss << "}";
	return oss.str();

}

std::string UtilityManager::clientToJson(const Client* client) {
	std::ostringstream oss;

	oss << "{";
	oss << "\"clientindex\": " << client->clientindex << ",";
	oss << "\"socket\": " << client->socket << ",";
	oss << "\"champindex\": " << client->champindex << ",";
	oss << "\"user_name\": \"" << client->user_name << "\",";
	oss << "\"team\": \"" << (client->team == 0 ? "blue" : "red") << "\",";
	oss << "\"kill\": " << client->kill << ",";
	oss << "\"death\": " << client->death << ",";
	oss << "\"assist\": " << client->assist << ",";
	oss << "\"level\": " << client->level << ",";
	oss << "\"maxhp\": " << client->maxhp << ",";
	oss << "\"maxmana\": " << client->maxmana << ",";
	oss << "\"attack\": " << client->attack << ",";
	oss << "\"critical\": " << client->critical << ",";
	oss << "\"criProbability\": " << client->criProbability << ",";
	oss << "\"attrange\": " << client->attrange << ",";
	oss << "\"attspeed\": " << client->attspeed << ",";
	oss << "\"movespeed\": " << client->movespeed << ",";

	oss << "\"itemList\": [";
	for (size_t i = 0; i < client->itemList.size(); ++i) {
		oss << "\"" << client->itemList[i] << "\"";
		if (i < client->itemList.size() - 1)
			oss << ",";
	}
	oss << "]";

	oss << "}";
	return oss.str();
}

std::vector<UserData> UtilityManager::parseTeamData(const std::string& teamData) {
	std::vector<UserData> team;
	size_t pos = 1;
	size_t end = 0;
	while ((end = teamData.find(",", pos)) != std::string::npos || (end = teamData.find("]", pos)) != std::string::npos) {
		std::string userData = teamData.substr(pos, end - pos);
		size_t separator = userData.find(":");
		if (separator != std::string::npos) {
			UserData user;
			try {
				user.user_index = userData.substr(0, separator);
				std::cout << "user_index : " << user.user_index << std::endl;
			}
			catch (const std::invalid_argument& e) {
				std::cerr << "stoi 호출 실패: " << userData.substr(0, separator) << std::endl;
				pos = end + 1;
				continue;
			}
			user.user_name = userData.substr(separator + 1);
			std::cout << "user_name : " << user.user_name << std::endl;
			team.push_back(user);
		}
		pos = end + 1;
	}
	return team;
}

RoomData UtilityManager::matchParsing(const std::string& request) {
	size_t jsonStart = request.find("(");
	size_t jsonEnd = request.find_last_of(")");

	if (jsonStart != std::string::npos && jsonEnd != std::string::npos) {
		std::string jsonData = request.substr(jsonStart + 1, jsonEnd - jsonStart - 1);

		try {

			size_t spaceIdStart = jsonData.find("spaceId=") + 8;
			size_t spaceIdEnd = jsonData.find(", ", spaceIdStart);
			std::string spaceId = jsonData.substr(spaceIdStart, spaceIdEnd - spaceIdStart);

			size_t teamsStart = jsonData.find("teams={") + 7;
			size_t teamsEnd = jsonData.find_last_of("}");
			std::string teamsData = jsonData.substr(teamsStart, teamsEnd - teamsStart);

			size_t redTeamStart = teamsData.find("red=[") + 5;
			size_t redTeamEnd = teamsData.find("],", redTeamStart);
			std::string redTeamData = teamsData.substr(redTeamStart - 1, redTeamEnd - redTeamStart + 2);

			size_t blueTeamStart = teamsData.find("blue=[", redTeamEnd) + 6;
			size_t blueTeamEnd = teamsData.find("]", blueTeamStart);
			std::string blueTeamData = teamsData.substr(blueTeamStart - 1, blueTeamEnd - blueTeamStart + 2);

			RoomData curRoom;
			curRoom.spaceId = spaceId;
			curRoom.redTeam = parseTeamData(redTeamData);
			std::cout << redTeamData << " parsing red team : " << (curRoom.redTeam.size()) << std::endl;
			curRoom.blueTeam = parseTeamData(blueTeamData);
			std::cout << blueTeamData << " parsing blue team : " << (curRoom.blueTeam.size()) << std::endl;

			return curRoom;
		}
		catch (const std::exception& e) {
			std::cerr << "Failed to parse data: " << e.what() << std::endl;
		}
	}
	else std::cerr << "Data not found in request." << std::endl;

	return RoomData(); // 파싱 실패 시 빈 객체 반환
}
