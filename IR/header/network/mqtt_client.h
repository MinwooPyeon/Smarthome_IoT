#ifndef MQTT_CLIENT_H
#define MQTT_CLIENT_H

#include <string>
#include <functional>

#ifdef _WIN32
// Windows 환경에서는 시뮬레이션
#else
// Linux 환경에서는 실제 mosquitto 사용
#include <mosquitto.h>
#endif

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
    // Windows 시뮬레이션 멤버
    bool connected;
    std::string broker;
    int port;
    std::function<void(const std::string&, const std::string&)> messageCallback;
#else
    // Linux 실제 구현 멤버
    struct mosquitto* mosq;
    bool connected;
    std::string broker;
    int port;
    std::function<void(const std::string&, const std::string&)> messageCallback;
    
    static void onConnect(struct mosquitto* mosq, void* userdata, int result);
    static void onMessage(struct mosquitto* mosq, void* userdata, const struct mosquitto_message* message);
    static void onDisconnect(struct mosquitto* mosq, void* userdata, int result);
#endif
};

#endif // MQTT_CLIENT_H
