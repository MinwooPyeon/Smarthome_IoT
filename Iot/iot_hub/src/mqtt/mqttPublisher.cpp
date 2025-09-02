// File: src/mqtt/mqttPublisher.cpp
#include "mqtt/mqttPublisher.hpp"
#include <iostream>
#include <thread>
#include <chrono>
#include <cstring>   // std::strlen

MqttPublisher::MqttPublisher(const std::string& host,
                             int                port,
                             const std::string& clientId,
                             const std::string& topicBase,
                             int qos, bool retained, int keepalive)
    : mosqpp::mosquittopp(clientId.c_str())
    , host_(host)
    , port_(port)
    , topicBase_(topicBase)
    , qos_(qos)
    , retained_(retained)
    , keepalive_(keepalive)
{
    // 자동 재연결 딜레이 (1~30초, 지수백오프)
    reconnect_delay_set(1 /*sec*/, 30 /*sec*/, true /*exponential*/);

    // LWT 설정: "offline"
    const std::string will_topic = topicBase_ + "/state";
    const char* will_payload = "offline";
    will_set(will_topic.c_str(),
             static_cast<int>(std::strlen(will_payload)),
             will_payload,
             1, /* qos */
             true /* retain */);
}

MqttPublisher::~MqttPublisher() {
    try { disconnect(); } catch (...) {}
}

void MqttPublisher::connect() {
    // 비동기 연결 + loop 스레드 시작
    int rc = connect_async(host_.c_str(), port_, keepalive_);
    if (rc != MOSQ_ERR_SUCCESS) {
        std::cerr << "[MQTT] connect_async failed: " << rc << "\n";
    }
    rc = loop_start();
    if (rc != MOSQ_ERR_SUCCESS) {
        std::cerr << "[MQTT] loop_start failed: " << rc << "\n";
    }
    // 연결 완료 후 on_connect()에서 online 발행
}

void MqttPublisher::publish(const json& j) {
    if (!connected_) {
        std::cerr << "[MQTT] not connected, drop message\n";
        return;
    }
    const std::string payload = j.dump();

    int mid = 0;
    int rc = mosqpp::mosquittopp::publish(
        &mid,
        topicBase_.c_str(),
        static_cast<int>(payload.size()),
        payload.data(),
        qos_,
        retained_
    );
    if (rc != MOSQ_ERR_SUCCESS) {
        std::cerr << "[MQTT] publish failed rc=" << rc << " (mid=" << mid << ")\n";
    }
}

void MqttPublisher::disconnect() {
    if (connected_) {
        const std::string offline = "offline";
        mosqpp::mosquittopp::publish(
            nullptr,
            (topicBase_ + "/state").c_str(),
            static_cast<int>(offline.size()),
            offline.data(),
            1, true
        );
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
    }
    // 연결 종료 & 루프 정지
    mosqpp::mosquittopp::disconnect();
    loop_stop(true /* force */);
    connected_ = false;
}

void MqttPublisher::on_connect(int rc) {
    if (rc == 0) {
        connected_ = true;
        std::cout << "[MQTT] connected\n";
        // 온라인 상태 알림
        const std::string online = "online";
        mosqpp::mosquittopp::publish(
            nullptr,
            (topicBase_ + "/state").c_str(),
            static_cast<int>(online.size()),
            online.data(),
            1, true
        );
    } else {
        std::cerr << "[MQTT] connect failed rc=" << rc << "\n";
    }
}

void MqttPublisher::on_disconnect(int rc) {
    connected_ = false;
    std::cerr << "[MQTT] disconnected rc=" << rc << "\n";
}

void MqttPublisher::on_publish(int mid) {
    // 성공 시 콜백
    std::cout << "[MQTT] publish ok (mid=" << mid << ")\n";
}
