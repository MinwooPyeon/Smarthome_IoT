#pragma once

#include <string>
#include <functional>

#ifdef _WIN32
#elif defined(ESP32) || defined(ESP_PLATFORM)
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include "stdint.h"
#else
#include <mosquitto.h>
#endif

class MqttClient {
public:
    MqttClient();
    ~MqttClient();

    bool connect(const std::string& broker, int port = 1883);
    bool connectSecure(const std::string& broker, int port = 8883,
                      const std::string& ca_cert = "",
                      const std::string& client_cert = "",
                      const std::string& client_key = "");
    void disconnect();
    bool isConnected() const;
    bool connected() const { return isConnected(); }

    bool publish(const std::string& topic, const std::string& message);
    bool publishSecure(const std::string& topic, const std::string& message, bool retain = false);
    bool subscribe(const std::string& topic);

    bool publishError(int tx_id, const std::string& error_type, const std::string& error_message);

    void setCredentials(const std::string& username, const std::string& password);
    void setWillMessage(const std::string& topic, const std::string& message, int qos = 1, bool retain = false);
    bool validateTopic(const std::string& topic) const;
    bool validateMessage(const std::string& message) const;

    void setMessageCallback(std::function<void(const std::string&, const std::string&)> callback);
    void loop();

    static void setGlobalInstance(MqttClient* instance);
    static MqttClient* global_instance_;

    std::function<void(const std::string&, const std::string&)> messageCallback;

private:
#ifdef _WIN32
    bool connected;
    std::string username_;
    std::string password_;
    std::string will_topic_;
    std::string will_message_;
    bool use_tls_;
    std::string ca_cert_;
    std::string client_cert_;
    std::string client_key_;
    std::string broker;
    int port;
    std::string client_id;
    std::function<void(const std::string&, const std::string&)> messageCallback;
#elif defined(ESP32) || defined(ESP_PLATFORM)
            PubSubClient* mqtt_client_;
            bool is_connected_;
            std::string username_;
            std::string password_;
            std::string will_topic_;
            std::string will_message_;
            bool use_tls_;
            std::string ca_cert_;
            std::string client_cert_;
            std::string client_key_;
            std::string broker;
            int port;
            std::string client_id;
#else
    struct mosquitto* mosq;
    bool connected;
    std::string broker;
    int port;
    std::string client_id;
    std::function<void(const std::string&, const std::string&)> messageCallback;

    static void onConnect(struct mosquitto* mosq, void* userdata, int result);
    static void onDisconnect(struct mosquitto* mosq, void* userdata, int result);
    static void onMessage(struct mosquitto* mosq, void* userdata,
                         const struct mosquitto_message* message);
#endif

    void onMQTTMessage(char* topic, uint8_t* payload, unsigned int length);
    static void staticOnMQTTMessage(char* topic, uint8_t* payload, unsigned int length);

};
