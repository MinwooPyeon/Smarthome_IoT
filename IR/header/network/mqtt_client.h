#pragma once

#include <string>
#include <functional>

#ifdef _WIN32
// Windows 환경에서는 시뮬레이션
#elif defined(ESP32) || defined(ESP_PLATFORM)
// ESP32 Arduino 환경에서는 PubSubClient 사용
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include "stdint.h"
#else
// Linux 환경에서는 실제 mosquitto 사용
#include <mosquitto.h>
#endif

/**
 * @brief MQTT 클라이언트 클래스
 *
 * ESP32, Linux, Windows 플랫폼을 지원하는 MQTT 클라이언트입니다.
 */
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
    bool connected() const { return isConnected(); }  // 호환성을 위한 별칭

    bool publish(const std::string& topic, const std::string& message);
    bool publishSecure(const std::string& topic, const std::string& message, bool retain = false);
    bool subscribe(const std::string& topic);

    // 에러 메시지 전송 함수
    bool publishError(int tx_id, const std::string& error_type, const std::string& error_message);

    // 보안 관련 메서드
    void setCredentials(const std::string& username, const std::string& password);
    void setWillMessage(const std::string& topic, const std::string& message, int qos = 1, bool retain = false);
    bool validateTopic(const std::string& topic) const;
    bool validateMessage(const std::string& message) const;

    void setMessageCallback(std::function<void(const std::string&, const std::string&)> callback);
    void loop();

    // 전역 인스턴스 관리 (ESP32 PubSubClient용)
    static void setGlobalInstance(MqttClient* instance);
    static MqttClient* global_instance_;

    // 메시지 콜백 함수
    std::function<void(const std::string&, const std::string&)> messageCallback;

private:
#ifdef _WIN32
    // Windows 시뮬레이션
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
            // ESP32 Arduino PubSubClient
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
    // Linux mosquitto
    struct mosquitto* mosq;
    bool connected;
    std::string broker;
    int port;
    std::string client_id;
    std::function<void(const std::string&, const std::string&)> messageCallback;

    // mosquitto 콜백 함수들
    static void onConnect(struct mosquitto* mosq, void* userdata, int result);
    static void onDisconnect(struct mosquitto* mosq, void* userdata, int result);
    static void onMessage(struct mosquitto* mosq, void* userdata,
                         const struct mosquitto_message* message);
#endif

    // ESP32 PubSubClient용 정적 콜백 함수들 (호환성 유지)
    void onMQTTMessage(char* topic, uint8_t* payload, unsigned int length);
    static void staticOnMQTTMessage(char* topic, uint8_t* payload, unsigned int length);

};
