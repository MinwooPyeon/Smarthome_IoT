#include "mqtt/mqttPublisher.hpp"
#include "util/mqttUtil.hpp"
#include <atomic>
#include <csignal>
#include <iostream>

using namespace mqtt;

static std::atomic<bool> g_run{true};
void on_sigint(int){ g_run.store(false); }

int main() {
    std::signal(SIGINT, on_sigint);

    mqttConfig cfg;
    cfg.brokerAddr = "192.168.0.10";
    cfg.port = 1883;
    cfg.clientId = "rpi-pub-demo";
    cfg.keepaliveSec = 30;
    cfg.cleanSession = true;

    // (선택) 인증 & LWT
    // cfg.username = "user";
    // cfg.password = "pass";
    // cfg.lwt_topic = "status/rpi";
    // cfg.lwt_payload = "offline";
    // cfg.lwt_qos = 1; cfg.lwt_retain = true;

    // ACK 타임아웃/재시도 커스터마이즈 가능
    cfg.backoffInit = std::chrono::milliseconds(5000);
    cfg.maxRetries = 3;

    mqttPublisher pub(cfg);
    pub.set_on_connect([](){ std::cout << "[MQTT] connected\n"; });
    pub.set_on_disconnect([](int rc){ std::cout << "[MQTT] disconnected rc=" << rc << "\n"; });
    pub.set_on_puback([](int mid){ std::cout << "[MQTT] ack mid=" << mid << "\n"; });
    pub.set_on_error([](const std::string& e){ std::cerr << "[MQTT] error " << e << "\n"; });

    pub.start();

    int i = 0;
    while (g_run.load()) {
        // sensors/dht11
        pubMsg m1;
        m1.topic = "sensors/dht11";
        const std::string payload1 = std::string("{\"t\":") + std::to_string(24 + (i%3)) + ",\"h\":55}";
        m1.payload.assign(payload1.begin(), payload1.end());
        m1.qos = 1; m1.retain = false;
        pub.publish(std::move(m1));

        // events/pir
        pubMsg m2;
        m2.topic = "events/pir";
        const std::string payload2 = std::string("{\"motion\":") + ((i%5)==0 ? "true" : "false") + "}";
        m2.payload.assign(payload2.begin(), payload2.end());
        m2.qos = 1; m2.retain = false;
        pub.publish(std::move(m2));

        std::this_thread::sleep_for(std::chrono::seconds(2));
        ++i;
    }

    pub.stop();
    return 0;
}
