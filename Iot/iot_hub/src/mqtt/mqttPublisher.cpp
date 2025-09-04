#include "mqtt/mqttPublisher.hpp"
#include <mosquitto.h>
#include <algorithm>
#include <cstring>
#include <iostream>

namespace mqtt {

using namespace std::chrono;

mqttPublisher::mqttPublisher(mqttConfig cfg)
: cfg_(std::move(cfg)), backoff_current_(cfg_.backoffInit)
{
    mosquitto_lib_init();
    mosq_ = mosquitto_new(cfg_.clientId.c_str(), cfg_.cleanSession, this);
    if (!mosq_) throw std::runtime_error("mosquitto_new failed");

    // 인증
    if (cfg_.username && cfg_.password) {
        mosquitto_username_pw_set(mosq_, cfg_.username->c_str(), cfg_.password->c_str());
    }

    // LWT
    if (cfg_.lwtTopic && cfg_.lwtPayload) {
        mosquitto_will_set(
            mosq_,
            cfg_.lwtTopic->c_str(),
            static_cast<int>(cfg_.lwtPayload->size()),
            cfg_.lwtPayload->data(),
            cfg_.lwtQos,
            cfg_.lwtRetain
        );
    }

    // 콜백 등록
    mosquitto_connect_callback_set(mosq_, &mqttPublisher::handle_connect);
    mosquitto_disconnect_callback_set(mosq_, &mqttPublisher::handle_disconnect);
    mosquitto_publish_callback_set(mosq_, &mqttPublisher::handle_publish);
}

mqttPublisher::~mqttPublisher() {
    stop();
    if (mosq_) {
        mosquitto_destroy(mosq_);
        mosq_ = nullptr;
    }
    mosquitto_lib_cleanup();
}

void mqttPublisher::start() {
    if (run_.exchange(true)) return;
    th_ = std::thread(&mqttPublisher::run_loop, this);
}

void mqttPublisher::stop() {
    if (!run_.exchange(false)) return;
    outbox_.stop();
    if (th_.joinable()) th_.join();
    do_disconnect();
}

void mqttPublisher::publish(pubMsg msg) {
    outbox_.push(std::move(msg));
}

/* static */ void mqttPublisher::handle_connect(struct mosquitto*, void* obj, int rc) {
    auto* self = static_cast<mqttPublisher*>(obj);
    if (rc == 0) {
        self->state_.store(connState::CONNECTED);
        self->backoff_current_ = self->cfg_.backoffInit;
        if (self->on_connect_) self->on_connect_();
    } else {
        if (self->on_error_) self->on_error_(std::string("connect failed rc=") + std::to_string(rc));
        self->state_.store(connState::DISCONNECTED);
    }
}

/* static */ void mqttPublisher::handle_disconnect(struct mosquitto*, void* obj, int rc) {
    auto* self = static_cast<mqttPublisher*>(obj);
    self->state_.store(connState::DISCONNECTED);
    if (self->on_disconnect_) self->on_disconnect_(rc);
}

/* static */ void mqttPublisher::handle_publish(struct mosquitto*, void* obj, int mid) {
    auto* self = static_cast<mqttPublisher*>(obj);
    self->complete_pending(mid);
    if (self->on_puback_) self->on_puback_(mid);
}

bool mqttPublisher::try_connect() {
    state_.store(connState::CONNECTING);
    last_conn_attempt_ = Clock::now();

    const int rc = mosquitto_connect(mosq_, cfg_.brokerAddr.c_str(), cfg_.port, cfg_.keepaliveSec);
    if (rc != MOSQ_ERR_SUCCESS) {
        state_.store(connState::DISCONNECTED);
        if (on_error_) on_error_(std::string("mosquitto_connect error: ") + mosquitto_strerror(rc));
        return false;
    }
    return true; // 최종 성공/실패는 handle_connect에서 결정
}

void mqttPublisher::do_disconnect() {
    if (state_.load() != connState::DISCONNECTED) {
        mosquitto_disconnect(mosq_);
        state_.store(connState::DISCONNECTED);
    }
}

void mqttPublisher::track_pending(int mid, const pubMsg& msg) {
    std::lock_guard<std::mutex> lk(pend_m_);
    pending_[mid] = Pending{msg, mid, Clock::now(), 1};
}

void mqttPublisher::complete_pending(int mid) {
    std::lock_guard<std::mutex> lk(pend_m_);
    pending_.erase(mid);
}

void mqttPublisher::retry_timeouts() {
    std::vector<Pending> to_retry;
    {
        std::lock_guard<std::mutex> lk(pend_m_);
        const auto now = Clock::now();
        for (auto it = pending_.begin(); it != pending_.end();) {
            auto& p = it->second;
            if (duration_cast<milliseconds>(now - p.t0) > cfg_.backoffInit) {
                to_retry.push_back(p);
                it = pending_.erase(it);
            } else {
                ++it;
            }
        }
    }

    for (auto& p : to_retry) {
        if (p.attempts >= cfg_.maxRetries) {
            if (on_error_) on_error_("publish timeout mid=" + std::to_string(p.mid));
            continue;
        }
        if (state_.load() == connState::CONNECTED) {
            int mid = 0;
            int qos = (p.msg.qos < 0 || p.msg.qos > 2) ? 1 : p.msg.qos;
            const int rc = mosquitto_publish(
                mosq_, &mid, p.msg.topic.c_str(),
                static_cast<int>(p.msg.payload.size()),
                p.msg.payload.data(),
                qos, p.msg.retain
            );
            if (rc == MOSQ_ERR_SUCCESS) {
                p.attempts++;
                p.t0 = Clock::now();
                p.mid = mid;
                track_pending(mid, p.msg);
            } else {
                if (on_error_) on_error_(std::string("re-publish failed: ") + mosquitto_strerror(rc));
                do_disconnect();
            }
        }
    }
}

void mqttPublisher::run_loop() {
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

        // 2) keepalive 보조 헬스체크
        const auto ping_interval = milliseconds(static_cast<int>(cfg_.keepaliveSec * 1000 * cfg_.pingFactor));
        if (now - last_ping_ >= ping_interval) {
            last_ping_ = now;
            // libmosquitto가 ping을 관리하므로 loop만 충실히 돌립니다.
        }

        // 3) 송신큐 → publish
        pubMsg msg;
        if (outbox_.pop_for(msg, milliseconds(5))) {
            int mid = 0;
            int qos = (msg.qos < 0 || msg.qos > 2) ? 1 : msg.qos;
            const int rc = mosquitto_publish(
                mosq_, &mid, msg.topic.c_str(),
                static_cast<int>(msg.payload.size()),
                msg.payload.data(),
                qos, msg.retain
            );
            if (rc == MOSQ_ERR_SUCCESS) {
                if (qos > 0) track_pending(mid, msg);
            } else {
                if (on_error_) on_error_(std::string("publish failed: ") + mosquitto_strerror(rc));
                do_disconnect();
            }
        }

        // 3-1) QoS 타임아웃/재전송
        retry_timeouts();

        // 4) 네트워크 이벤트 처리
        const int rc_loop = mosquitto_loop(mosq_, 0, 1);
        if (rc_loop != MOSQ_ERR_SUCCESS) {
            if (on_error_) on_error_(std::string("loop error: ") + mosquitto_strerror(rc_loop));
            do_disconnect();
        }

        // 5) 속도 조절
        std::this_thread::sleep_for(short_sleep);
    }
}

} // namespace mqttpub
