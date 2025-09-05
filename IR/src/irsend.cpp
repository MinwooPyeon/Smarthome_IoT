#include "hardware/irsend.h"
#include "core/platform.h"
#include <iostream>
#include <fstream>
#include "ArduinoJson.h"

#ifdef PLATFORM_ESP32
#include "IRremoteESP8266.h"
#include "IRsend.h"
#elif defined(PLATFORM_LINUX)
#include <lirc/lirc_client.h>
#elif defined(PLATFORM_WINDOWS)
#include <random>
#endif

IRSend::IRSend() 
    : initialized_(false), debug_mode_(false), code_store_(nullptr) {
    
#ifdef PLATFORM_ESP32
    irsend_ = nullptr;
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
    irsend_ = other.irsend_;
    other.irsend_ = nullptr;
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
        irsend_ = other.irsend_;
        other.irsend_ = nullptr;
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
    int tx_pin = 4; // 기본 GPIO 핀
    irsend_ = new IRsend(tx_pin);
    irsend_->begin();
    LOG_INFO("ESP32 IR 송신기 초기화 완료 - GPIO %d", tx_pin);
#elif defined(PLATFORM_LINUX)   
    lirc_fd_ = lirc_get_local_socket(nullptr, 0);
    if (lirc_fd_ < 0) {
        setLastError("lirc 소켓 생성 실패");
        return false;
    }
    LOG_INFO("Linux IR 송신기 초기화 완료");
#elif defined(PLATFORM_WINDOWS)
    LOG_INFO("Windows IR 송신기 시뮬레이션 초기화 완료");
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
    if (irsend_) {
        delete irsend_;
        irsend_ = nullptr;
    }
#elif defined(PLATFORM_LINUX)
    if (lirc_fd_ >= 0) {
        lirc_freeconfig(config_);
        lirc_deinit();
        lirc_fd_ = -1;
    }
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
    
    // 제어 신호를 IR 코드로 변환
    std::string ir_code = convertControlSignalToIRCode(control_signal);
    if (ir_code.empty()) {
        return IRSendStatus(IRSendResult::INVALID_CODE, "IR 코드 변환 실패: " + control_signal);
    }
    
    // IR 코드 전송
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
        uint64_t code = std::stoull(ir_code.substr(2), nullptr, 16); // "0x" 제거
        
#ifdef PLATFORM_ESP32
        // ESP32 IR 송신
        if (irsend_) {
            irsend_->sendNEC(code, 32); 
            LOG_INFO("ESP32 IR 코드 전송: %s", ir_code.c_str());
            return IRSendStatus(IRSendResult::SUCCESS, "IR 코드 전송 성공");
        }
#elif defined(PLATFORM_LINUX)
        if (lirc_fd_ >= 0) {
            std::string command = "SEND_ONCE " + ir_code;
            if (lirc_send_one(lirc_fd_, command.c_str()) == 0) {
                LOG_INFO("Linux IR 코드 전송: %s", ir_code.c_str());
                return IRSendStatus(IRSendResult::SUCCESS, "IR 코드 전송 성공");
            } else {
                return IRSendStatus(IRSendResult::TRANSMISSION_FAILED, "lirc 송신 실패");
            }
        }
#elif defined(PLATFORM_WINDOWS)
        LOG_INFO("Windows IR 코드 시뮬레이션 전송: %s", ir_code.c_str());
        return IRSendStatus(IRSendResult::SUCCESS, "IR 코드 시뮬레이션 전송 성공");
#endif
        
        return IRSendStatus(IRSendResult::TRANSMISSION_FAILED, "IR 송신 실패");
        
    } catch (const std::exception& e) {
        setLastError("IR 코드 전송 중 오류: " + std::string(e.what()));
        return IRSendStatus(IRSendResult::INVALID_CODE, "IR 코드 파싱 오류: " + ir_code);
    }
}

std::vector<IRSendStatus> IRSend::sendControlSignals(const std::vector<std::string>& control_signals, int delay_ms) {
    std::vector<IRSendStatus> results;
    
    for (const auto& signal : control_signals) {
        IRSendStatus result = sendControlSignal(signal);
        results.push_back(result);
        
        if (result.result != IRSendResult::SUCCESS) {
            LOG_ERROR("제어 신호 전송 실패: %s - %s", signal.c_str(), result.message.c_str());
        }
        
        if (delay_ms > 0) {
            delay_ms(delay_ms);
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
    LOG_INFO("IR 송신기 디버그 모드: %s", enabled ? "활성화" : "비활성화");
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
    static std::map<std::string, std::string> control_to_ir = {
        {"TV_POWER", "0xE0E040BF"},
        {"TV_VOLUME_UP", "0xE0E0E01F"},
        {"TV_VOLUME_DOWN", "0xE0E0D02F"},
        {"TV_CHANNEL_UP", "0xE0E048B7"},
        {"TV_CHANNEL_DOWN", "0xE0E008F7"},
        {"AC_POWER", "0xE0E040BF"},
        {"AC_TEMP_UP", "0xE0E01CE3"},
        {"AC_TEMP_DOWN", "0xE0E05CA3"},
        {"AC_MODE", "0xE0E014EB"},
        {"PURIFIER_POWER", "0xE0E040BF"},
        {"PURIFIER_MODE", "0xE0E014EB"},
        {"PROJECTOR_POWER", "0x20DF10EF"},
        {"PROJECTOR_MODE", "0x20DF50AF"}
    };
    
    auto it = control_to_ir.find(control_signal);
    if (it != control_to_ir.end()) {
        return it->second;
    }
    
    // IR 코드 저장소에서 찾기
    if (code_store_) {
        return code_store_->getIRCode(control_signal);
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
    
    // 평균 소요 시간 계산
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
        LOG_ERROR("IRSend 오류: %s", error.c_str());
    }
}
