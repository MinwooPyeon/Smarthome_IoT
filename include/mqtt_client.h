#pragma once

#include <string>
#include <functional>
#include <memory>
#include <mosquitto.h>
#include "proto/irremote.pb.h"

namespace irremote {

class MqttClient {
public:
    using MessageCallback = std::function<void(const std::string& topic, const irremote::Action& action)>;
    
    MqttClient();
    ~MqttClient();

    // Disable copy
    MqttClient(const MqttClient&) = delete;
    MqttClient& operator=(const MqttClient&) = delete;

    // Enable move
    MqttClient(MqttClient&&) = default;
    MqttClient& operator=(MqttClient&&) = default;

    // Connection methods
    bool connect(const std::string& broker, int port, const std::string& username, 
                 const std::string& password, const std::string& clientId);
    void disconnect();
    bool isConnected() const;

    // Subscription methods
    bool subscribe(const std::string& topic, int qos = 1);
    bool unsubscribe(const std::string& topic);

    // Publishing methods
    bool publish(const std::string& topic, const std::string& message, int qos = 1);

    // Callback registration
    void setMessageCallback(MessageCallback callback) { messageCallback_ = callback; }
    void setConnectCallback(std::function<void()> callback) { connectCallback_ = callback; }
    void setDisconnectCallback(std::function<void()> callback) { disconnectCallback_ = callback; }

    // Loop methods
    void loop(int timeout = -1);
    void loopStart();
    void loopStop();

private:
    static void onConnect(struct mosquitto* mosq, void* userdata, int result);
    static void onDisconnect(struct mosquitto* mosq, void* userdata, int result);
    static void onMessage(struct mosquitto* mosq, void* userdata, 
                         const struct mosquitto_message* message);
    static void onSubscribe(struct mosquitto* mosq, void* userdata, int mid, 
                           int qos_count, const int* granted_qos);
    static void onPublish(struct mosquitto* mosq, void* userdata, int mid);

    struct mosquitto* mosq_;
    bool connected_;
    MessageCallback messageCallback_;
    std::function<void()> connectCallback_;
    std::function<void()> disconnectCallback_;
};

} // namespace irremote
