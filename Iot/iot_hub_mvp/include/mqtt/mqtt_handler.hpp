#pragma once
#include <string>
#include <nlohmann/json.hpp>
#include "types.hpp"
#include "manager/csv_manager.hpp"
#include "manager/data_manager.hpp"
#include "mqtt/mqtt_client.hpp"

namespace mqtt{
    struct Deps {
        manager::IDataStore* dataMgr;  
        manager::IEventSink*  csvMgr;   
        mqtt::MqttClient*     mqtt;     
        AppConfig*      cfg;   
    };

    class MqttHandler{
    public:
        explicit MqttHandler(Deps d) : d_(d) {}
        
        void on_env_require(const bool streaming);
        void on_metrics(const Metrics& met);

        void on_ir_control(const IrSignalLog& log);
        void on_ir_regist(const IrSendDevice& device);
        void on_ir_capture(const IrSignal& signal);
    private:
        Deps d_;
        
        using json = nlohmann::json;

        void publish_env_if_enabled(const Metrics& m);
    };
}