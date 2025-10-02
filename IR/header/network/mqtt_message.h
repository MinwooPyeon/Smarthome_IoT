#ifndef MQTT_MESSAGE_H
#define MQTT_MESSAGE_H

#include <string>
#include <ArduinoJson.h>

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

    std::string toJson() const {
        DynamicJsonDocument doc(512);
        doc["device_id"] = device_id.c_str();
        doc["command"] = command.c_str();
        doc["remote_id"] = remote_id.c_str();

        std::string result;
        serializeJson(doc, result);
        return result;
    }

    static IRCommandMessage fromJson(const std::string& json) {
        IRCommandMessage msg;
        DynamicJsonDocument doc(512);
        DeserializationError error = deserializeJson(doc, json);

        if (!error) {
            msg.device_id = doc["device_id"].as<std::string>();
            msg.command = doc["command"].as<std::string>();
            msg.remote_id = doc["remote_id"].as<std::string>();
        }
        return msg;
    }
};

struct IRSignalMessage {
    std::string device_id;
    std::string remote_id;
    std::string command;
    std::string code;
    bool success;

    std::string toJson() const {
        DynamicJsonDocument doc(512);
        doc["device_id"] = device_id.c_str();
        doc["remote_id"] = remote_id.c_str();
        doc["command"] = command.c_str();
        doc["code"] = code.c_str();
        doc["success"] = success;

        std::string result;
        serializeJson(doc, result);
        return result;
    }

    static IRSignalMessage fromJson(const std::string& json) {
        IRSignalMessage msg;
        DynamicJsonDocument doc(512);
        DeserializationError error = deserializeJson(doc, json);

        if (!error) {
            msg.device_id = doc["device_id"].as<std::string>();
            msg.remote_id = doc["remote_id"].as<std::string>();
            msg.command = doc["command"].as<std::string>();
            msg.code = doc["code"].as<std::string>();
            msg.success = doc["success"];
        }
        return msg;
    }
};

#endif
