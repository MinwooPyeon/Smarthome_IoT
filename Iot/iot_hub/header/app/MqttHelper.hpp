// header/app/MqttHelper.hpp
#pragma once
#include <string>
#include <vector>
#include "mqtt/mqttPublisher.hpp"
#include "util/sensorTypes.hpp"

class MqttHelper {
public:
    MqttHelper(mqtt::mqttPublisher& pub, std::string deviceId)
    : pub_(pub), deviceId_(std::move(deviceId)) {}

    void publishState(const std::string& status);
    void publishError(int code, const std::string& detail);
    void publishEnv(const sensors::SensorReading& r, int intervalMs);
    void publishIr(const sensors::SensorReading& r);

private:
    // 문자열(payload)을 raw bytes로 바꾸는 유틸
    static std::vector<uint8_t> toBytes(const std::string& s) {
        return std::vector<uint8_t>(s.begin(), s.end());
    }

    // pubMsg 생성 헬퍼 (문자열 payload 버전)
    static mqtt::pubMsg makeMsg(const std::string& topic,
                                const std::string& payloadUtf8,
                                int qos,
                                bool retain) {
        mqtt::pubMsg m{};
        m.topic   = topic;
        m.payload = toBytes(payloadUtf8);
        m.qos     = qos;
        m.retain  = retain;
        return m;
    }

    mqtt::mqttPublisher& pub_;
    std::string deviceId_;
};
