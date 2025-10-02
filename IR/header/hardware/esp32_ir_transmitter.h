#pragma once

#include "core/ir_system.h"
#include <atomic>
#include <thread>
#include <mutex>

class ESP32IRTransmitter : public IRSystem::ITransmitter {
public:
    ESP32IRTransmitter(int gpio_pin = 23, int carrier_freq = 38000);
    
    ~ESP32IRTransmitter();
    
    bool transmit(const std::string& ir_code, int repeat_count = 1) override;
    bool isTransmitting() const override;
    bool waitForCompletion(int timeout_ms = 1000) override;
    
    void setGPIO(int gpio_pin);
    
    void setCarrierFrequency(int frequency);
    
    void setProtocol(const std::string& protocol);
    
    void setDebugMode(bool enabled);
    
    struct Statistics {
        size_t total_transmissions;
        size_t successful_transmissions;
        size_t failed_transmissions;
        double average_duration_ms;
        std::string last_transmitted_code;
    };
    
    Statistics getStatistics() const;

private:
    int gpio_pin_;
    int carrier_frequency_;
    std::string protocol_;
    
    std::atomic<bool> is_transmitting_;
    std::atomic<bool> debug_mode_;
    std::atomic<bool> initialized_;
    
    std::thread transmission_thread_;
    mutable std::mutex transmission_mutex_;
    
    mutable std::mutex stats_mutex_;
    Statistics stats_;
    
    bool initializeHardware();
    void cleanupHardware();
    
    void transmissionLoop();
    
    bool sendIRSignal(const std::string& ir_code, int repeat_count);
    
    bool sendNECProtocol(const std::string& ir_code, int repeat_count);
    
    bool sendRC5Protocol(const std::string& ir_code, int repeat_count);
    
    bool sendSonyProtocol(const std::string& ir_code, int repeat_count);
    
    void setGPIOHigh();
    void setGPIOLow();
    void delayMicroseconds(int us);
    
    void updateStatistics(bool success, double duration_ms, const std::string& code);
    
    void logMessage(const std::string& message);
};
