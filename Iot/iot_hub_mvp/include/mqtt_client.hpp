#pragma once
#include <mosquitto.h>
#include <functional>
#include <string>

class MqttClient {
public:
    using MessageHandler = std::function<void(const std::string& topic, const std::string& payload)>;

    bool init(const std::string& clientId, const std::string& host, int port,
              const std::string& user="", const std::string& pass="");
    void set_message_handler(MessageHandler h);
    bool subscribe(const std::string& topic, int qos=0);
    bool publish(const std::string& topic, const std::string& payload, int qos=0, bool retain=false);
    void loop_forever(); // blocking
    void cleanup();

private:
    static void on_connect_cb(struct mosquitto*, void*, int rc);
    static void on_message_cb(struct mosquitto*, void*, const struct mosquitto_message*);

    struct mosquitto* m_{nullptr};
    MessageHandler handler_;
};
