#pragma once

#include "core/ir_system.h"
#include <atomic>
#include <thread>
#include <mutex>
#include <functional>

class ESP32IRReceiver {
public:
    ESP32IRReceiver(int gpio_pin = 22);
    
    ~ESP32IRReceiver();
    
    bool startReceiving();
    void stopReceiving();
    bool isReceiving() const;
    
    void setIRCodeCallback(std::function<void(const std::string&)> callback);
    
    void setGPIO(int gpio_pin);
    int getGPIO() const;
    
    void setProtocol(const std::string& protocol);
    void setDebugMode(bool enabled);
    
    struct Statistics {
        size_t total_received;
        size_t valid_codes;
        size_t invalid_codes;
        std::string last_received_code;
        double last_receive_time;
    };
    
    Statistics getStatistics() const;

private:
    int gpio_pin_;
    std::string protocol_;
    std::atomic<bool> is_receiving_;
    std::atomic<bool> debug_mode_;
    std::atomic<bool> initialized_;
    
    std::thread receive_thread_;
    std::function<void(const std::string&)> ir_code_callback_;
    
    mutable std::mutex stats_mutex_;
    Statistics stats_;
    
    bool initializeHardware();
    void cleanupHardware();
    
    void receiveLoop();
    std::string readIRCode();
    
    std::string decodeNECProtocol();
    std::string decodeRC5Protocol();
    std::string decodeSonyProtocol();
    
    void setGPIOHigh();
    void setGPIOLow();
    int readGPIO();
    void delayMicroseconds(int us);
    
    void updateStatistics(const std::string& code, bool valid);
    void logMessage(const std::string& message);
};
