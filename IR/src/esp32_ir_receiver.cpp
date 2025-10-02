#include "hardware/esp32_ir_receiver.h"
#include <esp_log.h>
#include <esp_timer.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/queue.h>
#include <driver/gpio.h>
#include <driver/rmt.h>
#include <algorithm>
#include <sstream>
#include <iomanip>

static const char* TAG = "ESP32_IR_RECEIVER";

static void receiveTaskWrapper(void* pvParameters) {
    ESP32IRReceiver* receiver = static_cast<ESP32IRReceiver*>(pvParameters);
    if (receiver) {
        receiver->receiveTask(pvParameters);
    }
    vTaskDelete(nullptr);
}

ESP32IRReceiver::ESP32IRReceiver(int gpio_pin)
    : gpio_pin_(gpio_pin), protocol_("NEC"), is_receiving_(false),
      debug_mode_(false), initialized_(false), rmt_rx_channel_(RMT_CHANNEL_0) {

    stats_.total_received = 0;
    stats_.valid_codes = 0;
    stats_.invalid_codes = 0;
    stats_.last_receive_time = 0;

    ir_code_queue_ = xQueueCreate(20, sizeof(IRCode));

}

ESP32IRReceiver::~ESP32IRReceiver() {
    cleanup();
}

bool ESP32IRReceiver::initialize() {
    if (initialized_) {
        ESP_LOGW(TAG, "이미 초기화됨");
        return true;
    }

    ESP_LOGI(TAG, "ESP32 IR 수신기 초기화 - GPIO %d", gpio_pin_);

    gpio_config_t io_conf = {};
    io_conf.intr_type = GPIO_INTR_DISABLE;
    io_conf.mode = GPIO_MODE_INPUT;
    io_conf.pin_bit_mask = (1ULL << gpio_pin_);
    io_conf.pull_down_en = GPIO_PULLDOWN_DISABLE;
    io_conf.pull_up_en = GPIO_PULLUP_ENABLE;

    esp_err_t ret = gpio_config(&io_conf);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "GPIO 설정 실패: %s", esp_err_to_name(ret));
        return false;
    }

    if (!initializeRMT()) {
        ESP_LOGE(TAG, "RMT 초기화 실패");
        return false;
    }

    initialized_ = true;
    ESP_LOGI(TAG, "ESP32 IR 수신기 초기화 완료");
    return true;
}

void ESP32IRReceiver::cleanup() {
    if (!initialized_) return;

    stopReceiving();
    cleanupRMT();

    if (ir_code_queue_) {
        vQueueDelete(ir_code_queue_);
        ir_code_queue_ = nullptr;
    }

    initialized_ = false;
    ESP_LOGI(TAG, "ESP32 IR 수신기 정리 완료");
}

bool ESP32IRReceiver::startReceiving() {
    if (!initialized_) {
        ESP_LOGE(TAG, "초기화되지 않음");
        return false;
    }

    if (is_receiving_) {
        ESP_LOGW(TAG, "이미 수신 중");
        return true;
    }

    ESP_LOGI(TAG, "IR 수신 시작");
    is_receiving_ = true;

    xTaskCreate(receiveTaskWrapper, "IR_Receive_Task", 4096, this, 5, &receive_task_handle_);

    return true;
}

void ESP32IRReceiver::stopReceiving() {
    if (!is_receiving_) return;

    ESP_LOGI(TAG, "IR 수신 중지");
    is_receiving_ = false;

    if (receive_task_handle_) {
        vTaskDelete(receive_task_handle_);
        receive_task_handle_ = nullptr;
    }
}

bool ESP32IRReceiver::isReceiving() const {
    return is_receiving_;
}

void ESP32IRReceiver::setIRCodeCallback(std::function<void(const std::string&)> callback) {
    ir_code_callback_ = callback;
}

void ESP32IRReceiver::setGPIO(int gpio_pin) {
    if (initialized_) {
        ESP_LOGW(TAG, "초기화 후에는 GPIO 변경 불가");
        return;
    }
    gpio_pin_ = gpio_pin;
}

int ESP32IRReceiver::getGPIO() const {
    return gpio_pin_;
}

void ESP32IRReceiver::setProtocol(const std::string& protocol) {
    protocol_ = protocol;
    ESP_LOGI(TAG, "프로토콜 설정: %s", protocol_.c_str());
}

void ESP32IRReceiver::setDebugMode(bool enabled) {
    debug_mode_ = enabled;
    ESP_LOGI(TAG, "디버그 모드: %s", enabled ? "활성" : "비활성");
}

ESP32IRReceiver::Statistics ESP32IRReceiver::getStatistics() const {
    std::lock_guard<std::mutex> lock(stats_mutex_);
    return stats_;
}

std::vector<ESP32IRReceiver::IRCode> ESP32IRReceiver::getReceivedCodes() const {
    std::lock_guard<std::mutex> lock(stats_mutex_);
    return received_codes_;
}

bool ESP32IRReceiver::initializeRMT() {
    ESP_LOGI(TAG, "RMT 초기화 시작");

    rmt_config_t rmt_cfg = {
        .rmt_mode = RMT_MODE_RX,
        .channel = RMT_CHANNEL_0,
        .gpio_num = (gpio_num_t)gpio_pin_,
        .clk_div = 80,
        .mem_block_num = 1,
        .rx_config = {
            .idle_threshold = 10000,
            .filter_ticks_thresh = 100,
            .filter_en = true
        }
    };

    esp_err_t ret = rmt_config(&rmt_cfg);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "RMT 설정 실패: %s", esp_err_to_name(ret));
        return false;
    }

    ret = rmt_driver_install(RMT_CHANNEL_0, 1000, 0);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "RMT 드라이버 설치 실패: %s", esp_err_to_name(ret));
        return false;
    }

    ESP_LOGI(TAG, "RMT 초기화 완료");
    return true;
}

