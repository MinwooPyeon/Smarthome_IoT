// File: src/mqtt/mqttSubscriber.cpp
#include "mqtt/mqttSubscriber.hpp"
#include <iostream>
#include <thread>
#include <chrono>

MqttSubscriber::MqttSubscriber(const std::string& host,
                               int                port,
                               const std::string& clientId,
                               const std::vector<std::string>& topics,
                               int qos,
                               int keepalive)
    : mosqpp::mosquittopp(clientId.c_str())
    , host_(host)
    , port_(port)
    , keepalive_(keepalive)
    , qos_(qos)
    , topics_(topics)
{
    // 자동 재연결 딜레이 설정 (1~30초, 지수백오프)
    reconnect_delay_set(1 /*min*/, 30 /*max*/, true /*exponential*/);

    // clean_session = true(기본)일 경우 재연결마다 on_connect에서 재구독할 것
    // clean_session(false)를 쓰고 브로커에 세션을 유지하게 해도 됨(필요 시):
    // this->opts_set(MOSQ_OPT_PROTOCOL_VERSION, ...); // 등 추가 옵션 가능
}

MqttSubscriber::~MqttSubscriber() {
    try { disconnect(); } catch (...) {}
}

void MqttSubscriber::connect() {
    // 비동기 연결 + 내부 루프 스레드 시작
    int rc = connect_async(host_.c_str(), port_, keepalive_);
    if (rc != MOSQ_ERR_SUCCESS) {
        std::cerr << "[MQTT] connect_async failed: " << rc << "\n";
    }
    rc = loop_start();
    if (rc != MOSQ_ERR_SUCCESS) {
        std::cerr << "[MQTT] loop_start failed: " << rc << "\n";
    }
}

void MqttSubscriber::disconnect() {
    if (connected_) {
        // 구독자는 보낼 메시지가 없으니 바로 끊어도 OK
        mosqpp::mosquittopp::disconnect();
        loop_stop(true);
        connected_ = false;
    } else {
        // 그래도 안전하게 루프 정지 시도
        loop_stop(true);
    }
}

void MqttSubscriber::on_connect(int rc) {
    if (rc == 0) {
        connected_ = true;
        std::cout << "[MQTT] connected, subscribing...\n";
        subscribe_all();
    } else {
        std::cerr << "[MQTT] connect failed rc=" << rc << "\n";
    }
}

void MqttSubscriber::on_disconnect(int rc) {
    connected_ = false;
    std::cerr << "[MQTT] disconnected rc=" << rc << "\n";
}

void MqttSubscriber::on_subscribe(int mid, int qos_count, const int* granted_qos) {
    std::cout << "[MQTT] subscribed (mid=" << mid << ", count=" << qos_count << "): ";
    for (int i = 0; i < qos_count; ++i) std::cout << granted_qos[i] << (i+1==qos_count? "" : ",");
    std::cout << "\n";
}

void MqttSubscriber::on_message(const mosquitto_message* message) {
    if (!message) return;

    const std::string topic(static_cast<const char*>(message->topic));
    const std::string payload(
        static_cast<const char*>(message->payload),
        static_cast<size_t>(message->payloadlen)
    );

    // 로그 출력
    std::cout << "[MQTT] msg topic=" << topic << " payload=" << payload << "\n";

    // JSON 파싱 예시(실패해도 문제없게 try/catch)
    try {
        json j = json::parse(payload);
        if (j.contains("value")) {
            std::cout << "  - value: " << j["value"] << "\n";
        }
    } catch (const std::exception& e) {
        // JSON이 아니면 무시
        std::cerr << "JSON parse error: " << e.what() << "\n";
    }
}

void MqttSubscriber::subscribe_all() {
    for (const auto& t : topics_) {
        int mid = 0;
        int rc = mosqpp::mosquittopp::subscribe(&mid, t.c_str(), qos_);
        if (rc != MOSQ_ERR_SUCCESS) {
            std::cerr << "[MQTT] subscribe failed rc=" << rc << " topic=" << t << "\n";
        }
    }
}
