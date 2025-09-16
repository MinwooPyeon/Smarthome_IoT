#ifndef ESP32_IR_RECEIVER_H
#define ESP32_IR_RECEIVER_H

#include "core/ir_system.h"
#include <atomic>
#include <thread>
#include <mutex>
#include <functional>
#include <vector>
#include <string>
#include "driver/gpio.h"
#include "driver/rmt.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"

class ESP32IRReceiver {
public:
    ESP32IRReceiver(int gpio_pin = 22);

    ~ESP32IRReceiver();

    bool initialize();
    void cleanup();

    bool startReceiving();
    void stopReceiving();
    bool isReceiving() const;

    void setIRCodeCallback(std::function<void(const std::string&)> callback);

    void setGPIO(int gpio_pin);
    int getGPIO() const;

    void setProtocol(const std::string& protocol);
    void setDebugMode(bool enabled);

    // 태스크 함수 (public으로 이동)
    void receiveTask(void* pvParameters);

    struct IRCode {
        std::string code;
        std::string protocol;
        uint32_t timestamp;
        bool valid;
        int signal_strength;
    };

    struct Statistics {
        size_t total_received;
        size_t valid_codes;
        size_t invalid_codes;
        std::string last_received_code;
        double last_receive_time;
    };

    Statistics getStatistics() const;
    std::vector<IRCode> getReceivedCodes() const;

private:
    int gpio_pin_;
    std::string protocol_;
    std::atomic<bool> is_receiving_;
    std::atomic<bool> debug_mode_;
    std::atomic<bool> initialized_;

    TaskHandle_t receive_task_handle_;
    std::function<void(const std::string&)> ir_code_callback_;

    std::vector<IRCode> received_codes_;
    mutable std::mutex stats_mutex_;
    Statistics stats_;

    rmt_channel_t rmt_rx_channel_;
    QueueHandle_t ir_code_queue_;

    bool initializeRMT();
    void cleanupRMT();

    // RMT 콜백 함수
    static void rmt_rx_done_callback(rmt_channel_t channel, rmt_item32_t *item, void *user_data);

    std::string readIRCode();

    std::string decodeNECProtocol();
    std::string decodeRC5Protocol();
    std::string decodeSonyProtocol();

    void updateStatistics(const std::string& code, bool valid);
    bool validateIRCode(const std::string& ir_code) const;
    std::string normalizeIRCode(const std::string& code) const;
    void logMessage(const std::string& message);
};

#endif // ESP32_IR_RECEIVER_H
