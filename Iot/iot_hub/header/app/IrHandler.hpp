// IrHandler.hpp
#pragma once
#include "sensors/IrReceiverSensor.hpp"
#include "MqttHelper.hpp"

class IrHandler {
public:
    IrHandler(std::shared_ptr<sensors::IrReceiverSensor> sensor, MqttHelper& mqtt);
private:
    std::shared_ptr<sensors::IrReceiverSensor> sensor_;
    MqttHelper& mqtt_;
};
