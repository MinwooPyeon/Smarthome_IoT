#include "hardware/ir_receiver.h"
#include "core/platform.h"
#include <iostream>
#include <chrono>
#include <thread>
#include <atomic>

#ifdef PLATFORM_ESP32

#include "IRremoteESP8266.h"
#include "IRrecv.h"
#include "IRutils.h"
#elif defined(PLATFORM_LINUX)
#include <wiringPi.h>
#elif defined(PLATFORM_WINDOWS)
#include <random>
#endif

IRReceiver::IRReceiver(int gpio_pin) 
    : gpio_pin_(gpio_pin), is_receiving_(false) {
    
#ifdef PLATFORM_ESP32
    irrecv_ = new IRrecv(gpio_pin_);
    irrecv_->enableIRIn();
    LOG_INFO("ESP32 IR 수신기 초기화 완료 - GPIO %d", gpio_pin_);
#elif defined(PLATFORM_LINUX)
    if (wiringPiSetupGpio() == -1) {
        LOG_ERROR("WiringPi 초기화 실패");
        return;
    }
    pinMode(gpio_pin_, INPUT);
    LOG_INFO("Linux IR 수신기 초기화 완료 - GPIO %d", gpio_pin_);
#elif defined(PLATFORM_WINDOWS)
    LOG_INFO("Windows IR 수신기 시뮬레이션 초기화 완료 - GPIO %d", gpio_pin_);
#endif
}

IRReceiver::~IRReceiver() {
    stopReceiving();
    if (receive_thread_.joinable()) {
        receive_thread_.join();
    }
    
#ifdef PLATFORM_ESP32
    if (irrecv_) {
        delete irrecv_;
        irrecv_ = nullptr;
    }
#endif
}

bool IRReceiver::startReceiving() {
    if (is_receiving_) {
        return false;
    }
    
    is_receiving_ = true;
    receive_thread_ = std::thread(&IRReceiver::receiveLoop, this);
    
    LOG_INFO("IR 수신 시작 - GPIO %d", gpio_pin_);
    return true;
}

void IRReceiver::stopReceiving() {
    is_receiving_ = false;
    if (receive_thread_.joinable()) {
        receive_thread_.join();
    }
    LOG_INFO("IR 수신 중지");
}

bool IRReceiver::isReceiving() const {
    return is_receiving_;
}

std::string IRReceiver::receiveIRCode() {
    return readIRCode();
}

void IRReceiver::setIRCodeCallback(std::function<void(const std::string&)> callback) {
    ir_code_callback_ = callback;
}

void IRReceiver::setGPIO(int gpio_pin) {
    gpio_pin_ = gpio_pin;
    
#ifdef PLATFORM_ESP32
    if (irrecv_) {
        delete irrecv_;
        irrecv_ = nullptr;
    }
    irrecv_ = new IRrecv(gpio_pin_);
    irrecv_->enableIRIn();
#elif defined(PLATFORM_LINUX)
    pinMode(gpio_pin_, INPUT);
#endif
}

int IRReceiver::getGPIO() const {
    return gpio_pin_;
}

void IRReceiver::receiveLoop() {
    while (is_receiving_) {
        std::string ir_code = readIRCode();
        
        if (!ir_code.empty()) {
            std::cout << "IR 코드 수신: " << ir_code << std::endl;
            
            if (ir_code_callback_) {
                ir_code_callback_(ir_code);
            }
        }
        
        delay_ms(10);
    }
}

std::string IRReceiver::readIRCode() {
#ifdef PLATFORM_ESP32   
    if (irrecv_ && irrecv_->decode()) {
        std::string result = uint64ToString(irrecv_->decodedIRData.decodedRawData, HEX);
        irrecv_->resume(); 
        
        if (!result.empty() && result.substr(0, 2) != "0x") {
            result = "0x" + result;
        }
        
        // 프로토콜 정보 추가
        std::string protocol = getProtocolName(irrecv_->decodedIRData.protocol);
        LOG_DEBUG("ESP32 IR 코드 수신: %s (프로토콜: %s)", result.c_str(), protocol.c_str());
        return result;
    }
    return "";
#elif defined(PLATFORM_WINDOWS)
    static std::random_device rd;
    static std::mt19937 gen(rd());
    static std::uniform_int_distribution<> dis(0, 100);
    
    if (dis(gen) < 5) { 
        std::uniform_int_distribution<> hex_dis(0, 15);
        std::string code = "0x";
        for (int i = 0; i < 8; i++) {
            code += "0123456789ABCDEF"[hex_dis(gen)];
        }
        return code;
    }   
    return "";
#elif defined(PLATFORM_LINUX)
    if (digitalRead(gpio_pin_) == LOW) {
        return decodeNECProtocol();
    }
    return "";
#endif
}

