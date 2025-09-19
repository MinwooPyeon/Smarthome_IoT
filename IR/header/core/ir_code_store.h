#pragma once

#include <string>
#include <map>
#include <vector>
#include <memory>
#include <ArduinoJson.h>

struct ControlMapping {
    std::string control_signal;
    std::string ir_signal;
    std::string description;
    std::string device_type;

    ControlMapping() = default;
    ControlMapping(const std::string& control, const std::string& ir,
                   const std::string& desc = "", const std::string& type = "")
        : control_signal(control), ir_signal(ir), description(desc), device_type(type) {}
};

class IRCodeStore {
public:
    IRCodeStore() = default;
    ~IRCodeStore() = default;

    IRCodeStore(const IRCodeStore&) = default;
    IRCodeStore& operator=(const IRCodeStore&) = default;

    IRCodeStore(IRCodeStore&&) = default;
    IRCodeStore& operator=(IRCodeStore&&) = default;

    void addControlMapping(const std::string& control_signal,
                          const std::string& ir_signal,
                          const std::string& description = "",
                          const std::string& device_type = "");

    std::string getIRSignal(const std::string& control_signal) const;

    std::string getControlSignal(const std::string& ir_signal) const;

    bool hasControlSignal(const std::string& control_signal) const;

    bool hasIRSignal(const std::string& ir_signal) const;

    std::vector<std::string> getAllControlSignals() const;

    std::vector<std::string> getControlSignalsByDevice(const std::string& device_type) const;

    std::vector<std::string> searchControlSignals(const std::string& pattern) const;

    bool loadFromFile(const std::string& filename);

    bool saveToFile(const std::string& filename) const;

    bool loadFromString(const std::string& json_str);

    std::string toJsonString() const;

    size_t getTotalMappingCount() const;

    size_t getMappingCountByDevice(const std::string& device_type) const;

    void clear();

    bool removeControlMapping(const std::string& control_signal);

    size_t removeDeviceMappings(const std::string& device_type);

    void processHubIRSignal(const std::string& control_signal,
                           const std::string& ir_signal,
                           const std::string& device_type = "");

private:
    std::map<std::string, ControlMapping> control_mappings_;

    std::map<std::string, std::string> ir_to_control_map_;

    bool parseJsonObject(const JsonObject& json_obj);
    void addJsonMapping(const std::string& control_signal, const JsonObject& mapping);
};
