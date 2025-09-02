#include "mqtt/mqttSubscriber.hpp"
#include <mosquitto.h>
#include <algorithm>
#include <cstring>
#include <iostream>
#include <sstream>

namespace mqtt {

using namespace std::chrono;

mqttSubscriber::mqttSubscriber(mqttConfig cfg)
: cfg_(std::move(cfg)), backoff_current_(cfg_.backoffInit)
{
    mosquitto_lib_init();
    mosq_ = mosquitto_new(cfg_.clientId.c_str(), cfg_.cleanSession, this);
    if (!mosq_) throw std::runtime_error("mosquitto_new failed");

    if (cfg_.username && cfg_.password) {
        mosquitto_username_pw_set(mosq_, cfg_.username->c_str(), cfg_.password->c_str());
    }

    mosquitto_connect_callback_set(mosq_, &mqttSubscriber::handle_connect);
    mosquitto_disconnect_callback_set(mosq_, &mqttSubscriber::handle_disconnect);
    mosquitto_message_callback_set(mosq_, &mqttSubscriber::handle_message);
}

mqttSubscriber::~mqttSubscriber() {
    stop();
    if (mosq_) {
        mosquitto_destroy(mosq_);
        mosq_ = nullptr;
    }
    mosquitto_lib_cleanup();
}

void mqttSubscriber::set_topics(const std::vector<subSpec>& topics) {
    std::lock_guard<std::mutex> lk(topics_m_);
    topics_ = topics;
}

void mqttSubscriber::add_route(const std::string& filter, RouteHandler h) {
    std::lock_guard<std::mutex> lk(routes_m_);
    routes_.push_back({filter, std::move(h)});
}

void mqttSubscriber::start() {
    if (run_.exchange(true)) return;
    th_ = std::thread(&mqttSubscriber::run_loop, this);
}

void mqttSubscriber::stop() {
    if (!run_.exchange(false)) return;
    inbox_.stop();
    if (th_.joinable()) th_.join();
    do_disconnect();
}

bool mqttSubscriber::try_pop(inMsg& out, std::chrono::milliseconds wait) {
    return inbox_.pop_for(out, wait);
}

/* static */ void mqttSubscriber::handle_connect(struct mosquitto*, void* obj, int rc) {
    auto* self = static_cast<mqttSubscriber*>(obj);
    if (rc == 0) {
        self->state_.store(connState::CONNECTED);
        self->backoff_current_ = self->cfg_.backoffInit;

        // 재구독
        self->resubscribe_all();

        if (self->on_connect_) self->on_connect_();
    } else {
        if (self->on_error_) self->on_error_(std::string("connect failed rc=") + std::to_string(rc));
        self->state_.store(connState::DISCONNECTED);
    }
}

/* static */ void mqttSubscriber::handle_disconnect(struct mosquitto*, void* obj, int rc) {
    auto* self = static_cast<mqttSubscriber*>(obj);
    self->state_.store(connState::DISCONNECTED);
    if (self->on_disconnect_) self->on_disconnect_(rc);
}

/* static */ void mqttSubscriber::handle_message(struct mosquitto*, void* obj, const struct mosquitto_message* msg) {
    auto* self = static_cast<mqttSubscriber*>(obj);
    if (!msg || !msg->topic) return;

    inMsg im;
    im.topic = msg->topic;
    im.qos = msg->qos;
    im.retain = msg->retain != 0;
    if (msg->payloadlen > 0 && msg->payload) {
        const auto* p = static_cast<const uint8_t*>(msg->payload);
        im.payload.assign(p, p + msg->payloadlen);
    }
    self->inbox_.push(std::move(im));
}

bool mqttSubscriber::try_connect() {
    state_.store(connState::CONNECTING);
    last_conn_attempt_ = Clock::now();

    const int rc = mosquitto_connect(mosq_, cfg_.brokerAddr.c_str(), cfg_.port, cfg_.keepaliveSec);
    if (rc != MOSQ_ERR_SUCCESS) {
        state_.store(connState::DISCONNECTED);
        if (on_error_) on_error_(std::string("mosquitto_connect error: ") + mosquitto_strerror(rc));
        return false;
    }
    return true; // 최종 결과는 handle_connect에서
}

void mqttSubscriber::do_disconnect() {
    if (state_.load() != connState::DISCONNECTED) {
        mosquitto_disconnect(mosq_);
        state_.store(connState::DISCONNECTED);
    }
}

void mqttSubscriber::resubscribe_all() {
    std::lock_guard<std::mutex> lk(topics_m_);
    for (const auto& t : topics_) {
        int rc = mosquitto_subscribe(mosq_, nullptr, t.topic.c_str(), t.qos);
        if (rc != MOSQ_ERR_SUCCESS) {
            if (on_error_) on_error_(std::string("subscribe failed: ") + t.topic + " rc=" + mosquitto_strerror(rc));
        }
    }
}

bool mqttSubscriber::topic_match(const std::string& filter, const std::string& topic) {
    // MQTT 와일드카드 매칭: '+'(한 레벨), '#'(나머지 전체)
    auto split = [](const std::string& s) {
        std::vector<std::string> out;
        std::stringstream ss(s);
        std::string item;
        while (std::getline(ss, item, '/')) out.push_back(item);
        return out;
    };

    const auto f = split(filter);
    const auto t = split(topic);

    size_t i = 0, j = 0;
    for (; i < f.size(); ++i, ++j) {
        if (j >= t.size()) {
            // filter가 남았는데 topic이 끝남
            // 단, filter가 마지막이 '#'이면 ok
            return (f[i] == "#" && i == f.size() - 1);
        }

        if (f[i] == "#") {
            // 마지막이어야 하며, 나머지 전부 매치
            return (i == f.size() - 1);
        }

        if (f[i] == "+") {
            // 정확히 한 레벨 소비
            continue;
        }

        if (f[i] != t[j]) return false;
    }
    // filter 다 소비. topic도 다 소비되어야 완전 일치
    return (j == t.size());
}

void mqttSubscriber::run_loop() {
    const auto short_sleep = milliseconds(10);
    const auto conn_check  = milliseconds(50);

    last_ping_ = Clock::now();
    last_conn_attempt_ = Clock::now() - cfg_.backoffInit - milliseconds(1);

    while (run_.load()) {
        const auto now = Clock::now();

        // 1) 연결 상태 관리(백오프)
        if (state_.load() == connState::DISCONNECTED) {
            if (now - last_conn_attempt_ >= backoff_current_) {
                if (!try_connect()) {
                    backoff_current_ = std::min(backoff_current_ * 2, cfg_.backoffMax);
                }
            }
            std::this_thread::sleep_for(conn_check);
            continue;
        }

        // 2) keepalive 보조 체크 (libmosquitto가 ping 관리는 자동)
        const auto ping_interval = milliseconds(static_cast<int>(cfg_.keepaliveSec * 1000 * cfg_.pingFactor));
        if (now - last_ping_ >= ping_interval) {
            last_ping_ = now;
            // 별도의 pingreq 호출 불필요: mosquitto_loop가 처리
        }

        // 3) 네트워크 이벤트 처리 (non-blocking)
        const int rc_loop = mosquitto_loop(mosq_, 0, 1);
        if (rc_loop != MOSQ_ERR_SUCCESS) {
            if (on_error_) on_error_(std::string("loop error: ") + mosquitto_strerror(rc_loop));
            do_disconnect();
            std::this_thread::sleep_for(short_sleep);
            continue;
        }

        // 4) 메시지 디스패치(업무 로직)
        //  - inbox_에서 가능한 한 많이 소진(짧은 타임아웃으로 반복)
        for (int k = 0; k < 64; ++k) {
            inMsg msg;
            if (!inbox_.pop_for(msg, milliseconds(1))) break;

            // 라우팅
            std::vector<std::pair<std::string, RouteHandler>> routes_copy;
            {
                std::lock_guard<std::mutex> lk(routes_m_);
                routes_copy = routes_;
            }

            bool handled = false;
            for (auto& [flt, fn] : routes_copy) {
                if (topic_match(flt, msg.topic)) {
                    fn(msg);
                    handled = true;
                }
            }

            if (!handled && on_error_) {
                on_error_(std::string("no route matched for topic: ") + msg.topic);
            }
        }

        // 5) 속도 조절
        std::this_thread::sleep_for(short_sleep);
    }
}

} // namespace mqttpub
