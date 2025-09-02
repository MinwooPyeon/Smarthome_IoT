#ifndef MQTT_MESSAGE_H
#define MQTT_MESSAGE_H

#include <string>
#include <nlohmann/json.hpp>

struct MQTTMessage {
    std::string topic;
    std::string payload;
    int qos;
    
    MQTTMessage(const std::string& t = "", const std::string& p = "", int q = 1)
        : topic(t), payload(p), qos(q) {}
};

struct IRCommandMessage {
    std::string device_id;
    std::string command;
    std::string remote_id;
    
    nlohmann::json toJson() const {
        nlohmann::json j;
        j["device_id"] = device_id;
        j["command"] = command;
        j["remote_id"] = remote_id;
        return j;
    }
    
    static IRCommandMessage fromJson(const nlohmann::json& j) {
        IRCommandMessage msg;
        msg.device_id = j.value("device_id", "");
        msg.command = j.value("command", "");
        msg.remote_id = j.value("remote_id", "");
        return msg;
    }
};

struct IRSignalMessage {
    std::string device_id;
    std::string remote_id;
    std::string command;
    std::string code;
    bool success;
    
    nlohmann::json toJson() const {
        nlohmann::json j;
        j["device_id"] = device_id;
        j["remote_id"] = remote_id;
        j["command"] = command;
        j["code"] = code;
        j["success"] = success;
        return j;
    }
    
    static IRSignalMessage fromJson(const nlohmann::json& j) {
        IRSignalMessage msg;
        msg.device_id = j.value("device_id", "");
        msg.remote_id = j.value("remote_id", "");
        msg.command = j.value("command", "");
        msg.code = j.value("code", "");
        msg.success = j.value("success", false);
        return msg;
    }
};

#endif // MQTT_MESSAGE_H
