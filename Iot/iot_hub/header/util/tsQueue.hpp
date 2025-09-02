#pragma
#include <mutex>
#include <queue>
#include <condition_variable>
#include "mqttUtil.hpp"

namespace mqtt{
    class tsQueue{
    public:
        void push(PubMsg&& m){
            {
            std::lock_guard<std::mutex> lk(m_);
            q_.push(std::move(m));
            }
            cv_.notify_one();
        }

        bool popFor(PubMsg& out, std::chrono::milliseconds wait){
            std::unique_lock<std::mutex> lk(m_);
            if(!cv_.wait_for(lk, wait, [&]{return !q_.empty()|| stop_;})) return false;
            if(stop_) return false;

            out = std::move(q_.front());
            q_.pop();
             return true;
        }

        void stop(){
            {
            std::lock_guard<std::mutex> lk(m_);
            stop_ = true;
            }
            cv_.notify_all();
        }

        bool empty() const{
            std::lock_guard<std::mutex> lk(m_);
            return q_.empty();
        }
    private:
        mutable std::mutex m_;
        std::condition_variable cv_;
        std::queue<PubMsg> q_;
        bool stop_{false};
    };
}