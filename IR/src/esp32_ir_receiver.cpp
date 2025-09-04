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

ESP32IRReceiver::ESP32IRReceiver(gpio_num_t gpio_pin)
    : gpio_pin_(gpio_pin), protocol_("NEC"), is_receiving_(false), 
      debug_mode_(false), initialized_(false), rmt_rx_channel_(nullptr) {
    
    // 통계 초기화
    stats_.total_received = 0;
    stats_.valid_codes = 0;
    stats_.invalid_codes = 0;
    stats_.last_receive_time = 0;
    
    // 큐 생성
    ir_code_queue_ = xQueueCreate(20, sizeof(IRCode));
    
    // 뮤텍스 초기화
    portMUX_INITIALIZER(&stats_mutex_);
}

ESP32IRReceiver::~ESP32IRReceiver() {
    cleanup();
}

bool ESP32IRReceiver::initialize() {
    if (initialized_) {
        ESP_LOGW(TAG, "이미 초기화됨");
        return true;
    }
    
    ESP_LOGI(TAG, "ESP32 IR 수신기 초기화 시작 - GPIO %d", gpio_pin_);
    
    // GPIO 설정
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
    
    // RMT 초기화
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
    
    // 수신 태스크 생성
    xTaskCreate(receiveTask, "IR_Receive_Task", 4096, this, 5, &receive_task_handle_);
    
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

void ESP32IRReceiver::setGPIO(gpio_num_t gpio_pin) {
    if (initialized_) {
        ESP_LOGW(TAG, "초기화 후에는 GPIO 변경 불가");
        return;
    }
    gpio_pin_ = gpio_pin;
}

gpio_num_t ESP32IRReceiver::getGPIO() const {
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
    portENTER_CRITICAL(&stats_mutex_);
    Statistics stats = stats_;
    portEXIT_CRITICAL(&stats_mutex_);
    return stats;
}

std::vector<ESP32IRReceiver::IRCode> ESP32IRReceiver::getReceivedCodes() const {
    portENTER_CRITICAL(&stats_mutex_);
    std::vector<IRCode> codes = received_codes_;
    portEXIT_CRITICAL(&stats_mutex_);
    return codes;
}

bool ESP32IRReceiver::initializeRMT() {
    ESP_LOGI(TAG, "RMT 초기화 시작");
    
    // RMT 수신 채널 설정
    rmt_rx_channel_config_t rx_cfg = {
        .gpio_num = gpio_pin_,
        .clk_src = RMT_CLK_SRC_DEFAULT,
        .resolution_hz = 10000000, // 10MHz
        .mem_block_symbols = 64,
        .invert_in = false,
        .with_dma = false,
        .io_loop_back = false,
        .intr_priority = 1,
    };
    
    esp_err_t ret = rmt_new_rx_channel(&rx_cfg, &rmt_rx_channel_);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "RMT 채널 생성 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    // RMT 이벤트 콜백 설정
    rmt_rx_cbs_.on_recv_done = rmt_rx_done_callback;
    
    ret = rmt_rx_register_event_callbacks(rmt_rx_channel_, &rmt_rx_cbs_, this);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "RMT 콜백 등록 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    // RMT 채널 활성화
    ret = rmt_channel_enable(rmt_rx_channel_);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "RMT 채널 활성화 실패: %s", esp_err_to_name(ret));
        return false;
    }
    
    ESP_LOGI(TAG, "RMT 초기화 완료");
    return true;
}

void ESP32IRReceiver::cleanupRMT() {
    if (rmt_rx_channel_) {
        rmt_del_channel(rmt_rx_channel_);
        rmt_rx_channel_ = nullptr;
    }
}

void ESP32IRReceiver::rmt_rx_done_callback(rmt_channel_handle_t channel, 
                                          const rmt_rx_done_event_data_t *edata, 
                                          void *user_data) {
    ESP32IRReceiver* receiver = static_cast<ESP32IRReceiver*>(user_data);
    if (!receiver) return;
    
    ESP_LOGI(TAG, "RMT 수신 완료 - %d 신호", edata->num_symbols);
    
    // 프로토콜에 따른 디코딩
    std::string ir_code;
    if (receiver->protocol_ == "NEC") {
        ir_code = receiver->decodeNECProtocol(edata);
    } else if (receiver->protocol_ == "RC5") {
        ir_code = receiver->decodeRC5Protocol(edata);
    } else if (receiver->protocol_ == "Sony") {
        ir_code = receiver->decodeSonyProtocol(edata);
    } else {
        ESP_LOGW(TAG, "지원하지 않는 프로토콜: %s", receiver->protocol_.c_str());
        return;
    }
    
    if (!ir_code.empty()) {
        // IR 코드 콜백 호출
        if (receiver->ir_code_callback_) {
            receiver->ir_code_callback_(ir_code);
        }
        
        // 통계 업데이트
        receiver->updateStatistics(ir_code, true);
        
        // 큐에 추가
        IRCode code_data;
        code_data.code = ir_code;
        code_data.protocol = receiver->protocol_;
        code_data.timestamp = esp_timer_get_time();
        code_data.signal_strength = edata->num_symbols;
        
        if (xQueueSend(receiver->ir_code_queue_, &code_data, 0) != pdTRUE) {
            ESP_LOGW(TAG, "IR 코드 큐 전송 실패");
        }
    }
    
    // 다음 수신 준비
    rmt_receive(receiver->rmt_rx_channel_, receiver->ir_code_queue_, -1);
}

