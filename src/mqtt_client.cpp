#include "mqtt_client.h"
#include <iostream>
#include <google/protobuf/message.h>

namespace irremote {

MqttClient::MqttClient() : mosq_(nullptr), connected_(false) {
    mosquitto_lib_init();
    mosq_ = mosquitto_new(nullptr, true, this);
    
    if (mosq_) {
        mosquitto_connect_callback_set(mosq_, onConnect);
        mosquitto_disconnect_callback_set(mosq_, onDisconnect);
        mosquitto_message_callback_set(mosq_, onMessage);
        mosquitto_subscribe_callback_set(mosq_, onSubscribe);
        mosquitto_publish_callback_set(mosq_, onPublish);
    }
}

MqttClient::~MqttClient() {
    if (mosq_) {
        mosquitto_destroy(mosq_);
    }
    mosquitto_lib_cleanup();
}

bool MqttClient::connect(const std::string& broker, int port, const std::string& username, 
                        const std::string& password, const std::string& clientId) {
    if (!mosq_) {
        return false;
    }

    if (!username.empty()) {
        mosquitto_username_pw_set(mosq_, username.c_str(), password.c_str());
    }

    int result = mosquitto_connect(mosq_, broker.c_str(), port, 60);
    if (result != MOSQ_ERR_SUCCESS) {
        std::cerr << "Failed to connect to MQTT broker: " << mosquitto_strerror(result) << std::endl;
        return false;
    }

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

bool MqttClient::subscribe(const std::string& topic, int qos) {
    if (!mosq_ || !connected_) {
        return false;
    }

    int result = mosquitto_subscribe(mosq_, nullptr, topic.c_str(), qos);
    if (result != MOSQ_ERR_SUCCESS) {
        std::cerr << "Failed to subscribe to topic " << topic << ": " << mosquitto_strerror(result) << std::endl;
        return false;
    }

    return true;
}

bool MqttClient::unsubscribe(const std::string& topic) {
    if (!mosq_ || !connected_) {
        return false;
    }

    int result = mosquitto_unsubscribe(mosq_, nullptr, topic.c_str());
    if (result != MOSQ_ERR_SUCCESS) {
        std::cerr << "Failed to unsubscribe from topic " << topic << ": " << mosquitto_strerror(result) << std::endl;
        return false;
    }

    return true;
}

bool MqttClient::publish(const std::string& topic, const std::string& message, int qos) {
    if (!mosq_ || !connected_) {
        return false;
    }

    int result = mosquitto_publish(mosq_, nullptr, topic.c_str(), 
                                  static_cast<int>(message.length()), 
                                  message.c_str(), qos, false);
    if (result != MOSQ_ERR_SUCCESS) {
        std::cerr << "Failed to publish to topic " << topic << ": " << mosquitto_strerror(result) << std::endl;
        return false;
    }

    return true;
}

void MqttClient::loop(int timeout) {
    if (mosq_) {
        mosquitto_loop(mosq_, timeout, 1);
    }
}

void MqttClient::loopStart() {
    if (mosq_) {
        mosquitto_loop_start(mosq_);
    }
}

void MqttClient::loopStop() {
    if (mosq_) {
        mosquitto_loop_stop(mosq_, true);
    }
}

// Static callback methods
void MqttClient::onConnect(struct mosquitto* mosq, void* userdata, int result) {
    MqttClient* client = static_cast<MqttClient*>(userdata);
    
    if (result == 0) {
        client->connected_ = true;
        std::cout << "Connected to MQTT broker" << std::endl;
        if (client->connectCallback_) {
            client->connectCallback_();
        }
    } else {
        std::cerr << "Failed to connect to MQTT broker: " << mosquitto_strerror(result) << std::endl;
    }
}

void MqttClient::onDisconnect(struct mosquitto* mosq, void* userdata, int result) {
    MqttClient* client = static_cast<MqttClient*>(userdata);
    client->connected_ = false;
    std::cout << "Disconnected from MQTT broker" << std::endl;
    if (client->disconnectCallback_) {
        client->disconnectCallback_();
    }
}

void MqttClient::onMessage(struct mosquitto* mosq, void* userdata, 
                          const struct mosquitto_message* message) {
    MqttClient* client = static_cast<MqttClient*>(userdata);
    
    if (client->messageCallback_) {
        std::string topic(message->topic);
        std::string payload(static_cast<const char*>(message->payload), message->payloadlen);
        
        // Try to parse as JSON Action
        try {
            auto json_action = nlohmann::json::parse(payload);
            
            // Create Action struct from JSON
            struct Action {
                std::string remote_id;
                std::string command;
                int repeat = 1;
            } action;
            
            if (json_action.contains("remote_id")) {
                action.remote_id = json_action["remote_id"];
            }
            if (json_action.contains("command")) {
                action.command = json_action["command"];
            }
            if (json_action.contains("repeat")) {
                action.repeat = json_action["repeat"];
            }
            
            // Call message callback with parsed action
            client->messageCallback_(topic, action);
        } catch (const std::exception& e) {
            std::cerr << "Failed to parse message as JSON Action: " << e.what() << std::endl;
        }
    }
}

void MqttClient::onSubscribe(struct mosquitto* mosq, void* userdata, int mid, 
                            int qos_count, const int* granted_qos) {
    std::cout << "Subscribed to MQTT topic" << std::endl;
}

void MqttClient::onPublish(struct mosquitto* mosq, void* userdata, int mid) {
    // Optional: handle publish confirmation
}

} // namespace irremote
