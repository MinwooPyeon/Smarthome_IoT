#include "hardware/irsend.h"
#include "core/platform.h"
#include <iostream>
#include <fstream>
#include <chrono>

#ifndef PLATFORM_ESP32
#include <thread>
#endif

#ifdef PLATFORM_ESP32
#include "driver/rmt.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#endif

IRSend::IRSend()
    : initialized_(false), debug_mode_(false), code_store_(nullptr) {

#ifdef PLATFORM_ESP32
#endif
}

IRSend::~IRSend() {
    cleanup();
}

IRSend::IRSend(IRSend&& other) noexcept
    : initialized_(other.initialized_.load()),
      debug_mode_(other.debug_mode_.load()),
      code_store_(other.code_store_),
      stats_(other.stats_),
      last_error_(std::move(other.last_error_)) {

#ifdef PLATFORM_ESP32
#endif

    other.initialized_ = false;
    other.code_store_ = nullptr;
}

IRSend& IRSend::operator=(IRSend&& other) noexcept {
    if (this != &other) {
        cleanup();

        initialized_ = other.initialized_.load();
        debug_mode_ = other.debug_mode_.load();
        code_store_ = other.code_store_;
        stats_ = other.stats_;
        last_error_ = std::move(other.last_error_);

#ifdef PLATFORM_ESP32
#endif

        other.initialized_ = false;
        other.code_store_ = nullptr;
    }
    return *this;
}

bool IRSend::initialize() {
    std::lock_guard<std::mutex> lock(mutex_);

    if (initialized_) {
        return true;
    }

#ifdef PLATFORM_ESP32
    int tx_pin = 25;

    rmt_config_t config = {};
    config.rmt_mode = RMT_MODE_TX;
    config.channel = RMT_CHANNEL_1;
    config.gpio_num = (gpio_num_t)tx_pin;
    config.mem_block_num = 4;
    config.tx_config.loop_en = false;
    config.tx_config.carrier_en = true;
    config.tx_config.carrier_freq_hz = 38000;
    config.tx_config.carrier_duty_percent = 33;
    config.tx_config.carrier_level = RMT_CARRIER_LEVEL_HIGH;
    config.tx_config.idle_level = RMT_IDLE_LEVEL_LOW;
    config.tx_config.idle_output_en = true;

    config.clk_div = 80;

    esp_err_t ret = rmt_config(&config);
    if (ret != ESP_OK) {
        ESP_LOGE("IR_SEND", "RMT 설정 실패: %s", esp_err_to_name(ret));
        return false;
    }

    ret = rmt_driver_install(config.channel, 0, 0);
    if (ret != ESP_OK) {
        ESP_LOGE("IR_SEND", "RMT 드라이버 설치 실패: %s", esp_err_to_name(ret));
        return false;
    }

    ESP_LOGI("IR_SEND", "ESP32 IR 송신기 초기화 완료 - GPIO %d", tx_pin);
#endif

    initialized_ = true;
    return true;
}

void IRSend::cleanup() {
    std::lock_guard<std::mutex> lock(mutex_);

    if (!initialized_) {
        return;
    }

#ifdef PLATFORM_ESP32
    rmt_driver_uninstall(RMT_CHANNEL_1);
#endif

    initialized_ = false;
}

IRSendStatus IRSend::sendControlSignal(const std::string& control_signal) {
    if (!initialized_) {
        return IRSendStatus(IRSendResult::DEVICE_NOT_FOUND, "IR 송신기가 초기화되지 않음");
    }

    if (!validateControlSignal(control_signal)) {
        return IRSendStatus(IRSendResult::INVALID_CODE, "잘못된 제어 신호: " + control_signal);
    }

    auto start_time = std::chrono::high_resolution_clock::now();

    std::string ir_code = convertControlSignalToIRCode(control_signal);
    if (ir_code.empty()) {
        return IRSendStatus(IRSendResult::INVALID_CODE, "IR 코드 변환 실패: " + control_signal);
    }

    IRSendStatus result = sendIRCode(ir_code);

    auto end_time = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::microseconds>(end_time - start_time);
    result.duration_ms = duration.count() / 1000.0;

    updateStatistics(result);
    return result;
}

