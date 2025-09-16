#include "hardware/ir_receiver.h"
#include "core/platform.h"
#include <iostream>
#include <chrono>
#include <thread>
#include <atomic>

#ifdef PLATFORM_ESP32

#include "esp_log.h"
#include "driver/rmt.h"
#include "driver/gpio.h"
#include "Arduino.h"
#elif defined(PLATFORM_LINUX)
#include <wiringPi.h>
#elif defined(PLATFORM_WINDOWS)
#include <random>
#endif

IRReceiver::IRReceiver(int gpio_pin)
    : gpio_pin_(gpio_pin), is_receiving_(false) {

#ifdef PLATFORM_ESP32
    // ESP32 전용 IR 수신 초기화
    is_initialized_ = true;
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
    // ESP32 전용 정리
    is_initialized_ = false;
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
    // ESP32 전용 GPIO 설정
    is_initialized_ = true;
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
    // ESP32 전용 IR 코드 읽기 (실제 구현은 나중에)
    if (is_initialized_) {
        // 시뮬레이션된 IR 코드 반환
        return "0x12345678";
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

std::string IRReceiver::getProtocolName(int protocol) {
    // 프로토콜 상수 정의
    const int NEC = 1;
    const int SONY = 2;
    const int RC5 = 3;
    const int RC6 = 4;
    const int SAMSUNG = 5;
    const int LG = 6;
    const int PANASONIC = 7;
    const int JVC = 8;
    const int MITSUBISHI = 9;
    const int DENON = 10;
    const int SHARP = 11;
    const int SANYO = 12;
    const int TOSHIBA = 15;
    const int AIWA = 16;
    const int PIONEER = 17;
    const int ONKYO = 18;
    const int BOSE = 19;
    const int BANG_OLUFSEN = 20;
    const int UNKNOWN = 0;

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
