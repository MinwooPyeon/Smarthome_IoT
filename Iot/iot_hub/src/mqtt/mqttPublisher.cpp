#include "mqtt/MqttPublisher.hpp"
#include <mosquitto.h>
#include <algorithm>
#include <cstring>
#include <iostream>

namespace mqtt {

using namespace std::chrono;

MqttPublisher::MqttPublisher(MqttConfig cfg)
: cfg_(std::move(cfg)), backoff_current_(cfg_.backoff_init)
{
    mosquitto_lib_init();
    mosq_ = mosquitto_new(cfg_.client_id.c_str(), cfg_.clean_session, this);
    if (!mosq_) throw std::runtime_error("mosquitto_new failed");

    // 인증
    if (cfg_.username && cfg_.password) {
        mosquitto_username_pw_set(mosq_, cfg_.username->c_str(), cfg_.password->c_str());
    }

    // LWT
    if (cfg_.lwt_topic && cfg_.lwt_payload) {
        mosquitto_will_set(
            mosq_,
            cfg_.lwt_topic->c_str(),
            static_cast<int>(cfg_.lwt_payload->size()),
            cfg_.lwt_payload->data(),
            cfg_.lwt_qos,
            cfg_.lwt_retain
        );
    }

    // 콜백 등록
    mosquitto_connect_callback_set(mosq_, &MqttPublisher::handle_connect);
    mosquitto_disconnect_callback_set(mosq_, &MqttPublisher::handle_disconnect);
    mosquitto_publish_callback_set(mosq_, &MqttPublisher::handle_publish);
}

MqttPublisher::~MqttPublisher() {
    stop();
    if (mosq_) {
        mosquitto_destroy(mosq_);
        mosq_ = nullptr;
    }
    mosquitto_lib_cleanup();
}

void MqttPublisher::start() {
    if (run_.exchange(true)) return;
    th_ = std::thread(&MqttPublisher::run_loop, this);
}

void MqttPublisher::stop() {
    if (!run_.exchange(false)) return;
    outbox_.stop();
    if (th_.joinable()) th_.join();
    do_disconnect();
}

void MqttPublisher::publish(PubMsg msg) {
    outbox_.push(std::move(msg));
}

/* static */ void MqttPublisher::handle_connect(struct mosquitto*, void* obj, int rc) {
    auto* self = static_cast<MqttPublisher*>(obj);
    if (rc == 0) {
        self->state_.store(ConnState::CONNECTED);
        self->backoff_current_ = self->cfg_.backoff_init;
        if (self->on_connect_) self->on_connect_();
    } else {
        if (self->on_error_) self->on_error_(std::string("connect failed rc=") + std::to_string(rc));
        self->state_.store(ConnState::DISCONNECTED);
    }
}

/* static */ void MqttPublisher::handle_disconnect(struct mosquitto*, void* obj, int rc) {
    auto* self = static_cast<MqttPublisher*>(obj);
    self->state_.store(ConnState::DISCONNECTED);
    if (self->on_disconnect_) self->on_disconnect_(rc);
}

/* static */ void MqttPublisher::handle_publish(struct mosquitto*, void* obj, int mid) {
    auto* self = static_cast<MqttPublisher*>(obj);
    self->complete_pending(mid);
    if (self->on_puback_) self->on_puback_(mid);
}

bool MqttPublisher::try_connect() {
    state_.store(ConnState::CONNECTING);
    last_conn_attempt_ = Clock::now();

    const int rc = mosquitto_connect(mosq_, cfg_.broker_addr.c_str(), cfg_.port, cfg_.keepalive_sec);
    if (rc != MOSQ_ERR_SUCCESS) {
        state_.store(ConnState::DISCONNECTED);
        if (on_error_) on_error_(std::string("mosquitto_connect error: ") + mosquitto_strerror(rc));
        return false;
    }
    return true; // 최종 성공/실패는 handle_connect에서 결정
}

void MqttPublisher::do_disconnect() {
    if (state_.load() != ConnState::DISCONNECTED) {
        mosquitto_disconnect(mosq_);
        state_.store(ConnState::DISCONNECTED);
    }
}

void MqttPublisher::track_pending(int mid, const PubMsg& msg) {
    std::lock_guard<std::mutex> lk(pend_m_);
    pending_[mid] = Pending{msg, mid, Clock::now(), 1};
}

void MqttPublisher::complete_pending(int mid) {
    std::lock_guard<std::mutex> lk(pend_m_);
    pending_.erase(mid);
}

void MqttPublisher::retry_timeouts() {
    std::vector<Pending> to_retry;
    {
        std::lock_guard<std::mutex> lk(pend_m_);
        const auto now = Clock::now();
        for (auto it = pending_.begin(); it != pending_.end();) {
            auto& p = it->second;
            if (duration_cast<milliseconds>(now - p.t0) > cfg_.ack_timeout) {
                to_retry.push_back(p);
                it = pending_.erase(it);
            } else {
                ++it;
            }
        }
    }

    for (auto& p : to_retry) {
        if (p.attempts >= cfg_.max_retries) {
            if (on_error_) on_error_("publish timeout mid=" + std::to_string(p.mid));
            continue;
        }
        if (state_.load() == ConnState::CONNECTED) {
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

void MqttPublisher::run_loop() {
    const auto short_sleep = milliseconds(10);
    const auto conn_check  = milliseconds(50);

    last_ping_ = Clock::now();
    last_conn_attempt_ = Clock::now() - cfg_.backoff_init - milliseconds(1);

    while (run_.load()) {
        const auto now = Clock::now();

        // 1) 연결 상태 관리(백오프)
        if (state_.load() == ConnState::DISCONNECTED) {
            if (now - last_conn_attempt_ >= backoff_current_) {
                if (!try_connect()) {
                    backoff_current_ = std::min(backoff_current_ * 2, cfg_.backoff_max);
                }
            }
            std::this_thread::sleep_for(conn_check);
            continue;
        }

        // 2) keepalive 보조 헬스체크
        const auto ping_interval = milliseconds(static_cast<int>(cfg_.keepalive_sec * 1000 * cfg_.ping_factor));
        if (now - last_ping_ >= ping_interval) {
            last_ping_ = now;
            // libmosquitto가 ping을 관리하므로 loop만 충실히 돌립니다.
        }

        // 3) 송신큐 → publish
        PubMsg msg;
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
