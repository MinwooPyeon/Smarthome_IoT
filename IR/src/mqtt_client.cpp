#include "network/mqtt_client.h"
#include <iostream>
#include <cstring>
#include <chrono>



MqttClient::MqttClient() 
    : mosq_(nullptr), port_(1883), connected_(false) {
    // Mosquitto 초기화
    mosquitto_lib_init();
    mosq_ = mosquitto_new(nullptr, true, this);
    
    if (mosq_) {
        // 콜백 함수 설정
        mosquitto_connect_callback_set(mosq_, onConnect);
        mosquitto_disconnect_callback_set(mosq_, onDisconnect);
        mosquitto_message_callback_set(mosq_, onMessage);
        mosquitto_publish_callback_set(mosq_, onPublish);
    }
}

MqttClient::~MqttClient() {
    if (mosq_) {
        mosquitto_destroy(mosq_);
        mosq_ = nullptr;
    }
    mosquitto_lib_cleanup();
}

MqttClient::MqttClient(MqttClient&& other) noexcept
    : mosq_(other.mosq_), device_id_(std::move(other.device_id_)),
      broker_(std::move(other.broker_)), port_(other.port_), connected_(other.connected_),
      order_callback_(std::move(other.order_callback_)),
      connection_callback_(std::move(other.connection_callback_)),
      publish_callbacks_(std::move(other.publish_callbacks_)) {
    other.mosq_ = nullptr;
    other.connected_ = false;
}

MqttClient& MqttClient::operator=(MqttClient&& other) noexcept {
    if (this != &other) {
        if (mosq_) {
            mosquitto_destroy(mosq_);
        }
        
        mosq_ = other.mosq_;
        device_id_ = std::move(other.device_id_);
        broker_ = std::move(other.broker_);
        port_ = other.port_;
        connected_ = other.connected_;
        order_callback_ = std::move(other.order_callback_);
        connection_callback_ = std::move(other.connection_callback_);
        publish_callbacks_ = std::move(other.publish_callbacks_);
        
        other.mosq_ = nullptr;
        other.connected_ = false;
    }
    return *this;
}

bool MqttClient::connect(const std::string& broker, int port, 
                        const std::string& username, const std::string& password,
                        const std::string& client_id) {
    if (!mosq_) {
        std::cerr << "Mosquitto instance not initialized" << std::endl;
        return false;
    }
    
    broker_ = broker;
    port_ = port;
    
    // 사용자명/비밀번호 설정
    if (!username.empty()) {
        if (mosquitto_username_pw_set(mosq_, username.c_str(), password.c_str()) != MOSQ_ERR_SUCCESS) {
            std::cerr << "Failed to set username/password" << std::endl;
            return false;
        }
    }
    
    // 클라이언트 ID 설정
    std::string clientId = client_id;
    if (clientId.empty()) {
        clientId = "irremote_" + std::to_string(getpid());
    }
    
    // 연결 시도
    int result = mosquitto_connect(mosq_, broker.c_str(), port, 60);
    if (result != MOSQ_ERR_SUCCESS) {
        std::cerr << "Failed to connect to MQTT broker: " << mosquitto_strerror(result) << std::endl;
        return false;
    }
    
    std::cout << "Connecting to MQTT broker: " << broker << ":" << port << std::endl;
    return true;
}

void MqttClient::disconnect() {
    if (mosq_ && connected_) {
        mosquitto_disconnect(mosq_);
        connected_ = false;
    }
}

bool MqttClient::isConnected() const {
    return connected_;
}

bool MqttClient::subscribeToOrders(const std::string& deviceId, int qos) {
    if (!mosq_ || !connected_) {
        std::cerr << "Not connected to MQTT broker" << std::endl;
        return false;
    }
    
    std::string topic = buildTopic("order", deviceId);
    int result = mosquitto_subscribe(mosq_, nullptr, topic.c_str(), qos);
    
    if (result == MOSQ_ERR_SUCCESS) {
        std::cout << "Subscribed to orders: " << topic << std::endl;
        return true;
    } else {
        std::cerr << "Failed to subscribe: " << mosquitto_strerror(result) << std::endl;
        return false;
    }
}

bool MqttClient::unsubscribeFromOrders(const std::string& deviceId) {
    if (!mosq_ || !connected_) {
        return false;
    }
    
    std::string topic = buildTopic("order", deviceId);
    int result = mosquitto_unsubscribe(mosq_, nullptr, topic.c_str());
    
    if (result == MOSQ_ERR_SUCCESS) {
        std::cout << "Unsubscribed from orders: " << topic << std::endl;
        return true;
    } else {
        std::cerr << "Failed to unsubscribe: " << mosquitto_strerror(result) << std::endl;
        return false;
    }
}

bool MqttClient::publishIRSignal(const std::string& deviceId, const IRSignalMessage& message, 
                                int qos, PublishCallback callback) {
    nlohmann::json payload = message.toJson();
    std::string topic = buildTopic("irsignal", deviceId);
    return publishMessage(topic, payload, qos, callback);
}

bool MqttClient::publishEnvironment(const std::string& deviceId, const EnvMessage& message,
                                  int qos, PublishCallback callback) {
    nlohmann::json payload = message.toJson();
    std::string topic = buildTopic("env", deviceId);
    return publishMessage(topic, payload, qos, callback);
}

