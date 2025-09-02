#include "mqtt/mqttSubscriber.hpp"
#include "util/mqttUtil.hpp"
#include <atomic>
#include <csignal>
#include <iostream>

using namespace mqtt;

static std::atomic<bool> g_run{true};
void on_sigint(int){ g_run.store(false); }

static std::string to_string_payload(const std::vector<uint8_t>& v) {
    return std::string(v.begin(), v.end());
}

int main() {
    std::signal(SIGINT, on_sigint);

    // 1) 설정
    mqttConfig cfg;
    cfg.brokerAddr = "192.168.0.10";
    cfg.port = 1883;
    cfg.clientId = "rpi-sub-demo";
    cfg.keepaliveSec = 30;
    cfg.cleanSession = true;

    // 2) 구독 목록
    std::vector<subSpec> topics = {
        {"control/ir/#",      1},
        {"control/reboot",    1},
        {"config/+/update",   1},
    };

    mqttSubscriber sub(cfg);
    sub.set_topics(topics);

    sub.set_on_connect([](){
        std::cout << "[MQTT] connected & (re)subscribed\n";
    });
    sub.set_on_disconnect([](int rc){
        std::cout << "[MQTT] disconnected rc=" << rc << "\n";
    });
    sub.set_on_error([](const std::string& e){
        std::cerr << "[MQTT] error " << e << "\n";
    });

    // 3) 라우팅 등록 (의사코드의 예시 세 가지)
    sub.add_route("control/ir/send", [](const inMsg& m){
        // TODO: JSON 파싱 → IR 송신(S346)
        std::cout << "[IR] send request: " << to_string_payload(m.payload) << "\n";
    });

    sub.add_route("control/reboot", [](const inMsg&){
        // TODO: 안전한 재부팅 시퀀스 호출
        std::cout << "[SYS] reboot command received\n";
    });

    sub.add_route("config/+/update", [](const inMsg& m){
        // 예: config/device123/update → topic에서 대상 추출
        std::cout << "[CFG] update: topic=" << m.topic
                  << " payload=" << to_string_payload(m.payload) << "\n";
    });

    sub.start();

    // 메인 쓰레드는 단순 대기
    while (g_run.load()) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }

    sub.stop();
    return 0;
}
