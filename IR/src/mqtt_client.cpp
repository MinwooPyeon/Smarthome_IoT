#include "network/mqtt_client.h"
#include <iostream>
#include <cstring>
#include <chrono>
#include <atomic>
#include <sstream>
#include <vector>

#if defined(ESP32) || defined(ESP_PLATFORM)
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#endif

MqttClient* MqttClient::global_instance_ = nullptr;

#ifdef ESP32
void mqtt_callback(char* topic, byte* payload, unsigned int length) {
    if (MqttClient::global_instance_ && MqttClient::global_instance_->messageCallback) {
        std::string topic_str(topic);
        std::string message((char*)payload, length);
        MqttClient::global_instance_->messageCallback(topic_str, message);
    }
}
#endif

MqttClient::MqttClient() {
#if defined(ESP32) || defined(ESP_PLATFORM)
    mqtt_client_ = new PubSubClient();
    is_connected_ = false;
    port = 1883;
    client_id = "irremote_client";
    use_tls_ = false;
    username_ = "";
    password_ = "";
    will_topic_ = "";
    will_message_ = "";

    global_instance_ = this;
#endif
}

MqttClient::~MqttClient() {
#if defined(ESP32) || defined(ESP_PLATFORM)
    if (mqtt_client_) {
        delete static_cast<PubSubClient*>(mqtt_client_);
        mqtt_client_ = nullptr;
    }
    Serial.println("MQTT 클라이언트 정리 완료");
#endif
}

bool MqttClient::connect(const std::string& broker, int port) {
    this->broker = broker;
    this->port = port;

#if defined(ESP32) || defined(ESP_PLATFORM)
    this->broker = broker;
    this->port = port;

    PubSubClient* client = static_cast<PubSubClient*>(mqtt_client_);
    client->setServer(broker.c_str(), port);
    client->setCallback(mqtt_callback);

    bool connected = false;
    if (!username_.empty() && !password_.empty()) {
        connected = client->connect(client_id.c_str(), username_.c_str(), password_.c_str());
    } else {
        connected = client->connect(client_id.c_str());
    }

    if (connected) {
        is_connected_ = true;
        Serial.printf("MQTT 연결 성공: %s:%d\n", broker.c_str(), port);

        if (!will_topic_.empty() && !will_message_.empty()) {
            client->publish(will_topic_.c_str(), will_message_.c_str(), true);
        }

        return true;
    } else {
        is_connected_ = false;
        Serial.printf("MQTT 연결 실패: %s:%d\n", broker.c_str(), port);
        return false;
    }
#endif
}

void MqttClient::disconnect() {
#if defined(ESP32) || defined(ESP_PLATFORM)
    if (mqtt_client_) {
        static_cast<PubSubClient*>(mqtt_client_)->disconnect();
    }
    is_connected_ = false;
    Serial.println("MQTT 연결 해제");
#else
    if (mosq && is_connected_) {
        mosquitto_disconnect(mosq);
    }
#endif
}

bool MqttClient::isConnected() const {
    return is_connected_;
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

 #if defined(ESP32) || defined(ESP_PLATFORM)
    is_connected_ = true;
    Serial.printf("MQTT 보안 연결 시뮬레이션: %s:%d (TLS)\n", broker.c_str(), port);
    return true;
#else
    if (!mosq) {
        std::cerr << "MQTT 클라이언트가 초기화되지 않음" << std::endl;
        return false;
    }

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
    Serial.printf("MQTT 자격 증명 설정: %s\n", username.c_str());
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
    Serial.printf("MQTT Will 메시지 설정: %s\n", topic.c_str());
#elif !defined(_WIN32)
    if (mosq) {
        mosquitto_will_set(mosq, topic.c_str(), message.length(), message.c_str(), qos, retain);
    }
#endif
}

bool MqttClient::validateTopic(const std::string& topic) const {
    if (topic.empty() || topic.length() > 65535) {
        return false;
    }

    if (topic.find('\0') != std::string::npos) {
        return false;
    }

    std::vector<std::string> levels;
    std::stringstream ss(topic);
    std::string level;

    while (std::getline(ss, level, '/')) {
        if (level.empty() && !levels.empty() && !topic.empty()) {
            return false;
        }
        levels.push_back(level);
    }

    return true;
}

