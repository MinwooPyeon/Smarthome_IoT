#pragma once
#include <atomic>
#include <thread>
#include <memory>
#include "sensors/sensorManager.hpp"
#include "sensors/Dht11Sensor.hpp"
#include "sensors/IrReceiverSensor.hpp"
#include "mqtt/mqttPublisher.hpp"
#include "MqttHelper.hpp"
#include "SensorWorker.hpp"
#include "IrHandler.hpp"

class HubApp{
public:
    HubApp(std::string deviceId, std::string mqttHost, int mqttPort);
    ~HubApp();

    bool init();
    void run();
    void stop();
private:
    std::string deviceId_;
    std::unique_ptr<MqttPublisher> pub_;
    MqttHelper mqttHelper_;
    sensors::sensorManager mgr_;
    std::shared_ptr<sensors::Dht11Sensor> dht_;
    std::shared_ptr<sensors::IrReceiverSensor> ir_;

    std::shared_ptr<SensorWorker> dhtWorker_;
    std::shared_ptr<IrHandler> irHandler_;

    std::atomic<bool> running_{true};
};