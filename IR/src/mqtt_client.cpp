#include "network/mqtt_client.h"
#include <iostream>
#include <cstring>
#include <chrono>
#include <atomic>
#include <sstream>
#include <vector>

#ifdef _WIN32
// Windows 환경에서는 시뮬레이션
#include <thread>
#include <atomic>
#elif defined(ESP32) || defined(ESP_PLATFORM)
// ESP32 환경에서는 시뮬레이션 MQTT 클라이언트 사용
#include "esp_log.h"
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
    use_tls_ = false;
    username_ = "";
    password_ = "";
    will_topic_ = "";
    will_message_ = "";
#elif defined(ESP32) || defined(ESP_PLATFORM)
    // ESP32 환경 초기화
    mqtt_client_ = nullptr;
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
#elif defined(ESP32) || defined(ESP_PLATFORM)
    // ESP32 환경 정리
    if (mqtt_client_) {
        mqtt_client_ = nullptr;
        ESP_LOGI("MQTT", "MQTT 클라이언트 시뮬레이션 정리 완료");
    }
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
#elif defined(ESP32) || defined(ESP_PLATFORM)
    // ESP32 환경에서는 시뮬레이션 MQTT 클라이언트 사용
    mqtt_client_ = (void*)0x12345678;  // 시뮬레이션용 포인터
    connected = true;
    ESP_LOGI("MQTT", "MQTT 연결 시뮬레이션: %s:%d", broker.c_str(), port);
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
#elif defined(ESP32) || defined(ESP_PLATFORM)
    connected = false;
    ESP_LOGI("MQTT", "MQTT 연결 해제 시뮬레이션");
#else
    if (mosq && connected) {
        mosquitto_disconnect(mosq);
    }
#endif
}

bool MqttClient::isConnected() const {
    return connected;
}

bool MqttClient::connectSecure(const std::string& broker, int port,
                              const std::string& ca_cert,
                              const std::string& client_cert,
                              const std::string& client_key) {
    this->broker = broker;
    this->port = port;
    this->use_tls_ = true;
    this->ca_cert_ = ca_cert;
    this->client_cert_ = client_cert;
    this->client_key_ = client_key;

#ifdef _WIN32
    // Windows 시뮬레이션
    connected = true;
    std::cout << "MQTT 보안 연결 시뮬레이션: " << broker << ":" << port << " (TLS)" << std::endl;
    return true;
#elif defined(ESP32) || defined(ESP_PLATFORM)
    // ESP32 환경에서는 시뮬레이션 MQTT 보안 연결
    connected = true;
    ESP_LOGI("MQTT", "MQTT 보안 연결 시뮬레이션: %s:%d (TLS)", broker.c_str(), port);
    return true;
#else
    // Linux mosquitto TLS 구현
    if (!mosq) {
        std::cerr << "MQTT 클라이언트가 초기화되지 않음" << std::endl;
        return false;
    }

    // TLS 설정
    if (!ca_cert.empty()) {
        int result = mosquitto_tls_set(mosq, ca_cert.c_str(), nullptr,
                                      client_cert.c_str(), client_key.c_str(), nullptr);
        if (result != MOSQ_ERR_SUCCESS) {
            std::cerr << "TLS 설정 실패: " << mosquitto_strerror(result) << std::endl;
            return false;
        }
    }

    int result = mosquitto_connect(mosq, broker.c_str(), port, 60);
    if (result == MOSQ_ERR_SUCCESS) {
        std::cout << "MQTT 보안 연결 시도: " << broker << ":" << port << " (TLS)" << std::endl;
        return true;
    } else {
        std::cerr << "MQTT 보안 연결 실패: " << mosquitto_strerror(result) << std::endl;
        return false;
    }
#endif
}

