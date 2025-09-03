#pragma once
#include "iSensor.hpp"
#include <shared_mutex>
#include <unordered_map>

namespace sensors{

    class sensorManager{
    public:
        bool registerSensor(const ISensorPtr& sensor);
        bool unregisterSensor(const ISensorPtr& name);

        iSensorPtr get(const std::string& name) const;

        bool initializeAll(std::vector<Error>* errors = nullptr);
        bool startAll(std::vector<Error>* errors  = nullptr);
        void stopAll();

        void setReadingCallbackForAll(iSensor::ReadingCallback callback);
        void setErrorCallbackForAll(iSensor::ErrorCallback callback);
    private:
        mutable std::shared_mutex mutex_;
        std::unordered_map<std::string, iSensorPtr> map_;
    };
}