bool MqttClient::publishAck(const std::string& deviceId, const AckMessage& message,
                           int qos, PublishCallback callback) {
    nlohmann::json payload = message.toJson();
    std::string topic = buildTopic("order/ack", deviceId);
    return publishMessage(topic, payload, qos, callback);
}

bool MqttClient::publishError(const std::string& deviceId, const ErrorMessage& message,
                             int qos, PublishCallback callback) {
    nlohmann::json payload = message.toJson();
    std::string topic = buildTopic("error", deviceId);
    return publishMessage(topic, payload, qos, callback);
}

bool MqttClient::publishState(const std::string& deviceId, const StateMessage& message,
                             int qos, PublishCallback callback) {
    nlohmann::json payload = message.toJson();
    std::string topic = buildTopic("state", deviceId);
    return publishMessage(topic, payload, qos, callback);
}

bool MqttClient::loop(int timeout_ms) {
    if (!mosq_) {
        return false;
    }
    
    int result = mosquitto_loop(mosq_, timeout_ms, 1);
    if (result != MOSQ_ERR_SUCCESS && result != MOSQ_ERR_NO_CONN) {
        std::cerr << "MQTT loop error: " << mosquitto_strerror(result) << std::endl;
        return false;
    }
    
    return true;
}

bool MqttClient::publishMessage(const std::string& topic, const nlohmann::json& payload,
                               int qos, PublishCallback callback) {
    if (!mosq_ || !connected_) {
        std::cerr << "Not connected to MQTT broker" << std::endl;
        return false;
    }
    
    std::string payloadStr = payload.dump();
    int mid = mosquitto_publish(mosq_, nullptr, topic.c_str(), 
                               payloadStr.length(), payloadStr.c_str(), qos, false);
    
    if (mid > 0) {
        if (callback) {
            std::lock_guard<std::mutex> lock(callback_mutex_);
            publish_callbacks_[mid] = callback;
        }
        std::cout << "Published message to " << topic << " (MID: " << mid << ")" << std::endl;
        return true;
    } else {
        std::cerr << "Failed to publish message: " << mosquitto_strerror(mid) << std::endl;
        return false;
    }
}

std::string MqttClient::buildTopic(const std::string& type, const std::string& deviceId) const {
    return "hub/" + deviceId + "/" + type;
}

void MqttClient::handleOrderMessage(const std::string& payload) {
    try {
        nlohmann::json json = nlohmann::json::parse(payload);
        OrderMessage order;
        order.fromJson(json);
        
        if (order_callback_) {
            order_callback_(order);
        }
    } catch (const std::exception& e) {
        std::cerr << "Failed to parse order message: " << e.what() << std::endl;
        
        // 에러 메시지 발행
        ErrorMessage error(device_id_, "error/1.0");
        error.level = "ERROR";
        error.code = "PARSE_ERROR";
        error.detail = "Failed to parse order message";
        error.ctx["payload"] = payload;
        
        publishError(device_id_, error);
    }
}

// Static callback functions
void MqttClient::onConnect(struct mosquitto* mosq, void* userdata, int result) {
    MqttClient* client = static_cast<MqttClient*>(userdata);
    
    if (result == 0) {
        client->connected_ = true;
        std::cout << "Connected to MQTT broker successfully" << std::endl;
        
        // 연결 상태 콜백 호출
        if (client->connection_callback_) {
            client->connection_callback_(true);
        }
        
        // 디바이스 상태를 online으로 발행
        StateMessage state(client->device_id_, "state/1.0");
        state.status = "online";
        client->publishState(client->device_id_, state, 1, true);
        
    } else {
        client->connected_ = false;
        std::cerr << "Failed to connect to MQTT broker: " << result << std::endl;
        
        if (client->connection_callback_) {
            client->connection_callback_(false);
        }
    }
}

void MqttClient::onDisconnect(struct mosquitto* mosq, void* userdata, int result) {
    MqttClient* client = static_cast<MqttClient*>(userdata);
    client->connected_ = false;
    
    std::cout << "Disconnected from MQTT broker";
    if (result != 0) {
        std::cout << " (reason: " << result << ")";
    }
    std::cout << std::endl;
    
    if (client->connection_callback_) {
        client->connection_callback_(false);
    }
}

void MqttClient::onMessage(struct mosquitto* mosq, void* userdata, 
                          const struct mosquitto_message* message) {
    MqttClient* client = static_cast<MqttClient*>(userdata);
    
    if (message->payloadlen > 0) {
        std::string payload(static_cast<char*>(message->payload), message->payloadlen);
        std::string topic(message->topic);
        
        std::cout << "Received message on topic: " << topic << std::endl;
        
        // 명령 메시지 처리
        if (topic.find("/order") != std::string::npos) {
            client->handleOrderMessage(payload);
        }
    }
}

void MqttClient::onPublish(struct mosquitto* mosq, void* userdata, int mid) {
    MqttClient* client = static_cast<MqttClient*>(userdata);
    
    std::lock_guard<std::mutex> lock(client->callback_mutex_);
    auto it = client->publish_callbacks_.find(mid);
    if (it != client->publish_callbacks_.end()) {
        if (it->second) {
            it->second(true, mid);
        }
        client->publish_callbacks_.erase(it);
    }
}
