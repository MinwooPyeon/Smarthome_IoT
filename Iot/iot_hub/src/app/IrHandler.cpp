// IrHandler.cpp
#include "app/IrHandler.hpp"
using namespace sensors;

IrHandler::IrHandler(std::shared_ptr<IrReceiverSensor> sensor, MqttHelper& mqtt)
: sensor_(std::move(sensor)), mqtt_(mqtt) {
    sensor_->setReadingCallback([this](const SensorReading& r){
        mqtt_.publishIr(r);
    });
}
