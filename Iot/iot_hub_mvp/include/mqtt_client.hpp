#pragma once
#include "config.hpp"
#include <mosquitto.h>
#include <functional>
#include <string>

class MqttClient {
public:
    using MessageHandler = std::function<void(const std::string&, const std::string&)>;

    // ✅ config만 넘기면 끝
    bool init(const AppConfig& cfg, const std::string& clientId = "mqtt_client");

    void set_message_handler(MessageHandler h);
    bool subscribe(const std::string& topic, int qos=0);
    bool publish(const std::string& topic, const std::string& payload, int qos=0, bool retain=false);

    void loop_forever();
    void loop_for_ms(int ms);   // 테스트 편의용
    void cleanup();

private:
    static void on_connect_cb(struct mosquitto*, void*, int rc);
    static void on_message_cb(struct mosquitto*, void*, const struct mosquitto_message*);

    AppConfig cfg_;
    struct mosquitto* m_{nullptr};
    MessageHandler handler_;
};
