// src/sensors/sensorManager.cpp
#include "sensor/sensorManager.hpp"
#include <shared_mutex>

using namespace sensors;

bool sensorManager::registerSensor(const ISensorPtr& sensor) {
    if (!sensor) return false;
    const auto& name = sensor->config().name;
    if (name.empty()) return false;

    std::unique_lock lk(mutex_);
    return map_.emplace(name, sensor).second;
}

bool sensorManager::unregisterSensor(const std::string& name) {
    std::unique_lock lk(mutex_);
    return map_.erase(name) > 0;
}

ISensorPtr sensorManager::get(const std::string& name) const {
    std::shared_lock lk(mutex_);
    auto it = map_.find(name);
    return (it != map_.end()) ? it->second : nullptr;
}

bool sensorManager::initializeAll(std::vector<Error>* errors) {
    bool ok = true;
    std::shared_lock lk(mutex_);
    for (auto& [_, s] : map_) {
        Error e{};
        if (!s->initialize(&e)) {
            ok = false;
            if (errors) errors->push_back(e);
        }
    }
    return ok;
}

bool sensorManager::startAll(std::vector<Error>* errors) {
    bool ok = true;
    std::shared_lock lk(mutex_);
    for (auto& [_, s] : map_) {
        Error e{};
        if (!s->start(&e)) {
            ok = false;
            if (errors) errors->push_back(e);
        }
    }
    return ok;
}

void sensorManager::stopAll() {
    std::shared_lock lk(mutex_);
    for (auto& [_, s] : map_) s->stop();
}

void sensorManager::setReadingCallbackForAll(iSensor::ReadingCallback cb) {
    std::shared_lock lk(mutex_);
    for (auto& [_, s] : map_) s->setReadingCallback(cb);
}

void sensorManager::setErrorCallbackForAll(iSensor::ErrorCallback cb) {
    std::shared_lock lk(mutex_);
    for (auto& [_, s] : map_) s->setErrorCallback(cb);
}
