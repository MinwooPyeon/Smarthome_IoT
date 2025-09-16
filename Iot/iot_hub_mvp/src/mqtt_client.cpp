#include "mqtt_client.hpp"
#include <iostream>

bool MqttClient::init(const std::string& clientId, const std::string& host, int port,
                      const std::string& user, const std::string& pass) {
    mosquitto_lib_init();
    m_ = mosquitto_new(clientId.c_str(), true, this);
    if(!m_) return false;
    if(!user.empty()) mosquitto_username_pw_set(m_, user.c_str(), pass.c_str());

    mosquitto_connect_callback_set(m_, &MqttClient::on_connect_cb);
    mosquitto_message_callback_set(m_, &MqttClient::on_message_cb);

    int rc = mosquitto_connect(m_, host.c_str(), port, 60);
    if(rc != MOSQ_ERR_SUCCESS){
        std::cerr << "mosquitto_connect failed: " << mosquitto_strerror(rc) << "\n";
        return false;
    }
    return true;
}

void MqttClient::set_message_handler(MessageHandler h){ handler_ = std::move(h); }

bool MqttClient::subscribe(const std::string& topic, int qos){
    return mosquitto_subscribe(m_, nullptr, topic.c_str(), qos) == MOSQ_ERR_SUCCESS;
}

bool MqttClient::publish(const std::string& topic, const std::string& payload, int qos, bool retain){
    return mosquitto_publish(m_, nullptr, topic.c_str(), (int)payload.size(),
                             payload.data(), qos, retain) == MOSQ_ERR_SUCCESS;
}

void MqttClient::loop_forever(){
    int rc;
    while((rc = mosquitto_loop(m_, -1, 1)) == MOSQ_ERR_SUCCESS) {}
    std::cerr << "mosquitto_loop exited: " << mosquitto_strerror(rc) << "\n";
}
void MqttClient::cleanup(){
    if(m_){ mosquitto_destroy(m_); m_ = nullptr; }
    mosquitto_lib_cleanup();
}

void MqttClient::on_connect_cb(struct mosquitto*, void* obj, int rc){
    std::cout << "MQTT connected rc="<<rc<<"\n";
}

void MqttClient::on_message_cb(struct mosquitto*, void* obj, const struct mosquitto_message* msg){
    auto* self = static_cast<MqttClient*>(obj);
    if(self && self->handler_ && msg && msg->payload){
        std::string topic = msg->topic ? msg->topic : "";
        std::string payload((const char*)msg->payload, (size_t)msg->payloadlen);
        self->handler_(topic, payload);
    }
}
