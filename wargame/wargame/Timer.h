#include "base.h"
#include <functional>
#include <mutex>

class Timer {
public:
    Timer() : expired_(true), try_to_expire_(false) {
    }

    Timer(const Timer& t) {
        expired_ = t.expired_.load();
        try_to_expire_ = t.try_to_expire_.load();
    }

    ~Timer() {
        Expire();
        // 스레드가 종료될 때까지 기다립니다.
        if (th_.joinable()) {
            th_.join();
        }
    }

    void StartTimer(int interval, std::function<void()> task) {
        // 만약 타이머가 이미 실행 중이라면 기존 스레드를 종료하고 새로 시작합니다.
        if (expired_ == false) {
            Expire(); // 기존 타이머를 만료시키고
            if (th_.joinable()) {
                th_.join(); // 기존 스레드가 종료될 때까지 기다립니다.
            }
        }
        expired_ = false;
        try_to_expire_ = false;
        // th_ 멤버 변수를 사용하여 새 스레드를 생성하고 할당합니다.
        th_ = std::thread([this, interval, task]() {
            while (!try_to_expire_) {
                std::this_thread::sleep_for(std::chrono::milliseconds(interval));
                task();
            }
            // 루프를 빠져나오면 타이머가 만료됩니다.
            {
                std::lock_guard<std::mutex> locker(this->mutex_);
                this->expired_ = true;
                this->try_to_expire_ = false;
            }
            });
    }


    void Expire() {
        if (expired_) {
            // 타이머가 이미 만료되었거나 실행되지 않았다면 무시합니다.
            return;
        }

        if (try_to_expire_) {
            // 이미 만료되려고 시도 중이라면 무시합니다.
            return;
        }
        try_to_expire_ = true;
        {
            std::lock_guard<std::mutex> locker(mutex_);
            expired_ = true;
        }
    }

private:
    std::atomic<bool> expired_;
    std::atomic<bool> try_to_expire_;
    std::mutex mutex_;
    std::thread th_;
};