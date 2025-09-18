// mqtt_manager.hpp
#pragma once
#include <unordered_map>
#include <functional>
#include <atomic>
#include <thread>
#include <nlohmann/json.hpp>
#include "config.hpp"
#include "mqtt_client.hpp"
#include "dht11_reader.hpp"
#include "ir_receiver.hpp"
#include "analyzer.hpp"   // 계산 로직 사용 (이슬점/체감/절대습도/WBGT/PMV·PPD) :contentReference[oaicite:0]{index=0}
#include "types.hpp"      // Metrics/EnvSample 정의 :contentReference[oaicite:1]{index=1}

class MqttManager {
public:
    explicit MqttManager(const AppConfig& cfg);
    ~MqttManager();
    bool start();
    void stop();

private:
    AppConfig  cfg_;
    MqttClient mqtt_;
    Analyzer   az_;
    Dht11Reader dht_;

    std::atomic<bool> running_{false};
    std::thread       loopThread_;

    // topic → handler(json)
    std::unordered_map<std::string, std::function<void(const nlohmann::json&)>> handlers_;

    void setup_handlers();                     // 라우터 등록
    void on_mqtt_message(const std::string& topic, const std::string& payload);

    // 개별 핸들러(라우터가 호출)
    void h_env_request(const nlohmann::json& j);     // env start/stop
    void h_ir_req(const nlohmann::json& j);          // ir 등록
    void h_control(const nlohmann::json& j);         // 제어

    // 주기 작업
    void run_loop();

    // 공용 유틸
    void publish_env(double T, double RH, int64_t ts);
    void publish_error(int tx_id, const std::string& reason);
};
