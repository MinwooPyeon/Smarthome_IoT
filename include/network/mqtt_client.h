#ifndef MQTT_CLIENT_H
#define MQTT_CLIENT_H

#include <string>
#include <functional>
#include <mosquitto.h>

class MQTTClient {
public:
    MQTTClient();
    ~MQTTClient();
    
    bool connect(const std::string& broker, int port = 1883);
    void disconnect();
    bool isConnected() const;
    
    bool publish(const std::string& topic, const std::string& message);
    bool subscribe(const std::string& topic);
    
    void setMessageCallback(std::function<void(const std::string&, const std::string&)> callback);
    void loop();

private:
    struct mosquitto* mosq;
    bool connected;
    std::function<void(const std::string&, const std::string&)> messageCallback;
    
    static void onConnect(struct mosquitto* mosq, void* userdata, int result);
    static void onMessage(struct mosquitto* mosq, void* userdata, const struct mosquitto_message* message);
    static void onDisconnect(struct mosquitto* mosq, void* userdata, int result);
};

#endif // MQTT_CLIENT_H
