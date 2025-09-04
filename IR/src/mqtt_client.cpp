#include "network/mqtt_client.h"
#include <iostream>
#include <cstring>
#include <chrono>
#include <atomic>

#ifdef _WIN32
// Windows 환경에서는 시뮬레이션
#include <thread>
#include <atomic>
#else
// Linux 환경에서는 실제 mosquitto 사용
#include <mosquitto.h>
#endif

// MqttClient 클래스 구현
MqttClient::MqttClient() {
#ifdef _WIN32
    // Windows 시뮬레이션 초기화
    connected = false;
    port = 1883;
#else
    // Linux 실제 구현 초기화
    mosq = nullptr;
    connected = false;
    port = 1883;
    
    // Mosquitto 초기화
    mosquitto_lib_init();
    mosq = mosquitto_new(nullptr, true, this);
    
    if (mosq) {
        // 콜백 함수 설정
        mosquitto_connect_callback_set(mosq, onConnect);
        mosquitto_disconnect_callback_set(mosq, onDisconnect);
        mosquitto_message_callback_set(mosq, onMessage);
    }
#endif
}

MqttClient::~MqttClient() {
#ifdef _WIN32
    // Windows 시뮬레이션 정리
    connected = false;
#else
    // Linux 실제 구현 정리
    if (mosq) {
        mosquitto_destroy(mosq);
        mosq = nullptr;
    }
    mosquitto_lib_cleanup();
#endif
}

bool MqttClient::connect(const std::string& broker, int port) {
#ifdef _WIN32
    // Windows 시뮬레이션
    this->broker = broker;
    this->port = port;
    connected = true;
    std::cout << "Windows 시뮬레이션: MQTT 브로커 연결됨 " << broker << ":" << port << std::endl;
    return true;
#else
    // Linux 실제 구현
    if (!mosq) return false;
    
    this->broker = broker;
    this->port = port;
    
    int result = mosquitto_connect(mosq, broker.c_str(), port, 60);
    if (result != MOSQ_ERR_SUCCESS) {
        std::cerr << "Failed to connect to MQTT broker: " << mosquitto_strerror(result) << std::endl;
        return false;
    }
    
    std::cout << "Connecting to MQTT broker: " << broker << ":" << port << std::endl;
    return true;
#endif
}

void MqttClient::disconnect() {
#ifdef _WIN32
    // Windows 시뮬레이션
    connected = false;
    std::cout << "Windows 시뮬레이션: MQTT 브로커 연결 해제됨" << std::endl;
#else
    // Linux 실제 구현
    if (mosq && connected) {
        mosquitto_disconnect(mosq);
    }
#endif
}

bool MqttClient::isConnected() const {
    return connected;
}

bool MqttClient::publish(const std::string& topic, const std::string& message) {
#ifdef _WIN32
    // Windows 시뮬레이션
    if (!connected) return false;
    std::cout << "Windows 시뮬레이션: MQTT 발행 " << topic << " -> " << message << std::endl;
    return true;
#else
    // Linux 실제 구현
    if (!mosq || !connected) return false;
    
    int result = mosquitto_publish(mosq, nullptr, topic.c_str(), 
                                 message.length(), message.c_str(), 0, false);
    return result == MOSQ_ERR_SUCCESS;
#endif
}

bool MqttClient::subscribe(const std::string& topic) {
#ifdef _WIN32
    // Windows 시뮬레이션
    if (!connected) return false;
    std::cout << "Windows 시뮬레이션: MQTT 구독 " << topic << std::endl;
    return true;
#else
    // Linux 실제 구현
    if (!mosq || !connected) return false;
    
    int result = mosquitto_subscribe(mosq, nullptr, topic.c_str(), 0);
    return result == MOSQ_ERR_SUCCESS;
#endif
}

void MqttClient::setMessageCallback(std::function<void(const std::string&, const std::string&)> callback) {
    messageCallback = callback;
}

void MqttClient::loop() {
#ifdef _WIN32
    // Windows에서는 아무것도 하지 않음
#else
    // Linux 실제 구현
    if (mosq) {
        mosquitto_loop(mosq, 0, 1);
    }
#endif
}

#ifndef _WIN32
// Linux 전용 정적 콜백 함수들
void MqttClient::onConnect(struct mosquitto* mosq, void* userdata, int result) {
    MqttClient* client = static_cast<MqttClient*>(userdata);
    client->connected = (result == 0);
    
    if (result == 0) {
        std::cout << "Connected to MQTT broker successfully" << std::endl;
    } else {
        std::cerr << "Failed to connect to MQTT broker: " << result << std::endl;
    }
}

void MqttClient::onDisconnect(struct mosquitto* mosq, void* userdata, int result) {
    MqttClient* client = static_cast<MqttClient*>(userdata);
    client->connected = false;
    std::cout << "Disconnected from MQTT broker" << std::endl;
}

void MqttClient::onMessage(struct mosquitto* mosq, void* userdata, 
                         const struct mosquitto_message* message) {
    MqttClient* client = static_cast<MqttClient*>(userdata);
    
    if (message->payloadlen > 0 && client->messageCallback) {
        std::string payload(static_cast<char*>(message->payload), message->payloadlen);
        std::string topic(message->topic);
        client->messageCallback(topic, payload);
    }
}
#endif