void MqttClient::setCredentials(const std::string& username, const std::string& password) {
    username_ = username;
    password_ = password;

#ifdef ESP32
    // ESP32 환경에서는 시뮬레이션
    ESP_LOGI("MQTT", "MQTT 자격 증명 설정 시뮬레이션: %s", username.c_str());
#elif !defined(_WIN32)
    if (mosq) {
        mosquitto_username_pw_set(mosq, username.c_str(), password.c_str());
    }
#endif
}

void MqttClient::setWillMessage(const std::string& topic, const std::string& message, int qos, bool retain) {
    will_topic_ = topic;
    will_message_ = message;

#ifdef ESP32
    // ESP32 환경에서는 시뮬레이션
    ESP_LOGI("MQTT", "MQTT Will 메시지 설정 시뮬레이션: %s", topic.c_str());
#elif !defined(_WIN32)
    if (mosq) {
        mosquitto_will_set(mosq, topic.c_str(), message.length(), message.c_str(), qos, retain);
    }
#endif
}

bool MqttClient::validateTopic(const std::string& topic) const {
    // MQTT 토픽 검증
    if (topic.empty() || topic.length() > 65535) {
        return false;
    }

    // 금지된 문자 검사
    if (topic.find('\0') != std::string::npos) {
        return false;
    }

    // 토픽 레벨 검증
    std::vector<std::string> levels;
    std::stringstream ss(topic);
    std::string level;

    while (std::getline(ss, level, '/')) {
        if (level.empty() && !levels.empty() && !topic.empty()) {
            // 연속된 슬래시는 허용되지 않음
            return false;
        }
        levels.push_back(level);
    }

    return true;
}

bool MqttClient::validateMessage(const std::string& message) const {
    // 메시지 크기 검증
    if (message.length() > 268435455) { // MQTT 최대 메시지 크기
        return false;
    }

    // NULL 바이트 검사
    if (message.find('\0') != std::string::npos) {
        return false;
    }

    return true;
}

bool MqttClient::publishSecure(const std::string& topic, const std::string& message, bool retain) {
    // 보안 검증
    if (!validateTopic(topic)) {
        std::cerr << "잘못된 토픽: " << topic << std::endl;
        return false;
    }

    if (!validateMessage(message)) {
        std::cerr << "잘못된 메시지" << std::endl;
        return false;
    }

    return publish(topic, message);
}

bool MqttClient::publish(const std::string& topic, const std::string& message) {
#ifdef _WIN32
    // Windows 시뮬레이션
    std::cout << "MQTT 발행 시뮬레이션: " << topic << " -> " << message << std::endl;
    return true;
#elif defined(ESP32) || defined(ESP_PLATFORM)
    // ESP32 환경에서는 시뮬레이션 MQTT 발행
    ESP_LOGI("MQTT", "MQTT 발행 시뮬레이션: %s -> %s", topic.c_str(), message.c_str());
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
#elif defined(ESP32) || defined(ESP_PLATFORM)
    // ESP32 환경에서는 시뮬레이션 MQTT 구독
    ESP_LOGI("MQTT", "MQTT 구독 시뮬레이션: %s", topic.c_str());
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
    // Windows 시뮬레이션 - 아무것도 하지 않음
#elif defined(ESP32) || defined(ESP_PLATFORM)
    // ESP32 환경에서는 시뮬레이션 - 아무것도 하지 않음
#else
    if (mosq) {
        mosquitto_loop(mosq, 0, 1);
    }
#endif
}

#if !defined(_WIN32) && !defined(ESP32) && !defined(ESP_PLATFORM)
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
void MqttClient::onMQTTMessage(char* topic, uint8_t* payload, unsigned int length) {
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
void MqttClient::staticOnMQTTMessage(char* topic, uint8_t* payload, unsigned int length) {
    // 전역 인스턴스를 통해 실제 콜백 호출
    if (global_instance_) {
        global_instance_->onMQTTMessage(topic, payload, length);
    } else {
        std::cout << "MQTT 전역 인스턴스가 설정되지 않음" << std::endl;
    }
}
