#include "mqtt/mqtt_client.hpp"
#include <iostream>
#include <chrono>
#include <thread>

bool MqttClient::init(const AppConfig& cfg, const std::string& clientId){
    cfg_ = cfg;
    const char* caf = cfg_.mqttCAFile.empty()
                  ? "/etc/ssl/certs/ca-certificates.crt"
                  : cfg_.mqttCAFile.c_str();

    mosquitto_tls_set(m_, caf, nullptr, nullptr, nullptr, nullptr);
    mosquitto_tls_insecure_set(m_, cfg_.mqttTLSInsecure);
    mosquitto_lib_init();
    m_ = mosquitto_new(clientId.c_str(), true, this);
    if(!m_) return false;

    if(!cfg_.mqttUser.empty())
        mosquitto_username_pw_set(m_, cfg_.mqttUser.c_str(), cfg_.mqttPass.c_str());

    // TLS: 포트가 8883이면 시스템 CA로 TLS 세팅 + 호스트명 검증 생략(개발/테스트 편의)
    if(cfg_.mqttPort == 8883){
        int trc = mosquitto_tls_set(m_, caf, nullptr, nullptr, nullptr, nullptr);
        if(trc != MOSQ_ERR_SUCCESS){
            std::cerr << "[mqtt] tls_set failed: " << mosquitto_strerror(trc)
                      << " (trying to continue)\n";
        }
        // mosquitto_pub에서 --insecure 쓰던 것과 동일 효과(호스트명 검증 off)
        mosquitto_tls_insecure_set(m_, true);
    }

    mosquitto_connect_callback_set(m_, &MqttClient::on_connect_cb);
    mosquitto_message_callback_set(m_, &MqttClient::on_message_cb);

    int rc = mosquitto_connect(m_, cfg_.mqttHost.c_str(), cfg_.mqttPort, 60);
    if(rc != MOSQ_ERR_SUCCESS){
        std::cerr << "[mqtt] connect failed: " << mosquitto_strerror(rc) << "\n";
        return false;
    }

    
    return true;
}

void MqttClient::set_message_handler(MessageHandler h){ handler_ = std::move(h); }

bool MqttClient::subscribe(const std::string& topic, int qos){
    return mosquitto_subscribe(m_, nullptr, topic.c_str(), qos) == MOSQ_ERR_SUCCESS;
}

bool MqttClient::unsubscribe(const std::string &topic)
{
    return mosquitto_unsubscribe(m_, nullptr, topic.c_str());
}

bool MqttClient::publish(const std::string& topic, const std::string& payload, int qos, bool retain){
    return mosquitto_publish(m_, nullptr, topic.c_str(), (int)payload.size(),
                             payload.data(), qos, retain) == MOSQ_ERR_SUCCESS;
}

void MqttClient::loop_forever(){
    int rc;
    while((rc = mosquitto_loop(m_, -1, 1)) == MOSQ_ERR_SUCCESS) {}
    std::cerr << "[mqtt] loop exited: " << mosquitto_strerror(rc) << "\n";
}
void MqttClient::loop_for_ms(int ms){
    auto end = std::chrono::steady_clock::now() + std::chrono::milliseconds(ms);
    while(std::chrono::steady_clock::now() < end){
        mosquitto_loop(m_, 100, 1);
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
}

void MqttClient::cleanup(){
    if(m_){ mosquitto_destroy(m_); m_ = nullptr; }
    mosquitto_lib_cleanup();
}

void MqttClient::on_connect_cb(struct mosquitto*, void*, int rc){
    std::cout << "[mqtt] connected rc=" << rc << "\n";
}
void MqttClient::on_message_cb(struct mosquitto*, void* obj, const struct mosquitto_message* msg){
    auto* self = static_cast<MqttClient*>(obj);
    if(self && self->handler_ && msg && msg->payload){
        std::string topic = msg->topic ? msg->topic : "";
        std::string payload((const char*)msg->payload, (size_t)msg->payloadlen);
        self->handler_(topic, payload);
    }
}
