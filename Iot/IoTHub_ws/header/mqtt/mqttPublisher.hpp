#pragma once
#include "util/mqttUtil.hpp"
#include "util/tsQueue.hpp"
#include <atomic>
#include <chrono>
#include <functional>
#include <map>
#include <mutex>
#include <thread>

struct mosquitto; // 전방선언

namespace mqtt {

class mqttPublisher {
public:
    using Clock = std::chrono::steady_clock;

    explicit mqttPublisher(mqttConfig cfg);
    ~mqttPublisher();

    void start();                   // 내부 워커 시작
    void stop();                    // 정지/해제
    void publish(pubMsg msg);       // 송신큐 적재
    connState state() const { return state_.load(); }

    // (선택) 이벤트 콜백
    void set_on_connect(std::function<void()> cb) { on_connect_ = std::move(cb); }
    void set_on_disconnect(std::function<void(int)> cb) { on_disconnect_ = std::move(cb); }
    void set_on_puback(std::function<void(int)> cb) { on_puback_ = std::move(cb); }
    void set_on_error(std::function<void(const std::string&)> cb) { on_error_ = std::move(cb); }

private:
    // mosquitto 콜백 → 정적 함수
    static void handle_connect(struct mosquitto*, void* obj, int rc);
    static void handle_disconnect(struct mosquitto*, void* obj, int rc);
    static void handle_publish(struct mosquitto*, void* obj, int mid);

    // 내부 동작
    void run_loop();
    bool try_connect();
    void do_disconnect();

    // QoS 보류/완료/재전송
    struct Pending {
        pubMsg msg;
        int mid = 0;
        Clock::time_point t0;
        int attempts = 0;
    };
    void track_pending(int mid, const pubMsg& msg);
    void complete_pending(int mid);
    void retry_timeouts();

private:
    mqttConfig cfg_;
    std::atomic<connState> state_{connState::DISCONNECTED};

    mosquitto* mosq_{nullptr};

    tsQueue<pubMsg> outbox_;

    std::thread th_;
    std::atomic<bool> run_{false};

    // 타이밍
    Clock::time_point last_ping_{};
    Clock::time_point last_conn_attempt_{};
    std::chrono::milliseconds backoff_current_{};

    // 대기중 mid -> Pending
    std::mutex pend_m_;
    std::map<int, Pending> pending_;

    // 콜백
    std::function<void()> on_connect_;
    std::function<void(int)> on_disconnect_;
    std::function<void(int)> on_puback_;
    std::function<void(const std::string&)> on_error_;
};

} // namespace mqttpub
