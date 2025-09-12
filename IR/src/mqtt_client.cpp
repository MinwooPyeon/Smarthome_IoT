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

// 전역 인스턴스 초기화 (ESP32 PubSubClient용)
MqttClient* MqttClient::global_instance_ = nullptr;

MqttClient::MqttClient() {
#ifdef _WIN32
    connected = false;
    port = 1883;
    client_id = "irremote_client";
#else
    mosq = nullptr;
    connected = false;
    port = 1883;
    client_id = "irremote_client";
    
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
    connected = false;
#else
    if (mosq) {
        mosquitto_destroy(mosq);
        mosq = nullptr;
    }
    mosquitto_lib_cleanup();
#endif
}

bool MqttClient::connect(const std::string& broker, int port) {
    this->broker = broker;
    this->port = port;
    
#ifdef _WIN32
    // Windows 시뮬레이션
    connected = true;
    std::cout << "MQTT 연결 시뮬레이션: " << broker << ":" << port << std::endl;
    return true;
#else
    // Linux mosquitto
    if (!mosq) {
        std::cerr << "MQTT 클라이언트가 초기화되지 않음" << std::endl;
        return false;
    }
    
    int result = mosquitto_connect(mosq, broker.c_str(), port, 60);
    if (result == MOSQ_ERR_SUCCESS) {
        std::cout << "MQTT 연결 시도: " << broker << ":" << port << std::endl;
        return true;
    } else {
        std::cerr << "MQTT 연결 실패: " << mosquitto_strerror(result) << std::endl;
        return false;
    }
#endif
}

void MqttClient::disconnect() {
#ifdef _WIN32
    connected = false;
    std::cout << "MQTT 연결 해제 시뮬레이션" << std::endl;
#else
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
    std::cout << "MQTT 발행 시뮬레이션: " << topic << " -> " << message << std::endl;
    return true;
#else
    // Linux mosquitto
    if (!mosq || !connected) {
        return false;
    }
    
    int result = mosquitto_publish(mosq, nullptr, topic.c_str(), 
                                  message.length(), message.c_str(), 0, false);
    return result == MOSQ_ERR_SUCCESS;
#endif
}

bool MqttClient::subscribe(const std::string& topic) {
#ifdef _WIN32
    // Windows 시뮬레이션
    std::cout << "MQTT 구독 시뮬레이션: " << topic << std::endl;
    return true;
#else
    // Linux mosquitto
    if (!mosq || !connected) {
        return false;
    }

    int result = mosquitto_subscribe(mosq, nullptr, topic.c_str(), 0);
    return result == MOSQ_ERR_SUCCESS;
#endif
}

void MqttClient::setMessageCallback(std::function<void(const std::string&, const std::string&)> callback) {
    messageCallback = callback;
}

void MqttClient::loop() {
#ifdef _WIN32
#else
    if (mosq) {
        mosquitto_loop(mosq, 0, 1);
    }
#endif
}

#ifndef _WIN32
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

// ESP32 PubSubClient용 정적 콜백 함수들 (호환성 유지)
void MqttClient::onMQTTMessage(char* topic, byte* payload, unsigned int length) {
    // 페이로드를 문자열로 변환
    std::string message;
    message.assign((char*)payload, length);
    
    std::string topic_str(topic);
    
    std::cout << "MQTT 메시지 수신: " << topic_str << " -> " << message << std::endl;
    
    // 콜백 함수 호출
    if (messageCallback) {
        messageCallback(topic_str, message);
    }
}

void MqttClient::setGlobalInstance(MqttClient* instance) {
    global_instance_ = instance;
    std::cout << "MQTT 전역 인스턴스 설정 완료" << std::endl;
}

// 정적 콜백 함수 (ESP32 PubSubClient용)
void MqttClient::staticOnMQTTMessage(char* topic, byte* payload, unsigned int length) {
    // 전역 인스턴스를 통해 실제 콜백 호출
    if (global_instance_) {
        global_instance_->onMQTTMessage(topic, payload, length);
    } else {
        std::cout << "MQTT 전역 인스턴스가 설정되지 않음" << std::endl;
    }
}