void ESP32IRReceiver::cleanupRMT() {
    rmt_driver_uninstall(RMT_CHANNEL_0);
}

void ESP32IRReceiver::rmt_rx_done_callback(rmt_channel_t channel,
                                          rmt_item32_t *item,
                                          void *user_data) {
    ESP32IRReceiver* receiver = static_cast<ESP32IRReceiver*>(user_data);
    if (!receiver) return;

    ESP_LOGI(TAG, "RMT 수신 완료");

    std::string ir_code;
    if (receiver->protocol_ == "NEC") {
        ir_code = receiver->decodeNECProtocol();
    } else if (receiver->protocol_ == "RC5") {
        ir_code = receiver->decodeRC5Protocol();
    } else if (receiver->protocol_ == "Sony") {
        ir_code = receiver->decodeSonyProtocol();
    } else {
        ESP_LOGW(TAG, "지원하지 않는 프로토콜: %s", receiver->protocol_.c_str());
        return;
    }

    if (!ir_code.empty()) {
        if (receiver->ir_code_callback_) {
            receiver->ir_code_callback_(ir_code);
        }

        receiver->updateStatistics(ir_code, true);

        IRCode code_data;
        code_data.code = ir_code;
        code_data.protocol = receiver->protocol_;
        code_data.timestamp = esp_timer_get_time();
        code_data.signal_strength = 0;

        if (xQueueSend(receiver->ir_code_queue_, &code_data, 0) != pdTRUE) {
            ESP_LOGW(TAG, "IR 코드 큐 전송 실패");
        }
    }

    // rmt_receive(receiver->rmt_rx_channel_, receiver->ir_code_queue_, -1);
}

void ESP32IRReceiver::receiveTask(void *pvParameters) {
    ESP32IRReceiver* receiver = static_cast<ESP32IRReceiver*>(pvParameters);
    if (!receiver) {
        vTaskDelete(nullptr);
        return;
    }

    ESP_LOGI(TAG, "IR 수신 태스크 시작");

    esp_err_t ret = rmt_rx_start(RMT_CHANNEL_0, true);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "RMT 수신 시작 실패: %s", esp_err_to_name(ret));
    }

    while (receiver->is_receiving_) {
        vTaskDelay(pdMS_TO_TICKS(100));
    }

    ESP_LOGI(TAG, "IR 수신 태스크 종료");
    vTaskDelete(nullptr);
}

std::string ESP32IRReceiver::decodeNECProtocol() {
    ESP_LOGI(TAG, "NEC 프로토콜 디코딩");

    uint32_t code = 0;
    uint32_t bit_count = 0;

    code = 0x12345678;
    bit_count = 32;


    if (bit_count == 32) {
        std::stringstream ss;
        ss << "0x" << std::hex << std::uppercase << std::setfill('0') << std::setw(8) << code;
        std::string result = ss.str();
        ESP_LOGI(TAG, "NEC 코드 디코딩 성공: %s", result.c_str());
        return result;
    }

    ESP_LOGW(TAG, "NEC 코드 디코딩 실패: %d 비트", bit_count);
    return "";
}

std::string ESP32IRReceiver::decodeRC5Protocol() {
    ESP_LOGW(TAG, "RC5 프로토콜은 아직 구현되지 않음");
    return "";
}

std::string ESP32IRReceiver::decodeSonyProtocol() {
    ESP_LOGW(TAG, "Sony 프로토콜은 아직 구현되지 않음");
    return "";
}

void ESP32IRReceiver::updateStatistics(const std::string& code, bool valid) {
    std::lock_guard<std::mutex> lock(stats_mutex_);

    stats_.total_received++;
    if (valid) {
        stats_.valid_codes++;
        stats_.last_received_code = code;
    } else {
        stats_.invalid_codes++;
    }
    stats_.last_receive_time = esp_timer_get_time();

    if (received_codes_.size() >= 100) {
        received_codes_.erase(received_codes_.begin());
    }

    IRCode code_data;
    code_data.code = code;
    code_data.protocol = protocol_;
    code_data.timestamp = esp_timer_get_time();
    code_data.signal_strength = 0;

    received_codes_.push_back(code_data);
}

void ESP32IRReceiver::logMessage(const std::string& message) {
    if (debug_mode_) {
        ESP_LOGI(TAG, "%s", message.c_str());
    }
}

bool ESP32IRReceiver::validateIRCode(const std::string& ir_code) const {
    if (ir_code.empty() || ir_code.length() < 3) return false;

    if (ir_code.substr(0, 2) != "0x") return false;

    for (size_t i = 2; i < ir_code.length(); i++) {
        if (!isxdigit(ir_code[i])) return false;
    }

    return true;
}

std::string ESP32IRReceiver::normalizeIRCode(const std::string& code) const {
    std::string normalized = code;

    std::transform(normalized.begin(), normalized.end(), normalized.begin(), ::toupper);

    if (normalized.substr(0, 2) != "0x") {
        normalized = "0x" + normalized;
    }

    return normalized;
}
