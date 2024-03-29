#pragma once
#include <iostream>
#include <list>
#include <mutex>

template <typename T>
class syncList {
public:
    void push_back(const T& item) {
        std::lock_guard<std::mutex> lock(mutex_); //lock_guard는 scope 벗어나면 mutex 해제
        list_.push_back(item);
    }

    void remove(const T& item) {
        std::lock_guard<std::mutex> lock(mutex_);
        list_.remove(item);
    }

    bool contains(const T& item) {
        std::lock_guard<std::mutex> lock(mutex_);
        return std::find(list_.begin(), list_.end(), item) != list_.end();
    }

    int size() {
        return list_.size();
    }

private:
    std::list<T> list_;
    std::mutex mutex_;
};