#pragma once

#include <string>
#include <map>
#include <vector>
#include <memory>
#include <mutex>
#include <ArduinoJson.h>
#include "nvs_flash.h"
#include "nvs.h"
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"

class ESP32IRStore {
public:
    ESP32IRStore();
    ~ESP32IRStore();

    ESP32IRStore(const ESP32IRStore&) = default;
    ESP32IRStore& operator=(const ESP32IRStore&) = default;

    ESP32IRStore(ESP32IRStore&&) = default;
    ESP32IRStore& operator=(ESP32IRStore&&) = default;

    bool initialize();
    void cleanup();

    bool storeCode(const std::string& device_type, const std::string& action, const std::string& ir_code);
    std::string getCode(const std::string& device_type, const std::string& action) const;

    std::vector<std::string> getDeviceTypes() const;
    std::vector<std::string> getActions(const std::string& device_type) const;

    bool loadFromNVS();
    bool saveToNVS() const;

    void setDefaultCodes();

    bool removeDevice(const std::string& device_type);
    bool removeAction(const std::string& device_type, const std::string& action);

    size_t getCodeCount() const;

    void clear();

    bool isValidCode(const std::string& ir_code) const;

    void printDebugInfo() const;

    bool codeExists(const std::string& ir_code) const;

    std::string findDeviceByCode(const std::string& ir_code) const;
    std::string findActionByCode(const std::string& ir_code) const;

    struct IRCodeInfo {
        std::string device_type;
        std::string action;
        std::string ir_code;
        std::string description;
        std::string protocol;
        uint32_t timestamp;
        int usage_count;
    };
    std::vector<IRCodeInfo> getAllCodes() const;

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
    bool removeControlMapping(const std::string& control_signal);
    void clearAllMappings();
    size_t getMappingCount() const;
    bool loadFromFile(const std::string& filename);
    bool saveToFile(const std::string& filename) const;
    bool loadFromJSON(const std::string& json_str);
    std::string toJSON() const;

private:
    std::map<std::string, std::string> control_to_ir_mapping_;
    std::map<std::string, std::string> ir_to_control_mapping_;
    std::map<std::string, std::string> descriptions_;
    std::map<std::string, std::string> device_types_;

    std::map<std::string, std::map<std::string, std::string>> code_map_;
    std::map<std::string, std::vector<std::string>> action_index_;
    std::map<std::string, std::map<std::string, std::string>> device_actions_;
    std::map<std::string, std::string> action_descriptions_;

    SemaphoreHandle_t store_mutex_;
    nvs_handle_t nvs_handle_;

    static const char* NVS_NAMESPACE;
    static const char* NVS_KEY_IR_CODES;

    void updateActionIndex();
    std::string normalizeIRCode(const std::string& code) const;
    bool parseHexCode(const std::string& code) const;
    bool isValidHexString(const std::string& str) const;

    bool readNVSData();
    bool writeNVSData() const;
    bool readJsonFile(const std::string& filename, std::string& content) const;
    bool writeJsonFile(const std::string& filename, const std::string& content) const;
    std::string serializeToJSON() const;
    bool deserializeFromJSON(const std::string& json);
};
