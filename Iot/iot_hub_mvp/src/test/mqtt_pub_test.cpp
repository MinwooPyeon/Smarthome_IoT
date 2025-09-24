#include "config.hpp"
#include "mqtt_client.hpp"
#include <nlohmann/json.hpp>
#include <iostream>
#include <algorithm>

using json = nlohmann::json;

static std::string arg_or(char** b, char** e, const std::string& k, const std::string& d){
    auto it = std::find(b, e, k);
    if(it!=e && ++it!=e) return std::string(*it);
    return d;
}

int main(int argc, char** argv){
    AppConfig cfg; // 네 config 그대로 사용
    std::string topic   = arg_or(argv, argv+argc, "-t", "hub/" + cfg.deviceId + "/env");
    std::string payload = arg_or(argv, argv+argc, "-m", "");

    if(payload.empty()){
        auto now = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
        json j = {
            {"ts", now},
            {"deviceId", cfg.deviceId},
            {"msgId", "env-pubtest-" + std::to_string(now)},
            {"schema", "env/1.1"},
            {"temperature", 26.5}, {"humidity", 50.2}, {"gasDensity", 410.0}
        };
        payload = j.dump();
    }

    MqttClient cli;
    if(!cli.init(cfg, "pub_test_" + cfg.deviceId)){
        std::cerr << "init failed\n"; return 1;
    }
    if(!cli.publish(topic, payload, /*qos*/1, /*retain*/false)){
        std::cerr << "publish failed\n"; return 2;
    }
    std::cout << "[pub] " << topic << "\n" << payload << "\n";
    // QoS1 ack 기다리기
    cli.loop_for_ms(1000);
    cli.cleanup();
    return 0;
}
