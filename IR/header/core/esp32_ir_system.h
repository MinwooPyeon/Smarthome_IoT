#pragma once

#include "core/ir_system.h"
#include "core/ir_code_store.h"
#include "hardware/esp32_ir_receiver.h"
#include <memory>
#include <atomic>
#include <thread>
#include <queue>
#include <mutex>
#include <condition_variable>

class ESP32IRSystem : public IRSystem {
public:
    ESP32IRSystem(const std::string& config_file = "config/app_config.json");
    
    ~ESP32IRSystem();
    
    bool initialize() override;
    void cleanup() override;
    ControlResult executeCommand(const Command& command) override;
    std::vector<ControlResult> executeCommands(const std::vector<Command>& commands) override;
    bool isReady() const override;
    std::string getStatus() const override;
    
    void handleMQTTMessage(const std::string& topic, const std::string& message);
    
    void setControlCallback(std::function<void(const ControlResult&)> callback);
    
    void setLogCallback(std::function<void(const std::string&)> callback);
    
    bool start();
    
    void stop();
    
    bool isRunning() const;
    
    size_t getQueueSize() const;
    
    void clearQueue();
    
    struct SystemStatistics {
        size_t total_received;
        size_t valid_codes;
        size_t invalid_codes;
        size_t queued_commands;
        double uptime_seconds;
        std::string last_received_code;
        std::string system_status;
    };
    
    SystemStatistics getSystemStatistics() const;

private:
    std::unique_ptr<IRCodeStore> code_store_;
    std::unique_ptr<ESP32IRReceiver> receiver_;
    std::unique_ptr<IMQTTClient> mqtt_client_;
    
    std::atomic<bool> running_;
    std::atomic<bool> initialized_;
    std::chrono::steady_clock::time_point start_time_;
    
    std::queue<Command> command_queue_;
    mutable std::mutex queue_mutex_;
    std::condition_variable queue_cv_;
    std::thread command_processor_thread_;
    
    std::function<void(const ControlResult&)> control_callback_;
    std::function<void(const std::string&)> log_callback_;
    
    std::string config_file_;
    std::string mqtt_broker_;
    int mqtt_port_;
    std::string mqtt_client_id_;
    std::string mqtt_topic_;
    
    mutable std::mutex stats_mutex_;
    SystemStatistics stats_;
    
    bool loadConfiguration();
    bool initializeComponents();
    void cleanupComponents();
    
    void commandProcessorLoop();
    
    ControlResult processCommand(const Command& command);
    
    void enqueueCommand(const Command& command);
    
    bool dequeueCommand(Command& command);
    
    bool setupMQTT();
    
    bool parseCommandFromJSON(const std::string& json, Command& command);
    
    std::string createResponseJSON(const ControlResult& result) const;
    
    void updateStatistics(const ControlResult& result);
    
    void logMessage(const std::string& message);
    
    void updateSystemStatus();
};
