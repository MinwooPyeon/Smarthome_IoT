// SensorWorker.hpp
#pragma once
#include <thread>
#include <atomic>
#include "sensors/Dht11Sensor.hpp"
#include "MqttHelper.hpp"

class SensorWorker {
public:
    SensorWorker(std::shared_ptr<sensors::Dht11Sensor> sensor,
                 int intervalMs, MqttHelper& mqtt)
    : sensor_(std::move(sensor)), intervalMs_(intervalMs), mqtt_(mqtt) {}

    void start();
    void stop();

private:
    void loop();

    std::shared_ptr<sensors::Dht11Sensor> sensor_;
    int intervalMs_;
    MqttHelper& mqtt_;
    std::atomic<bool> running_{false};
    std::thread th_;
};
