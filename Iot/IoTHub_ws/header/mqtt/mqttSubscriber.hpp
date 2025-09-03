#pragma once
#include "util/mqttUtil.hpp"
#include "util/tsQueue.hpp"
#include <atomic>
#include <chrono>
#include <functional>
#include <map>
#include <mutex>
#include <thread>
#include <vector>

struct mosquitto; // forward
struct mosquitto_message;

namespace mqtt {

class mqttSubscriber {
public:
    using Clock = std::chrono::steady_clock;

    explicit mqttSubscriber(mqttConfig cfg);
    ~mqttSubscriber();

    // 수명주기
    void start();
    void stop();

    // 구독 목록 설정 (연결 시/재연결 시 자동 구독)
    void set_topics(const std::vector<subSpec>& topics);

    // 수신 메시지 큐를 외부에서 소비
    bool try_pop(inMsg& out, std::chrono::milliseconds wait = std::chrono::milliseconds(5));

    connState state() const { return state_.load(); }

    // (선택) 이벤트 콜백
    void set_on_connect(std::function<void()> cb) { on_connect_ = std::move(cb); }
    void set_on_disconnect(std::function<void(int)> cb) { on_disconnect_ = std::move(cb); }
    void set_on_error(std::function<void(const std::string&)> cb) { on_error_ = std::move(cb); }

    // 라우팅: MQTT 와일드카드(+/#) 패턴과 핸들러 등록
    using RouteHandler = std::function<void(const inMsg&)>;
    void add_route(const std::string& filter, RouteHandler h);

private:
    // mosquitto 콜백
    static void handle_connect(struct mosquitto*, void* obj, int rc);
    static void handle_disconnect(struct mosquitto*, void* obj, int rc);
    static void handle_message(struct mosquitto*, void* obj, const struct mosquitto_message* msg);

    // 내부 동작
    void run_loop();
    bool try_connect();
    void do_disconnect();
    void resubscribe_all();

    // 와일드카드 매칭
    static bool topic_match(const std::string& filter, const std::string& topic);

private:
    mqttConfig cfg_;
    std::atomic<connState> state_{connState::DISCONNECTED};

    mosquitto* mosq_{nullptr};

    // 구독 목록
    std::mutex topics_m_;
    std::vector<subSpec> topics_;

    // 수신 큐(비즈니스 로직 처리용)
    tsQueue<inMsg> inbox_;

    // 루프/재연결
    std::thread th_;
    std::atomic<bool> run_{false};
    Clock::time_point last_ping_{};
    Clock::time_point last_conn_attempt_{};
    std::chrono::milliseconds backoff_current_{};

    // 라우팅
    std::mutex routes_m_;
    std::vector<std::pair<std::string, RouteHandler>> routes_;

    // 콜백
    std::function<void()> on_connect_;
    std::function<void(int)> on_disconnect_;
    std::function<void(const std::string&)> on_error_;
};

} // namespace mqtt
