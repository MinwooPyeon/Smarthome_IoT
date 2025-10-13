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

    gpio_config_t io_conf = {};
    io_conf.intr_type = GPIO_INTR_DISABLE;
    io_conf.mode = GPIO_MODE_OUTPUT;
    io_conf.pin_bit_mask = (1ULL << tx_pin);
    io_conf.pull_down_en = GPIO_PULLDOWN_DISABLE;
    io_conf.pull_up_en = GPIO_PULLUP_DISABLE;

    esp_err_t gpio_ret = gpio_config(&io_conf);
    if (gpio_ret != ESP_OK) {
        ESP_LOGE("IR_SEND", "GPIO %d 설정 실패: %s", tx_pin, esp_err_to_name(gpio_ret));
        return false;
    }
    ESP_LOGI("IR_SEND", "GPIO %d 설정 완료", tx_pin);

    rmt_config_t config = {};
    config.rmt_mode = RMT_MODE_TX;
    config.channel = RMT_CHANNEL_1;
    config.gpio_num = (gpio_num_t)tx_pin;
    config.mem_block_num = 4;
    config.tx_config.loop_en = false;
    config.tx_config.carrier_en = true;
    config.tx_config.carrier_freq_hz = 38000;
    config.tx_config.carrier_duty_percent = 50;
    config.tx_config.carrier_level = RMT_CARRIER_LEVEL_HIGH;
    config.tx_config.idle_level = RMT_IDLE_LEVEL_LOW;
    config.tx_config.idle_output_en = true;

    config.clk_div = 50;

    esp_err_t ret = rmt_config(&config);
    if (ret != ESP_OK) {
        ESP_LOGE("IR_SEND", "RMT 설정 실패: %s", esp_err_to_name(ret));
        ESP_LOGE("IR_SEND", "클럭 분주비: %d, 메모리 블록: %d", config.clk_div, config.mem_block_num);
        return false;
    }

    ret = rmt_driver_install(config.channel, 0, 0);
    if (ret != ESP_OK) {
        ESP_LOGE("IR_SEND", "RMT 드라이버 설치 실패: %s", esp_err_to_name(ret));
        ESP_LOGE("IR_SEND", "채널: %d, 클럭 분주비: %d", config.channel, config.clk_div);
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

    for (size_t i = 0; i < raw_data.size(); i++) {
        if (raw_data[i] <= 0 || raw_data[i] > 65535) {
            return IRSendStatus(IRSendResult::INVALID_CODE,
                "잘못된 타이밍 값: 인덱스 " + std::to_string(i) + ", 값 " + std::to_string(raw_data[i]));
        }
    }

    std::vector<int> processed_data = raw_data;

    for (size_t i = 0; i < processed_data.size(); i++) {
        if (processed_data[i] < 5) {
            processed_data[i] = 5;
        } else if (processed_data[i] > 65535) {
            processed_data[i] = 65535;
        }

    }

    ESP_LOGI("IR_SEND", "Raw 데이터 검증 완료: %d개 펄스", (int)processed_data.size());
    if (debug_mode_) {
        ESP_LOGI("IR_SEND", "원본 vs 처리된 타이밍 (첫 10개):");
        for (size_t i = 0; i < std::min((size_t)10, raw_data.size()); i++) {
            ESP_LOGI("IR_SEND", "  [%d]: %d → %d μs", (int)i, raw_data[i], processed_data[i]);
        }
    }

    auto start_time = std::chrono::high_resolution_clock::now();

#ifdef PLATFORM_ESP32
    size_t item_count = (processed_data.size() + 1) / 2;
    rmt_item32_t* items = new rmt_item32_t[item_count];

    for (size_t i = 0; i < processed_data.size(); i += 2) {
        size_t item_idx = i / 2;

        items[item_idx].level0 = 1;
        items[item_idx].duration0 = processed_data[i];

        if (i + 1 < processed_data.size()) {
            items[item_idx].level1 = 0;
            items[item_idx].duration1 = processed_data[i + 1];
        } else {
            items[item_idx].level1 = 0;
            items[item_idx].duration1 = 0;
        }
    }

    esp_err_t ret = rmt_write_items(RMT_CHANNEL_1, items, item_count, true);
    delete[] items;

    if (ret == ESP_OK) {
        ESP_LOGI("IR_SEND", "ESP32 Raw 데이터 전송: %d개 펄스", (int)raw_data.size());
        ESP_LOGI("IR_SEND", "IR 송신기 GPIO 25번 핀에서 신호 송신 중...");
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

IRSendStatus IRSend::sendContinuousSignal(int duration_ms) {
    std::lock_guard<std::mutex> lock(mutex_);

    if (!initialized_) {
        return IRSendStatus(IRSendResult::DEVICE_NOT_FOUND, "IR 송신기가 초기화되지 않음");
    }

    if (duration_ms <= 0 || duration_ms > 10000) {
        return IRSendStatus(IRSendResult::INVALID_CODE, "지속시간은 1-10000ms 범위여야 함");
    }

    auto start_time = std::chrono::high_resolution_clock::now();

#ifdef PLATFORM_ESP32
    ESP_LOGI("IR_SEND", "연속 IR 신호 송신 시작 - %dms 동안", duration_ms);

    rmt_item32_t continuous_item;
    continuous_item.level0 = 1;
    continuous_item.duration0 = 13;
    continuous_item.level1 = 0;
    continuous_item.duration1 = 5;

    auto end_time = start_time + std::chrono::milliseconds(duration_ms);

    while (std::chrono::high_resolution_clock::now() < end_time) {
        esp_err_t ret = rmt_write_items(RMT_CHANNEL_1, &continuous_item, 1, false);
        if (ret != ESP_OK) {
            ESP_LOGE("IR_SEND", "연속 IR 신호 전송 실패: %s", esp_err_to_name(ret));
            return IRSendStatus(IRSendResult::TRANSMISSION_FAILED, "연속 IR 신호 전송 실패");
        }

        vTaskDelay(pdMS_TO_TICKS(1));
    }

    ESP_LOGI("IR_SEND", "연속 IR 신호 송신 완료 - GPIO 25번 핀");
    auto actual_end_time = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::microseconds>(actual_end_time - start_time);
    return IRSendStatus(IRSendResult::SUCCESS, "연속 IR 신호 전송 성공", duration.count() / 1000.0);
#endif

    return IRSendStatus(IRSendResult::TRANSMISSION_FAILED, "연속 IR 신호 송신 실패");
}

IRSendStatus IRSend::sendRepeatedSignal(const std::vector<int>& raw_data, int repeat_count, int delay_ms) {
    std::lock_guard<std::mutex> lock(mutex_);

    if (!initialized_) {
        return IRSendStatus(IRSendResult::DEVICE_NOT_FOUND, "IR 송신기가 초기화되지 않음");
    }

    if (raw_data.empty()) {
        return IRSendStatus(IRSendResult::INVALID_CODE, "Raw 데이터가 비어있음");
    }

    if (repeat_count <= 0 || repeat_count > 10) {
        return IRSendStatus(IRSendResult::INVALID_CODE, "반복 횟수는 1-10 범위여야 함");
    }

    auto start_time = std::chrono::high_resolution_clock::now();

#ifdef PLATFORM_ESP32
    ESP_LOGI("IR_SEND", "IR 신호 반복 송신 시작 - %d회 반복, %dms 간격", repeat_count, delay_ms);

    std::vector<int> processed_data = raw_data;
    for (size_t i = 0; i < processed_data.size(); i++) {
        if (processed_data[i] < 5) processed_data[i] = 5;
        if (processed_data[i] > 65535) processed_data[i] = 65535;
    }

    for (int i = 0; i < repeat_count; i++) {
        size_t item_count = (processed_data.size() + 1) / 2;
        rmt_item32_t* items = new rmt_item32_t[item_count];

        for (size_t j = 0; j < processed_data.size(); j += 2) {
            size_t item_idx = j / 2;

            items[item_idx].level0 = 1;
            items[item_idx].duration0 = processed_data[j];

            if (j + 1 < processed_data.size()) {
                items[item_idx].level1 = 0;
                items[item_idx].duration1 = processed_data[j + 1];
            } else {
                items[item_idx].level1 = 0;
                items[item_idx].duration1 = 0;
            }
        }

        esp_err_t ret = rmt_write_items(RMT_CHANNEL_1, items, item_count, true);
        delete[] items;

        if (ret != ESP_OK) {
            ESP_LOGE("IR_SEND", "IR 신호 반복 송신 실패 (%d/%d): %s", i+1, repeat_count, esp_err_to_name(ret));
            return IRSendStatus(IRSendResult::TRANSMISSION_FAILED, "IR 신호 반복 송신 실패");
        }

        ESP_LOGI("IR_SEND", "IR 신호 송신 완료 (%d/%d)", i+1, repeat_count);

        if (i < repeat_count - 1) {
            vTaskDelay(pdMS_TO_TICKS(delay_ms));
        }
    }

    ESP_LOGI("IR_SEND", "IR 신호 반복 송신 완료 - GPIO 25번 핀");
    auto end_time = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::microseconds>(end_time - start_time);
    return IRSendStatus(IRSendResult::SUCCESS, "IR 신호 반복 송신 성공", duration.count() / 1000.0);
#endif

    return IRSendStatus(IRSendResult::TRANSMISSION_FAILED, "IR 신호 반복 송신 실패");
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
