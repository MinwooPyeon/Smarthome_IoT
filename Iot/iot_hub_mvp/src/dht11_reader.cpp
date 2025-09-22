#include "dht11_reader.hpp"
#include <pigpio.h>
#include <stdexcept>
#include <thread>
#include <chrono>

// CDht11 구현을 Dht11Reader 내부로 이식
// - 타이밍: pigpio gpioTick() 사용
// - 비트 판정: (LowTime < HighTime) -> 1, else 0
// - 성공 시 tempC/hum은 high 바이트 기준(float 캐스팅)
// - DHT11 특성상 소수부(LL) 대부분 0이므로 high 바이트만 사용

bool Dht11Reader::init() {
    gpioSetMode(pin_, PI_OUTPUT);
    gpioWrite(pin_, 1); // idle high
    return true;
}

void Dht11Reader::send_start_signal() {
    gpioWrite(pin_, 0);
    std::this_thread::sleep_for(std::chrono::milliseconds(20)); // ≥18ms
    gpioWrite(pin_, 1);
    // 센서가 응답 준비하도록 수십 µs 후 입력으로 전환
}

int Dht11Reader::wait_for_low(int max_us) {
    uint32_t start = gpioTick();
    while (gpioRead(pin_) != 0) {
        if ((int)(gpioTick() - start) > max_us) {
            throw std::runtime_error("Timeout while waiting for LOW.");
        }
    }
    return (int)(gpioTick() - start);
}

int Dht11Reader::wait_for_high(int max_us) {
    uint32_t start = gpioTick();
    while (gpioRead(pin_) == 0) {
        if ((int)(gpioTick() - start) > max_us) {
            throw std::runtime_error("Timeout while waiting for HIGH.");
        }
    }
    return (int)(gpioTick() - start);
}

uint8_t Dht11Reader::calc_checksum(uint8_t hh, uint8_t hl, uint8_t th, uint8_t tl) {
    return static_cast<uint8_t>(hh + hl + th + tl);
}

std::optional<Dht11Data> Dht11Reader::process_data(uint64_t Data) {
    uint8_t HumidityHigh    = (Data >> 32) & 0xFF;
    uint8_t HumidityLow     = (Data >> 24) & 0xFF;
    uint8_t TemperatureHigh = (Data >> 16) & 0xFF;
    uint8_t TemperatureLow  = (Data >> 8)  & 0xFF;
    uint8_t Parity          = (Data)       & 0xFF;

    if (Parity != calc_checksum(HumidityHigh, HumidityLow, TemperatureHigh, TemperatureLow)) {
        return std::nullopt; // checksum fail
    }

    // DHT11은 소수부(LL)가 거의 0. 안전하게 High 바이트만 사용.
    Dht11Data out;
    out.hum   = static_cast<float>(HumidityHigh);
    out.tempC = static_cast<float>(TemperatureHigh);
    return out;
}

std::optional<Dht11Data> Dht11Reader::read_with_retry(int attempts,
                                                      int timeout_ms,
                                                      int cool_down_ms) {
    if (attempts < 1) attempts = 1;

    for (int i = 0; i < attempts; ++i) {
        auto r = read_once(timeout_ms);
        if (r) return r;

        // DHT11은 1Hz 제한 → 시도 사이 충분히 쉬어야 함
        // pigpio gpioDelay 단위는 us
        gpioDelay(cool_down_ms * 1000);
    }
    return std::nullopt;
}


std::optional<Dht11Data> Dht11Reader::read_once(int timeout_ms) {
    // pigpio는 µs 단위, 상한 보호
    const int MAX_US = std::max(1, timeout_ms) * 1000;

    // Start signal
    send_start_signal();
    gpioSetMode(pin_, PI_INPUT);
    gpioSetPullUpDown(pin_, PI_PUD_UP);

    uint64_t data = 0;

    try {
        // Sensor response: ~80us Low -> ~80us High -> ~Low (프리엠블)
        wait_for_low(MAX_US);
        wait_for_high(MAX_US);
        wait_for_low(MAX_US);

        // 40 bits
        for (int i = 0; i < 40; ++i) {
            data <<= 1;
            int LowTime  = wait_for_high(MAX_US); // 50us Low 구간
            int HighTime = wait_for_low(MAX_US);  // (0: ~26-28us, 1: ~70us)
            if (LowTime < HighTime) {
                data |= 0x1; // bit=1
            }
        }
        // 마무리 High로 돌아오는 구간 대기(선택)
        wait_for_high(MAX_US);
    } catch (...) {
        // 핀을 안전 상태로 복구
        gpioSetMode(pin_, PI_OUTPUT);
        gpioWrite(pin_, 1);
        return std::nullopt;
    }

    // 핀을 idle로 복구
    gpioSetMode(pin_, PI_OUTPUT);
    gpioWrite(pin_, 1);

    return process_data(data);
}
