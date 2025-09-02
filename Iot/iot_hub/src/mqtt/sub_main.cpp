// File: src/mqtt/sub_main.cpp
#include "mqtt/mqttSubscriber.hpp"
#include <iostream>
#include <thread>
#include <chrono>
#include <csignal>
#include <atomic>
#include <mosquittopp.h>

static std::atomic<bool> g_running{true};
void on_sigint(int){ g_running = false; }

int main() {
    std::signal(SIGINT, on_sigint);
    std::signal(SIGTERM, on_sigint);

    mosqpp::lib_init();  // 전역 초기화

    // 원하는 토픽(여러 개 가능, 와일드카드도 O)
    std::vector<std::string> topics = {
        "iothub/sensor/temperature",
        "iothub/sensor/temperature/state"
        // "iothub/sensor/#"
    };

    MqttSubscriber sub(
        "localhost",     // host
        1883,            // port
        "sensor-sub",    // clientId
        topics,
        1,               // qos
        60               // keepalive
    );

    sub.connect();

    // Ctrl+C까지 대기
    while (g_running.load()) {
        std::this_thread::sleep_for(std::chrono::milliseconds(200));
    }

    sub.disconnect();

    mosqpp::lib_cleanup(); // 전역 정리
    return 0;
}
