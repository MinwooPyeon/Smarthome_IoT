#include "hardware/ir_receiver.h"
#include <iostream>
#include <chrono>
#include <thread>
#include <atomic>

#ifdef _WIN32
// Windows 환경에서는 시뮬레이션
#include <random>
#else
// Linux 환경에서는 실제 GPIO 사용
#include <wiringPi.h>
#endif

IRReceiver::IRReceiver(int gpio_pin) 
    : gpio_pin_(gpio_pin), is_receiving_(false) {
    
#ifndef _WIN32
    // Linux에서만 wiringPi 초기화
    if (wiringPiSetupGpio() == -1) {
        std::cerr << "WiringPi 초기화 실패" << std::endl;
        return;
    }
    pinMode(gpio_pin_, INPUT);
#endif
}

IRReceiver::~IRReceiver() {
    stopReceiving();
    if (receive_thread_.joinable()) {
        receive_thread_.join();
    }
}

bool IRReceiver::startReceiving() {
    if (is_receiving_) {
        return false;
    }
    
    is_receiving_ = true;
    receive_thread_ = std::thread(&IRReceiver::receiveLoop, this);
    
    std::cout << "IR 수신 시작 - GPIO " << gpio_pin_ << std::endl;
    return true;
}

void IRReceiver::stopReceiving() {
    is_receiving_ = false;
    if (receive_thread_.joinable()) {
        receive_thread_.join();
    }
    std::cout << "IR 수신 중지" << std::endl;
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
#ifndef _WIN32
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
        
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
}

std::string IRReceiver::readIRCode() {
#ifdef _WIN32
    // Windows 시뮬레이션: 랜덤 IR 코드 생성
    static std::random_device rd;
    static std::mt19937 gen(rd());
    static std::uniform_int_distribution<> dis(0, 100);
    
    if (dis(gen) < 5) { // 5% 확률로 IR 코드 수신
        std::uniform_int_distribution<> hex_dis(0, 15);
        std::string code = "0x";
        for (int i = 0; i < 8; i++) {
            code += "0123456789ABCDEF"[hex_dis(gen)];
        }
        return code;
    }
    return "";
#else
    // Linux: 실제 IR 센서에서 신호 읽기
    if (digitalRead(gpio_pin_) == LOW) {
        return decodeNECProtocol();
    }
    return "";
#endif
}

std::string IRReceiver::decodeNECProtocol() {
    // NEC 프로토콜 디코딩 (대부분 가전기기 리모컨)
    std::string ir_code;
    
    // 리드 타임 대기 (9ms)
    auto start_time = std::chrono::high_resolution_clock::now();
    while (digitalRead(gpio_pin_) == LOW) {
        auto now = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::microseconds>(now - start_time);
        if (duration.count() > 10000) break; // 10ms 초과 시 중단
        
        std::this_thread::sleep_for(std::chrono::microseconds(100));
    }
    
    // 데이터 비트 읽기 (32비트)
    uint32_t data = 0;
    for (int i = 0; i < 32; i++) {
        // 펄스 폭 측정
        auto pulse_start = std::chrono::high_resolution_clock::now();
        while (digitalRead(gpio_pin_) == HIGH) {
            std::this_thread::sleep_for(std::chrono::microseconds(100));
        }
        
        auto pulse_end = std::chrono::high_resolution_clock::now();
        auto pulse_width = std::chrono::duration_cast<std::chrono::microseconds>(pulse_end - pulse_start);
        
        // NEC 프로토콜: 560μs = 0, 1690μs = 1
        if (pulse_width.count() > 1000) {
            data |= (1 << i);
        }
    }
    
    // 16진수 문자열로 변환
    char hex_code[16];
    snprintf(hex_code, sizeof(hex_code), "0x%08X", data);
    return std::string(hex_code);
}

std::string IRReceiver::decodeRC5Protocol() {
    // RC5 프로토콜 디코딩 (Philips 등)
    // 구현 생략 - 필요시 추가
    return "";
}

std::string IRReceiver::decodeSonyProtocol() {
    // Sony 프로토콜 디코딩
    // 구현 생략 - 필요시 추가
    return "";
}
