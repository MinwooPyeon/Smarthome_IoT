#include "sensors/sensorManager.hpp"

namespace sensors {

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
    return (it == map_.end()) ? nullptr : it->second;
}

bool sensorManager::initializeAll(std::vector<Error>* errors) {
    bool ok = true;
    std::shared_lock lk(mutex_);
    for (auto& kv : map_) {
        Error e;
        if (!kv.second->initialize(&e)) {
            ok = false;
            if (errors) errors->push_back(e.code || e.message.size() ? e : Error{1, "initialize failed: " + kv.first});
        }
    }
    return ok;
}

bool sensorManager::startAll(std::vector<Error>* errors) {
    bool ok = true;
    std::shared_lock lk(mutex_);
    for (auto& kv : map_) {
        Error e;
        if (!kv.second->start(&e)) {
            ok = false;
            if (errors) errors->push_back(e.code || e.message.size() ? e : Error{1, "start failed: " + kv.first});
        }
    }
    return ok;
}

void sensorManager::stopAll() {
    std::shared_lock lk(mutex_);
    for (auto& kv : map_) kv.second->stop();
}

void sensorManager::setReadingCallbackForAll(iSensor::ReadingCallback callback) {
    std::shared_lock lk(mutex_);
    for (auto& kv : map_) kv.second->setReadingCallback(callback);
}

void sensorManager::setErrorCallbackForAll(iSensor::ErrorCallback callback) {
    std::shared_lock lk(mutex_);
    for (auto& kv : map_) kv.second->setErrorCallback(callback);
}

} // namespace sensors
