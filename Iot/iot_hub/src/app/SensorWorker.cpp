// SensorWorker.cpp
#include "app/SensorWorker.hpp"
using namespace sensors;

void SensorWorker::start() {
    running_ = true;
    th_ = std::thread(&SensorWorker::loop, this);
}
void SensorWorker::stop() {
    running_ = false;
    if (th_.joinable()) th_.join();
}
void SensorWorker::loop() {
    while (running_) {
        Error e{};
        if (auto rd = sensor_->readOnce(&e)) {
            mqtt_.publishEnv(*rd, intervalMs_);
        } else if (!e.message.empty()) {
            mqtt_.publishError(1, "DHT11 readOnce: " + e.message);
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(intervalMs_));
    }
}
