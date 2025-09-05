// src/app/MqttHelper.cpp
#include "app/MqttHelper.hpp"
#include <algorithm>
#include <nlohmann/json.hpp>

using json = nlohmann::json;
using namespace sensors;

void MqttHelper::publishState(const std::string& status) {
    json st = {{"status", status}, {"ts", now_ms()}};
    auto msg = makeMsg("hub/"+deviceId_+"/state", st.dump(), /*qos=*/1, /*retain=*/true);
    pub_.publish(std::move(msg));
}

void MqttHelper::publishError(int code, const std::string& detail) {
    json err = {{"ts", now_ms()}, {"deviceId", deviceId_},
                {"schema","error/1.0"}, {"level","WARN"},
                {"code", code}, {"detail", detail}};
    auto msg = makeMsg("hub/"+deviceId_+"/error", err.dump(), 1, false);
    pub_.publish(std::move(msg));
}

void MqttHelper::publishEnv(const SensorReading& r, int intervalMs) {
    // values.at(...) 전에 존재 확인 (안전)
    const double temp = (r.values.count("temperature_c") ? r.values.at("temperature_c") : 0.0);
    const double hum  = (r.values.count("humidity_rh")   ? r.values.at("humidity_rh")   : 0.0);

    json env = {
        {"ts", r.ts_ms}, {"deviceId", deviceId_}, {"schema","env/1.1"},
        {"temperature", temp},
        {"humidity",    hum},
        {"units", {{"temperature","C"},{"humidity","%RH"}}},
        {"sampleRateHz", 1000.0 / std::max(1, intervalMs)},
        {"status","ok"},
        {"meta", {{"sensorModel","DHT11"}}}
    };
    auto msg = makeMsg("hub/"+deviceId_+"/env", env.dump(), 1, true);
    pub_.publish(std::move(msg));
}

void MqttHelper::publishIr(const SensorReading& r) {
    json j = {{"ts", r.ts_ms}, {"deviceId", deviceId_},
              {"schema","irsignal/1.0"}, {"encoding","NEC"}};
    if (auto it = r.values.find("nec_address"); it != r.values.end()) j["address"] = (int)it->second;
    if (auto it = r.values.find("nec_command"); it != r.values.end()) j["command"] = (int)it->second;
    if (auto it = r.values.find("raw32"); it != r.values.end())       j["data"]    = (uint32_t)it->second;

    auto msg = makeMsg("hub/"+deviceId_+"/irsignal", j.dump(), 1, false);
    pub_.publish(std::move(msg));
}