std::string IRReceiver::decodeNECProtocol() {

    std::string ir_code;   

    auto start_time = std::chrono::high_resolution_clock::now();
    while (digitalRead(gpio_pin_) == LOW) {
        auto now = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::microseconds>(now - start_time);
        if (duration.count() > 10000) break; 
        
        std::this_thread::sleep_for(std::chrono::microseconds(100));
    }
    
    uint32_t data = 0;
    for (int i = 0; i < 32; i++) {

        auto pulse_start = std::chrono::high_resolution_clock::now();
        while (digitalRead(gpio_pin_) == HIGH) {
            std::this_thread::sleep_for(std::chrono::microseconds(100));
        }
        
        auto pulse_end = std::chrono::high_resolution_clock::now();
        auto pulse_width = std::chrono::duration_cast<std::chrono::microseconds>(pulse_end - pulse_start);
        

        if (pulse_width.count() > 1000) {
            data |= (1 << i);
        }
    }
    

    char hex_code[16];
    snprintf(hex_code, sizeof(hex_code), "0x%08X", data);
    return std::string(hex_code);
}

std::string IRReceiver::decodeRC5Protocol() {
    
    std::string ir_code;    
    uint16_t data = 0;
    
    auto start_time = std::chrono::high_resolution_clock::now();
    while (digitalRead(gpio_pin_) == LOW) {
        auto now = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::microseconds>(now - start_time);
        if (duration.count() > 2000) break; 
        
        std::this_thread::sleep_for(std::chrono::microseconds(50));
    }
    
    for (int i = 0; i < 14; i++) {
        auto pulse_start = std::chrono::high_resolution_clock::now();
        while (digitalRead(gpio_pin_) == HIGH) {
            std::this_thread::sleep_for(std::chrono::microseconds(50));
        }
        
        auto pulse_end = std::chrono::high_resolution_clock::now();
        auto pulse_width = std::chrono::duration_cast<std::chrono::microseconds>(pulse_end - pulse_start);
        
        if (pulse_width.count() > 1200) {
            data |= (1 << (13 - i));
        }
        
        while (digitalRead(gpio_pin_) == LOW) {
            std::this_thread::sleep_for(std::chrono::microseconds(50));
        }
    }
    
    // 16진수 문자열로 변환
    char hex_code[8];
    snprintf(hex_code, sizeof(hex_code), "0x%04X", data);
    return std::string(hex_code);
}

std::string IRReceiver::decodeSonyProtocol() {
    std::string ir_code;
    uint32_t data = 0;
    int bit_count = 0;
    
    auto start_time = std::chrono::high_resolution_clock::now();
    while (digitalRead(gpio_pin_) == LOW) {
        auto now = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::microseconds>(now - start_time);
        if (duration.count() > 5000) break; 
        
        std::this_thread::sleep_for(std::chrono::microseconds(100));
    }
    
    for (int i = 0; i < 20; i++) {
        auto pulse_start = std::chrono::high_resolution_clock::now();
        while (digitalRead(gpio_pin_) == HIGH) {
            std::this_thread::sleep_for(std::chrono::microseconds(50));
        }
        
        auto pulse_end = std::chrono::high_resolution_clock::now();
        auto pulse_width = std::chrono::duration_cast<std::chrono::microseconds>(pulse_end - pulse_start);
        
        if (pulse_width.count() > 900) {
            data |= (1 << i);
        }
        
        bit_count++;
        
        
        auto low_start = std::chrono::high_resolution_clock::now();
        while (digitalRead(gpio_pin_) == LOW) {
            auto now = std::chrono::high_resolution_clock::now();
            auto duration = std::chrono::duration_cast<std::chrono::microseconds>(now - low_start);
            if (duration.count() > 3000) {
                
                break;
            }
            std::this_thread::sleep_for(std::chrono::microseconds(50));
        }
        
        // 신호 종료 감지
        if (digitalRead(gpio_pin_) == HIGH) {
            auto check_time = std::chrono::high_resolution_clock::now();
            auto check_duration = std::chrono::duration_cast<std::chrono::microseconds>(check_time - low_start);
            if (check_duration.count() > 3000) {
                break; 
            }
        }
    }
    
    char hex_code[16];
    if (bit_count <= 12) {
        snprintf(hex_code, sizeof(hex_code), "0x%03X", data & 0xFFF);
    } else if (bit_count <= 15) {
        snprintf(hex_code, sizeof(hex_code), "0x%04X", data & 0xFFFF);
    } else {
        snprintf(hex_code, sizeof(hex_code), "0x%05X", data & 0xFFFFF);
    }
    
    return std::string(hex_code);
}

std::string IRReceiver::getProtocolName(decode_type_t protocol) {
    switch (protocol) {
        case NEC: return "NEC";
        case SONY: return "Sony";
        case RC5: return "RC5";
        case RC6: return "RC6";
        case SAMSUNG: return "Samsung";
        case LG: return "LG";
        case PANASONIC: return "Panasonic";
        case JVC: return "JVC";
        case MITSUBISHI: return "Mitsubishi";
        case DENON: return "Denon";
        case SHARP: return "Sharp";
        case SANYO: return "Sanyo";
        case TOSHIBA: return "Toshiba";
        case AIWA: return "Aiwa";
        case PIONEER: return "Pioneer";
        case ONKYO: return "Onkyo";
        case BOSE: return "Bose";
        case BANG_OLUFSEN: return "Bang & Olufsen";
        case UNKNOWN:
        default: return "Unknown";
    }
}
