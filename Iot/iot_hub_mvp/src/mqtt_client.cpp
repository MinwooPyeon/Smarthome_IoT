#include "MqttClient.hpp"
#include <iostream>
#include <cstring>
#include <atomic>

static std::atomic<bool> g_lib_inited{false};

bool MqttClient::init(const std::string& clientId, const std::string& host, int port,
                      const std::string& user, const std::string& pass)
{
    if (!g_lib_inited.exchange(true)) {
        mosquitto_lib_init();
    }

    // clean session = true, userdata = this
    m_ = mosquitto_new(clientId.c_str(), true, this);
    if (!m_) {
        std::cerr << "[MQTT] mosquitto_new() failed\n";
        return false;
    }

    // 멀티스레드에서 publish할 수 있도록
    mosquitto_threaded_set(m_, true);

    if (!user.empty()) {
        int rc = mosquitto_username_pw_set(m_, user.c_str(), pass.empty() ? nullptr : pass.c_str());
        if (rc != MOSQ_ERR_SUCCESS) {
            std::cerr << "[MQTT] username_pw_set failed: " << mosquitto_strerror(rc) << "\n";
            return false;
        }
    }

    mosquitto_connect_callback_set(m_, &MqttClient::on_connect_cb);
    mosquitto_message_callback_set(m_, &MqttClient::on_message_cb);

    int rc = mosquitto_connect(m_, host.c_str(), port, /*keepalive*/60);
    if (rc != MOSQ_ERR_SUCCESS) {
        std::cerr << "[MQTT] connect failed: " << mosquitto_strerror(rc) << "\n";
        return false;
    }
    return true;
}

void MqttClient::set_message_handler(MessageHandler h) { handler_ = std::move(h); }

bool MqttClient::subscribe(const std::string& topic, int qos)
{
    if (!m_) return false;
    int mid = 0;
    int rc = mosquitto_subscribe(m_, &mid, topic.c_str(), qos);
    if (rc != MOSQ_ERR_SUCCESS) {
        std::cerr << "[MQTT] subscribe failed: " << mosquitto_strerror(rc) << "\n";
        return false;
    }
    return true;
}

bool MqttClient::publish(const std::string& topic, const std::string& payload, int qos, bool retain)
{
    if (!m_) return false;
    int rc = mosquitto_publish(m_, /*mid*/nullptr, topic.c_str(),
                               static_cast<int>(payload.size()), payload.data(),
                               qos, retain);
    if (rc != MOSQ_ERR_SUCCESS) {
        std::cerr << "[MQTT] publish failed: " << mosquitto_strerror(rc) << "\n";
        return false;
    }
    return true;
}

void MqttClient::loop_forever()
{
    if (!m_) return;
    // 네트워크 루프(블로킹). 신호/외부에서 mosquitto_disconnect 호출 시 반환됨.
    int rc = mosquitto_loop_forever(m_, /*timeout_ms*/-1, /*max_packets*/1);
    if (rc != MOSQ_ERR_SUCCESS && rc != MOSQ_ERR_NO_CONN) {
        std::cerr << "[MQTT] loop_forever returned: " << mosquitto_strerror(rc) << "\n";
    }
}

void MqttClient::cleanup()
{
    if (m_) {
        // 연결 중이면 끊기
        mosquitto_disconnect(m_);
        mosquitto_destroy(m_);
        m_ = nullptr;
    }
    // 단일 프로세스에서 한 번만 정리
    if (g_lib_inited.exchange(false)) {
        mosquitto_lib_cleanup();
    }
}

// ---- static callbacks ----
void MqttClient::on_connect_cb(struct mosquitto* m, void* userdata, int rc)
{
    auto* self = static_cast<MqttClient*>(userdata);
    if (rc == 0) {
        std::cerr << "[MQTT] connected.\n";
    } else {
        std::cerr << "[MQTT] connect error: " << mosquitto_strerror(rc) << "\n";
    }
    (void)m; (void)self;
}

void MqttClient::on_message_cb(struct mosquitto* m, void* userdata, const struct mosquitto_message* msg)
{
    auto* self = static_cast<MqttClient*>(userdata);
    if (!self || !self->handler_) return;

    // payload는 NUL-terminated 보장X → 길이 기반으로 문자열 구성
    std::string topic = msg->topic ? msg->topic : "";
    std::string payload;
    if (msg->payload && msg->payloadlen > 0) {
        payload.assign(static_cast<const char*>(msg->payload),
                       static_cast<size_t>(msg->payloadlen));
    }

    self->handler_(topic, payload);
    (void)m;
}
