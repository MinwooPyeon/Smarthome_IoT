#pragma once

#include <string>
#include <functional>
#include <memory>
#include <atomic>
#include <vector>

#ifdef ESP32
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#else
#include <thread>
#endif

class PIRSensor {
public:
    using IRSignalCallback = std::function<void(const std::string&, int)>;

    using StatusCallback = std::function<void(bool, const std::string&)>;

    PIRSensor();
    ~PIRSensor();

    PIRSensor(const PIRSensor&) = delete;
    PIRSensor& operator=(const PIRSensor&) = delete;

    PIRSensor(PIRSensor&&) noexcept;
    PIRSensor& operator=(PIRSensor&&) noexcept;

    bool initialize(int gpio_pin, float sensitivity = 0.5);

    void cleanup();

    void setIRSignalCallback(IRSignalCallback callback) { ir_signal_callback_ = callback; }

    void setStatusCallback(StatusCallback callback) { status_callback_ = callback; }

    void enable();

    void disable();

    bool isEnabled() const { return enabled_; }

    std::string getLastIRSignal() const { return last_ir_signal_; }

    int getDetectedCount() const { return detected_count_; }

    void setSensitivity(float sensitivity);

    float getSensitivity() const { return sensitivity_; }

public:
    void monitoringThread();

private:

    std::string analyzeIRPattern(const std::vector<int>& signal_data);

    std::vector<int> filterNoise(const std::vector<int>& signal_data);

    int gpio_pin_;
    float sensitivity_;
    std::atomic<bool> enabled_;
    std::atomic<bool> running_;

    IRSignalCallback ir_signal_callback_;
    StatusCallback status_callback_;

#ifdef ESP32
    TaskHandle_t monitoring_task_handle_;
#else
    std::unique_ptr<std::thread> monitoring_thread_;
#endif

    std::string last_ir_signal_;
    std::atomic<int> detected_count_;

    std::vector<int> signal_buffer_;
    int signal_threshold_;
    int debounce_time_ms_;
};
