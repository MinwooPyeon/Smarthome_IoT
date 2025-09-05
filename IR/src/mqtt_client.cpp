#include "network/mqtt_client.h"
#include "core/platform.h"
#include <iostream>
#include <cstring>
#include <chrono>
#include <atomic>

#ifdef PLATFORM_ESP32
#include "WiFi.h"
#include "PubSubClient.h"
#include <WiFiClient.h>
#elif defined(PLATFORM_WINDOWS)
#include <thread>
#include <atomic>
#elif defined(PLATFORM_LINUX)
#include <mosquitto.h>
#endif

// MqttClient 클래스 구현
MqttClient::MqttClient() {
#ifdef PLATFORM_ESP32
    wifi_client_ = new WiFiClient();
    mqtt_client_ = new PubSubClient(*wifi_client_);
    connected = false;
    port = 1883;
    client_id = "esp32_ir_controller";
    
    // ESP32 MQTT 콜백 설정
    mqtt_client_->setCallback(staticOnMQTTMessage);
    mqtt_client_->setBufferSize(1024);  // 버퍼 크기 설정
    mqtt_client_->setKeepAlive(60);     // Keep-alive 설정
#elif defined(PLATFORM_WINDOWS)
    connected = false;
    port = 1883;
#elif defined(PLATFORM_LINUX)
    mosq = nullptr;
    connected = false;
    port = 1883;
    
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
#ifdef PLATFORM_ESP32
    this->broker = broker;
    this->port = port;
    
    mqtt_client_->setServer(broker.c_str(), port);
    
    // ESP32 MQTT 연결 시도
    int retry_count = 0;
    while (!mqtt_client_->connected() && retry_count < 5) {
        ESP_LOGI("MQTT", "MQTT 브로커 연결 시도 %d/5", retry_count + 1);
        
        if (mqtt_client_->connect(client_id.c_str())) {
            connected = true;
            ESP_LOGI("MQTT", "MQTT 브로커 연결 성공: %s:%d", broker.c_str(), port);
            
            // 기본 토픽 구독
            subscribe("irremote/command");
            subscribe("irremote/learn");
            subscribe("irremote/status");
            
            return true;
        } else {
            ESP_LOGE("MQTT", "MQTT 연결 실패: %d", mqtt_client_->state());
            retry_count++;
            vTaskDelay(pdMS_TO_TICKS(2000));
        }
    }
    
    ESP_LOGE("MQTT", "MQTT 브로커 연결 최종 실패");
    return false;
#elif defined(_WIN32)
    this->broker = broker;
    this->port = port;
    connected = true;
    std::cout << "Windows 시뮬레이션: MQTT 브로커 연결됨 " << broker << ":" << port << std::endl;
    return true;
#else
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
    connected = false;
    std::cout << "Windows 시뮬레이션: MQTT 브로커 연결 해제됨" << std::endl;
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
    if (!connected) return false;
    std::cout << "Windows 시뮬레이션: MQTT 발행 " << topic << " -> " << message << std::endl;
    return true;
#else
    if (!mosq || !connected) return false;
    
    int result = mosquitto_publish(mosq, nullptr, topic.c_str(), 
                                 message.length(), message.c_str(), 0, false);
    return result == MOSQ_ERR_SUCCESS;
#endif
}

bool MqttClient::subscribe(const std::string& topic) {
#ifdef _WIN32
    if (!connected) return false;
    std::cout << "Windows 시뮬레이션: MQTT 구독 " << topic << std::endl;
    return true;
#else
    if (!mosq || !connected) return false;
    
    int result = mosquitto_subscribe(mosq, nullptr, topic.c_str(), 0);
    return result == MOSQ_ERR_SUCCESS;
#endif
}

void MqttClient::setMessageCallback(std::function<void(const std::string&, const std::string&)> callback) {
    messageCallback = callback;
}

void MqttClient::loop() {
#ifdef PLATFORM_ESP32
    if (mqtt_client_ && connected) {
        if (!mqtt_client_->connected()) {
            // 연결 끊김 감지 - 재연결 시도
            connected = false;
            ESP_LOGW("MQTT", "MQTT 연결 끊김 감지, 재연결 시도");
            
            if (mqtt_client_->connect(client_id.c_str())) {
                connected = true;
                ESP_LOGI("MQTT", "MQTT 재연결 성공");
                
                // 토픽 재구독
                subscribe("irremote/command");
                subscribe("irremote/learn");
                subscribe("irremote/status");
            }
        } else {
            mqtt_client_->loop();
        }
    }
#elif defined(_WIN32)
    // Windows에서는 아무것도 하지 않음
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

#ifdef PLATFORM_ESP32
// ESP32 MQTT 메시지 콜백 함수
void MqttClient::onMQTTMessage(char* topic, byte* payload, unsigned int length) {
    // 페이로드를 문자열로 변환
    std::string message;
    message.assign((char*)payload, length);
    
    std::string topic_str(topic);
    
    ESP_LOGI("MQTT", "메시지 수신: %s -> %s", topic_str.c_str(), message.c_str());
    
    // 콜백 함수 호출
    if (messageCallback) {
        messageCallback(topic_str, message);
    }
}

// 정적 콜백 함수 (ESP32 PubSubClient용)
void MqttClient::staticOnMQTTMessage(char* topic, byte* payload, unsigned int length) {
    // 전역 인스턴스에 위임 (실제로는 인스턴스 포인터를 전달해야 함)
    // TODO: 인스턴스 포인터를 전달하는 방법 구현 필요
    ESP_LOGI("MQTT", "정적 콜백 호출됨");
}
#endif
