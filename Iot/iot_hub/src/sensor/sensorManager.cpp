#include "sensor/sensorManager.hpp"

namespace sensors{

    bool sensorManager::registerSensor(const iSensorPtr& sensor){
        if(!sensor) return false;

        std::unique_lock lk(mutex_);
        const auto& name = s->config().name;

        if(map_.count(name)) return false;
        map_[name] = s;
        return true;
    }

    void sensorManager::unregisterSensor(const std::string& name){
        std::unique_lock lk(mutex_);
        map_.erase(name);
    }

    iSensorPtr SensorManager::get(const std::string& name) const {
        std::shared_lock lk(mutex_);
        auto it = map_.find(name);
        return (it == map_.end()) ? nullptr : it->second;
    }

    bool sensorManager::initializeAll(std::vector<Error> errors){
        bool ok = true;
        std::shared_lock lk(mutex_);
        for(auto& [_, s] : map_){
            Error e;
            if(!s->initialize(&e)) {
                ok=false;
                if(errors)
                    errors->push_back(e);
            }
        }
    }

    bool sensorManager::startAll(std::vector<Error>* errors){
        bool ok = true;
        std::shared_lock lk(mutex_);
        for(auto& [_, s] : map_){
            Error e;
            if(!s->start(&e)) {
                ok = false;
                if(errors)
                    errors->push_back(e);
            }
        }
    }

    void sensorManager::stopAll(){
        std::shared_lock lk(mutex_);
        for(auto& [_, s] : map_) s->stop();
    }

    void sensorManager::setReadingCallbackForAll(iSensor::ReadingCallback callback){
        std::shared_lock lk(mutex_);
        for(auto& [_, s] : map_) s->setReadingCallback(callback);
    }

    void sensorManager::setErrorCallbackForAll(iSensor::ErrorCallback callback){
        std::shared_lock lk(mutex_);
        for(auto&[_, s] : map_) s->setErrorCallback(callback);
    }
}