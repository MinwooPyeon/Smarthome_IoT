#pragma once

#include <string>
#include <functional>

#ifdef _WIN32
// Windows 환경에서는 시뮬레이션
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
    void disconnect();
    bool isConnected() const;
    
    bool publish(const std::string& topic, const std::string& message);
    bool subscribe(const std::string& topic);
    
    void setMessageCallback(std::function<void(const std::string&, const std::string&)> callback);
    void loop();

private:
#ifdef _WIN32
    // Windows 시뮬레이션
    bool connected;
    std::string broker;
    int port;
    std::string client_id;
    std::function<void(const std::string&, const std::string&)> messageCallback;
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
    void onMQTTMessage(char* topic, byte* payload, unsigned int length);
    static void staticOnMQTTMessage(char* topic, byte* payload, unsigned int length);
    
    // 전역 인스턴스 관리 (ESP32 PubSubClient용)
    static MqttClient* global_instance_;
    static void setGlobalInstance(MqttClient* instance);
};

#endif // MQTT_CLIENT_H