#pragma
#include <mutex>
#include <queue>
#include <condition_variable>
#include <chrono>
#include "mqttUtil.hpp"

namespace mqtt{
    class TSQueue {
public:
    void push(T&& v) {
        {
            std::lock_guard<std::mutex> lk(m_);
            q_.push(std::move(v));
        }
        cv_.notify_one();
    }

    // timeout 대기 pop (성공 시 true)
    bool pop_for(T& out, std::chrono::milliseconds wait) {
        std::unique_lock<std::mutex> lk(m_);
        if (!cv_.wait_for(lk, wait, [&]{ return !q_.empty() || stop_; })) return false;
        if (stop_) return false;
        out = std::move(q_.front());
        q_.pop();
        return true;
    }

    void stop() {
        {
            std::lock_guard<std::mutex> lk(m_);
            stop_ = true;
        }
        cv_.notify_all();
    }

    bool empty() const {
        std::lock_guard<std::mutex> lk(m_);
        return q_.empty();
    }

private:
    mutable std::mutex m_;
    std::condition_variable cv_;
    std::queue<T> q_;
    bool stop_{false};
};
}