IRSendStatus IRSend::sendIRCode(const std::string& ir_code) {
    std::lock_guard<std::mutex> lock(mutex_);

    if (!initialized_) {
        return IRSendStatus(IRSendResult::DEVICE_NOT_FOUND, "IR 송신기가 초기화되지 않음");
    }

    try {
        uint64_t code = std::stoull(ir_code.substr(2), nullptr, 16);

#ifdef PLATFORM_ESP32
        rmt_item32_t items[68];
        int item_count = 0;

        items[item_count].level0 = 1;
        items[item_count].duration0 = 9000;
        items[item_count].level1 = 0;
        items[item_count].duration1 = 4500;
        item_count++;

        for (int i = 31; i >= 0; i--) {
            bool bit = (code >> i) & 1;
            items[item_count].level0 = 1;
            items[item_count].duration0 = 560;
            items[item_count].level1 = 0;
            if (bit) {
                items[item_count].duration1 = 1690;
            } else {
                items[item_count].duration1 = 560;
            }
            item_count++;
        }

        items[item_count].level0 = 1;
        items[item_count].duration0 = 560;
        items[item_count].level1 = 0;
        items[item_count].duration1 = 0;
        item_count++;

        esp_err_t ret = rmt_write_items(RMT_CHANNEL_1, items, item_count, true);
        if (ret == ESP_OK) {
            ESP_LOGI("IR_SEND", "ESP32 IR 코드 전송: %s", ir_code.c_str());
            return IRSendStatus(IRSendResult::SUCCESS, "IR 코드 전송 성공");
        } else {
            ESP_LOGE("IR_SEND", "RMT 전송 실패: %s", esp_err_to_name(ret));
            return IRSendStatus(IRSendResult::TRANSMISSION_FAILED, "RMT 전송 실패");
        }
#endif

        return IRSendStatus(IRSendResult::TRANSMISSION_FAILED, "IR 송신 실패");

    } catch (const std::exception& e) {
        setLastError("IR 코드 전송 중 오류: " + std::string(e.what()));
        return IRSendStatus(IRSendResult::INVALID_CODE, "IR 코드 파싱 오류: " + ir_code);
    }
}

IRSendStatus IRSend::sendRawData(const std::vector<int>& raw_data) {
    std::lock_guard<std::mutex> lock(mutex_);

    if (!initialized_) {
        return IRSendStatus(IRSendResult::DEVICE_NOT_FOUND, "IR 송신기가 초기화되지 않음");
    }

    if (raw_data.empty()) {
        return IRSendStatus(IRSendResult::INVALID_CODE, "Raw 데이터가 비어있음");
    }

    auto start_time = std::chrono::high_resolution_clock::now();

#ifdef PLATFORM_ESP32
    size_t item_count = (raw_data.size() + 1) / 2;
    rmt_item32_t* items = new rmt_item32_t[item_count];

    for (size_t i = 0; i < raw_data.size(); i += 2) {
        size_t item_idx = i / 2;

        items[item_idx].level0 = 1;
        items[item_idx].duration0 = raw_data[i];

        if (i + 1 < raw_data.size()) {
            items[item_idx].level1 = 0;
            items[item_idx].duration1 = raw_data[i + 1];
        } else {
            items[item_idx].level1 = 0;
            items[item_idx].duration1 = 0;
        }
    }

    esp_err_t ret = rmt_write_items(RMT_CHANNEL_1, items, item_count, true);
    delete[] items;

    if (ret == ESP_OK) {
        ESP_LOGI("IR_SEND", "ESP32 Raw 데이터 전송: %d개 펄스", (int)raw_data.size());
        auto end_time = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::microseconds>(end_time - start_time);
        return IRSendStatus(IRSendResult::SUCCESS, "Raw 데이터 전송 성공", duration.count() / 1000.0);
    } else {
        ESP_LOGE("IR_SEND", "RMT Raw 데이터 전송 실패: %s", esp_err_to_name(ret));
        return IRSendStatus(IRSendResult::TRANSMISSION_FAILED, "RMT Raw 데이터 전송 실패");
    }
#endif

    return IRSendStatus(IRSendResult::TRANSMISSION_FAILED, "Raw 데이터 송신 실패");
}

