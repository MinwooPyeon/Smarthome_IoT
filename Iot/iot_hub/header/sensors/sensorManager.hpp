#pragma once
#include "sensors/iSensor.hpp"
#include <shared_mutex>
#include <string>
#include <unordered_map>
#include <vector>
namespace sensors { 
    class sensorManager { 
        public: 
        // sensor->config().name 을 키로 등록 
            bool registerSensor(const ISensorPtr& sensor);
            bool unregisterSensor(const std::string& name);
            ISensorPtr get(const std::string& name) const;
            bool initializeAll(std::vector<Error>* errors = nullptr);
            bool startAll(std::vector<Error>* errors = nullptr);
            void stopAll();
            void setReadingCallbackForAll(iSensor::ReadingCallback callback);
            void setErrorCallbackForAll(iSensor::ErrorCallback callback);
        private:
            mutable std::shared_mutex mutex_;
            std::unordered_map<std::string, ISensorPtr> map_; 
    }; 
} // namespace sensors