bool MqttClient::validateMessage(const std::string& message) const {
    if (message.length() > 268435455) {
        return false;
    }

    if (message.find('\0') != std::string::npos) {
        return false;
    }

    return true;
}

bool MqttClient::publishSecure(const std::string& topic, const std::string& message, bool retain) {
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
#if defined(ESP32) || defined(ESP_PLATFORM)
    if (!mqtt_client_ || !is_connected_) {
        return false;
    }

    PubSubClient* client = static_cast<PubSubClient*>(mqtt_client_);
    bool result = client->publish(topic.c_str(), message.c_str());

    if (result) {
        Serial.printf("MQTT 발행 성공: %s -> %s\n", topic.c_str(), message.c_str());
    } else {
        Serial.printf("MQTT 발행 실패: %s\n", topic.c_str());
    }

    return result;
#else
    if (!mosq || !is_connected_) {
        return false;
    }

    int result = mosquitto_publish(mosq, nullptr, topic.c_str(),
                                  message.length(), message.c_str(), 0, false);
    return result == MOSQ_ERR_SUCCESS;
#endif
}

bool MqttClient::subscribe(const std::string& topic) {
#if defined(ESP32) || defined(ESP_PLATFORM)
    if (!mqtt_client_ || !is_connected_) {
        return false;
    }

    PubSubClient* client = static_cast<PubSubClient*>(mqtt_client_);
    bool result = client->subscribe(topic.c_str());

    if (result) {
        Serial.printf("MQTT 구독 성공: %s\n", topic.c_str());
    } else {
        Serial.printf("MQTT 구독 실패: %s\n", topic.c_str());
    }

    return result;
#else
    if (!mosq || !is_connected_) {
        return false;
    }

    int result = mosquitto_subscribe(mosq, nullptr, topic.c_str(), 0);
    return result == MOSQ_ERR_SUCCESS;
#endif
}

bool MqttClient::publishError(int tx_id, const std::string& error_type, const std::string& error_message) {
    DynamicJsonDocument doc(512);
    doc["tx_id"] = tx_id;
    doc["error"] = error_type.c_str();
    doc["message"] = error_message.c_str();

    std::string json_message;
    serializeJson(doc, json_message);

    std::string error_topic = "hub/test-device/error";

    return publish(error_topic, json_message);
}

void MqttClient::setMessageCallback(std::function<void(const std::string&, const std::string&)> callback) {
    messageCallback = callback;
}

void MqttClient::loop() {
#if defined(ESP32) || defined(ESP_PLATFORM)
    if (mqtt_client_) {
        static_cast<PubSubClient*>(mqtt_client_)->loop();
    }
#else
    if (mosq) {
        mosquitto_loop(mosq, 0, 1);
    }
#endif
}

#if !defined(_WIN32) && !defined(ESP32) && !defined(ESP_PLATFORM)
void MqttClient::onConnect(struct mosquitto* mosq, void* userdata, int result) {
    MqttClient* client = static_cast<MqttClient*>(userdata);
    client->is_connected_ = (result == 0);

    if (result == 0) {
        std::cout << "Connected to MQTT broker successfully" << std::endl;
    } else {
        std::cerr << "Failed to connect to MQTT broker: " << result << std::endl;
    }
}

void MqttClient::onDisconnect(struct mosquitto* mosq, void* userdata, int result) {
    MqttClient* client = static_cast<MqttClient*>(userdata);
    client->is_connected_ = false;
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

void MqttClient::onMQTTMessage(char* topic, uint8_t* payload, unsigned int length) {
    std::string message;
    message.assign((char*)payload, length);

    std::string topic_str(topic);

    std::cout << "MQTT 메시지 수신: " << topic_str << " -> " << message << std::endl;

    if (messageCallback) {
        messageCallback(topic_str, message);
    }
}

void MqttClient::setGlobalInstance(MqttClient* instance) {
    global_instance_ = instance;
    std::cout << "MQTT 전역 인스턴스 설정 완료" << std::endl;
}

void MqttClient::staticOnMQTTMessage(char* topic, uint8_t* payload, unsigned int length) {
    if (global_instance_) {
        global_instance_->onMQTTMessage(topic, payload, length);
    } else {
        std::cout << "MQTT 전역 인스턴스가 설정되지 않음" << std::endl;
    }
}
