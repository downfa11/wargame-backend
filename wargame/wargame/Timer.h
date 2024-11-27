#include "GameManager.h"

#include <map>
#include <functional>
#include <chrono>
#include <thread>
#include <mutex>

#define TEST_CLIENT_SOCKET 1000

using CallbackFunction = std::function<void()>;

class Timer {
public:
	static void TimeOutCheck();
	static void AddTimer(int id, CallbackFunction callback, int delay);
	static void RemoveTimer(int id);
	static void ProcessTimers();
private:
	static std::map<int, std::pair<std::chrono::time_point<std::chrono::steady_clock>, std::function<void()>>> timers;
	static std::mutex timer_mutex;
	static int timeout_check_time;
};