std::vector<IRSendStatus> IRSend::sendControlSignals(const std::vector<std::string>& control_signals, int delay_ms) {
    std::vector<IRSendStatus> results;

    for (const auto& signal : control_signals) {
        IRSendStatus result = sendControlSignal(signal);
        results.push_back(result);

        if (result.result != IRSendResult::SUCCESS) {
#ifdef PLATFORM_ESP32
            ESP_LOGE("IR_SEND", "제어 신호 전송 실패: %s - %s", signal.c_str(), result.message.c_str());
#else
            LOG_ERROR("IR_SEND", "제어 신호 전송 실패: %s - %s", signal.c_str(), result.message.c_str());
#endif
        }

        if (delay_ms > 0) {
#ifdef PLATFORM_ESP32
            vTaskDelay(pdMS_TO_TICKS(delay_ms));
#else
            std::this_thread::sleep_for(std::chrono::milliseconds(delay_ms));
#endif
        }
    }

    return results;
}

void IRSend::setCodeStore(IRCodeStore* code_store) {
    std::lock_guard<std::mutex> lock(mutex_);
    code_store_ = code_store;
}

void IRSend::setDebugMode(bool enabled) {
    debug_mode_ = enabled;
#ifdef PLATFORM_ESP32
    ESP_LOGI("IR_SEND", "IR 송신기 디버그 모드: %s", enabled ? "활성화" : "비활성화");
#else
    LOG_INFO("IR_SEND", "IR 송신기 디버그 모드: %s", enabled ? "활성화" : "비활성화");
#endif
}

std::string IRSend::getLastError() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return last_error_;
}

IRSend::Statistics IRSend::getStatistics() const {
    std::lock_guard<std::mutex> lock(stats_mutex_);
    return stats_;
}

bool IRSend::checkDevicePermissions() {
#ifdef PLATFORM_LINUX
    return access("/dev/lirc0", W_OK) == 0;
#else
    return true;
#endif
}

bool IRSend::validateControlSignal(const std::string& control_signal) {
    if (control_signal.empty()) {
        return false;
    }

    return control_signal.length() > 0 && control_signal.length() < 100;
}

std::string IRSend::convertControlSignalToIRCode(const std::string& control_signal) {
    if (code_store_) {
        return code_store_->getIRSignal(control_signal);
    }

    return "";
}

void IRSend::updateStatistics(const IRSendStatus& status) {
    std::lock_guard<std::mutex> lock(stats_mutex_);

    stats_.total_sent++;
    if (status.result == IRSendResult::SUCCESS) {
        stats_.successful_sends++;
    } else {
        stats_.failed_sends++;
    }

    if (stats_.total_sent > 0) {
        stats_.average_duration_ms =
            (stats_.average_duration_ms * (stats_.total_sent - 1) + status.duration_ms) / stats_.total_sent;
    }

    stats_.last_send_time = std::chrono::steady_clock::now();
}

void IRSend::setLastError(const std::string& error) {
    std::lock_guard<std::mutex> lock(mutex_);
    last_error_ = error;
    if (debug_mode_) {
#ifdef PLATFORM_ESP32
        ESP_LOGE("IR_SEND", "IRSend 오류: %s", error.c_str());
#else
        LOG_ERROR("IR_SEND", "IRSend 오류: %s", error.c_str());
#endif
    }
}