void ESP32IRReceiver::receiveTask(void *pvParameters) {
    ESP32IRReceiver* receiver = static_cast<ESP32IRReceiver*>(pvParameters);
    if (!receiver) {
        vTaskDelete(nullptr);
        return;
    }
    
    ESP_LOGI(TAG, "IR 수신 태스크 시작");
    
    // 첫 번째 수신 시작
    esp_err_t ret = rmt_receive(receiver->rmt_rx_channel_, receiver->ir_code_queue_, -1);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "RMT 수신 시작 실패: %s", esp_err_to_name(ret));
    }
    
    // 태스크 루프
    while (receiver->is_receiving_) {
        vTaskDelay(pdMS_TO_TICKS(100));
    }
    
    ESP_LOGI(TAG, "IR 수신 태스크 종료");
    vTaskDelete(nullptr);
}

std::string ESP32IRReceiver::decodeNECProtocol(const rmt_rx_done_event_data_t *edata) {
    if (!edata || edata->num_symbols < 32) {
        ESP_LOGW(TAG, "NEC 프로토콜 신호 부족");
        return "";
    }
    
    uint32_t code = 0;
    uint32_t bit_count = 0;
    
    // NEC 프로토콜 디코딩 (32비트)
    for (int i = 0; i < edata->num_symbols && bit_count < 32; i++) {
        uint32_t high_time = edata->received_symbols[i].duration0;
        uint32_t low_time = edata->received_symbols[i].duration1;
        
        // NEC 타이밍: HIGH 9000us, LOW 4500us (1) 또는 2250us (0)
        if (high_time > 8000 && high_time < 10000) {
            if (low_time > 4000 && low_time < 5000) {
                // 1 비트
                code |= (1ULL << bit_count);
            } else if (low_time > 2000 && low_time < 2500) {
                // 0 비트
                // 이미 0으로 설정됨
            } else {
                ESP_LOGW(TAG, "잘못된 NEC 타이밍: HIGH=%d, LOW=%d", high_time, low_time);
                return "";
            }
            bit_count++;
        }
    }
    
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

std::string ESP32IRReceiver::decodeRC5Protocol(const rmt_rx_done_event_data_t *edata) {
    // RC5 프로토콜 디코딩 구현
    ESP_LOGW(TAG, "RC5 프로토콜은 아직 구현되지 않음");
    return "";
}

std::string ESP32IRReceiver::decodeSonyProtocol(const rmt_rx_done_event_data_t *edata) {
    // Sony 프로토콜 디코딩 구현
    ESP_LOGW(TAG, "Sony 프로토콜은 아직 구현되지 않음");
    return "";
}

void ESP32IRReceiver::updateStatistics(const std::string& code, bool valid) {
    portENTER_CRITICAL(&stats_mutex_);
    
    stats_.total_received++;
    if (valid) {
        stats_.valid_codes++;
        stats_.last_received_code = code;
    } else {
        stats_.invalid_codes++;
    }
    stats_.last_receive_time = esp_timer_get_time();
    
    // 수신된 코드 저장 (최대 100개)
    if (received_codes_.size() >= 100) {
        received_codes_.erase(received_codes_.begin());
    }
    
    IRCode code_data;
    code_data.code = code;
    code_data.protocol = protocol_;
    code_data.timestamp = esp_timer_get_time();
    code_data.signal_strength = 0;
    
    received_codes_.push_back(code_data);
    
    portEXIT_CRITICAL(&stats_mutex_);
}

void ESP32IRReceiver::logMessage(const std::string& message) {
    if (debug_mode_) {
        ESP_LOGI(TAG, "%s", message.c_str());
    }
}

bool ESP32IRReceiver::validateIRCode(const std::string& ir_code) const {
    if (ir_code.empty() || ir_code.length() < 3) return false;
    
    // 16진수 형식 검증 (0x로 시작)
    if (ir_code.substr(0, 2) != "0x") return false;
    
    // 16진수 문자 검증
    for (size_t i = 2; i < ir_code.length(); i++) {
        if (!isxdigit(ir_code[i])) return false;
    }
    
    return true;
}

std::string ESP32IRReceiver::normalizeIRCode(const std::string& code) const {
    std::string normalized = code;
    
    // 대문자로 변환
    std::transform(normalized.begin(), normalized.end(), normalized.begin(), ::toupper);
    
    // 0x 접두사 확인
    if (normalized.substr(0, 2) != "0x") {
        normalized = "0x" + normalized;
    }
    
    return normalized;
}
