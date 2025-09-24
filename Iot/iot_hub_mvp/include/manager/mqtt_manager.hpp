#pragma once
#include <unordered_map>
#include <functional>
#include <atomic>
#include <thread>
#include <nlohmann/json.hpp>

#include "config.hpp"
#include "mqtt/mqtt_client.hpp"
#include "mqtt/mqtt_handler.hpp"

#include "actuator/dht11_reader.hpp"
#include "actuator/ir_receiver.hpp"

#include "analyzer.hpp"
#include "types.hpp"

#include "app/dispatcher.hpp"
#include "manager/data_manager.hpp"
#include "manager/csv_manager.hpp"

namespace manager
{

    class MqttManager
    {
    public:
        explicit MqttManager(const AppConfig &cfg);
        ~MqttManager();

        bool start();
        void stop();

    private:
        using json = nlohmann::json;

        // config & deps
        AppConfig cfg_;
        mqtt::MqttClient mqtt_;
        Analyzer az_;
        Dht11Reader dht_;

        // event infra
        app::Dispatcher bus_;
        manager::DataManager dataMgr_;
        manager::CsvManager csvMgr_;

        // runtime
        std::atomic<bool> running_{false};
        std::thread loopThread_;

        // topic → handler(json)
        std::unordered_map<std::string, std::function<void(const json &)>> handlers_;

    private:
        // router
        void setup_handlers();
        void on_mqtt_message(const std::string &topic, const std::string &payload);

        // handlers
        void h_env_request(const json &j); // env start/stop toggle
        void h_ir_req(const json &j);      // IR capture request
        void h_regist_send(const json &j); // dynamic subscribe/unsubscribe

        // loop
        void run_loop();

        // utils
        void publish_error(int tx_id, const std::string &reason);
    };

}
