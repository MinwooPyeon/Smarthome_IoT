// mqtt_manager.hpp
#pragma once
#include <unordered_map>
#include <functional>
#include <atomic>
#include <thread>
#include <nlohmann/json.hpp>

#include "config.hpp"
#include "mqtt/mqtt_client.hpp"
#include "actuator/dht11_reader.hpp"
#include "actuator/ir_receiver.hpp"
#include "analyzer.hpp"
#include "types.hpp"

#include "ir_device_manager.hpp"
#include "log_manager.hpp"
#include "env_manager.hpp"

namespace mqtt
{

    class MqttManager
    {
    public:
        explicit MqttManager(const AppConfig &cfg);
        ~MqttManager();
        bool start();
        void stop();

    private:
        AppConfig cfg_;
        MqttClient mqtt_;
        Analyzer az_;
        Dht11Reader dht_;
        IrDeviceManager irMgr_;
        LogManager logMgr_;
        EnvManager envMgr_;

        std::atomic<bool> running_{false};
        std::thread loopThread_;

        // topic → handler(json)
        std::unordered_map<std::string, std::function<void(const nlohmann::json &)>> handlers_;

        void setup_handlers(); // 라우터 등록
        void on_mqtt_message(const std::string &topic, const std::string &payload);

        // 개별 핸들러(라우터가 호출)
        void h_env_request(const nlohmann::json &j); // env start/stop
        void h_ir_req(const nlohmann::json &j);      // ir 등록
        void h_control(const nlohmann::json &j);     // 제어 로그
        void h_regist_send(const nlohmann::json &j); // ir device 등록/제거

        // 주기 작업
        void run_loop();

        // 공용 유틸
        void publish_env(double T, double RH, int64_t ts);
        void publish_error(int tx_id, const std::string &reason);
    };
}