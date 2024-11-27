#include "Timer.h"


std::map<int, std::pair<std::chrono::time_point<std::chrono::steady_clock>, std::function<void()>>> Timer::timers;
std::mutex Timer::timer_mutex;
int Timer::timeout_check_time = 0;


void Timer::TimeOutCheck()
{
	if (time(NULL) > timeout_check_time)
	{
		std::list<Client*> des_cli;
		for (auto inst : GameManager::client_list_all)
		{
			if (inst->socket != -1 && time(NULL) > inst->out_time)
				des_cli.push_back(inst);
		}

		for (auto inst : des_cli)
		{
			if (inst->socket == TEST_CLIENT_SOCKET) // test
				continue;
			printf("Time out %d \n", inst->socket);
			GameManager::ClientClose(inst->socket);
		}

		timeout_check_time = time(NULL) + 10;
	}
}

void Timer::AddTimer(int id, CallbackFunction callback, int delay) {
	auto expire_time = std::chrono::steady_clock::now() + std::chrono::milliseconds(delay);
	std::lock_guard<std::mutex> lock(timer_mutex);
	timers[id] = { expire_time, callback };
}

void Timer::RemoveTimer(int id) {
	std::lock_guard<std::mutex> lock(timer_mutex);
	timers.erase(id);
}

void Timer::ProcessTimers() {
	auto now = std::chrono::steady_clock::now();
	std::list<int> expired_timers;

	{
		std::lock_guard<std::mutex> lock(timer_mutex);
		for (auto& [id, timer] : timers) {
			if (now >= timer.first) {
				expired_timers.push_back(id);
			}
		}
	}

	for (int id : expired_timers) {
		std::lock_guard<std::mutex> lock(timer_mutex);
		auto callback = timers[id].second;
		timers.erase(id);
		callback();
	}
}