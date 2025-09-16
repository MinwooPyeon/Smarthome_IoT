// src/sensors/Dht11Sensor.cpp
#include "sensors/Dht11Sensor.hpp"
#include <chrono>
#include <thread>
#include <optional>
#include <vector>
#include <string>
#include <cstdint>
#include <iostream>   // ✅ 디버그 출력
using namespace sensors;
using namespace std::chrono;

// ===== 디버그 스위치 =====
#ifndef DHT11_DEBUG
#define DHT11_DEBUG 1   // ✅ 디버그 ON
#endif
#if DHT11_DEBUG
  #define DHTDBG(x) do { std::cout << "[DHT11] " << x << std::endl; } while(0)
#else
  #define DHTDBG(x) do {} while(0)
#endif

static inline int64_t now_ms() {
    return duration_cast<milliseconds>(steady_clock::now().time_since_epoch()).count();
}
static inline uint64_t tick_us() {
    return duration_cast<microseconds>(steady_clock::now().time_since_epoch()).count();
}

// ====== DHT11: iGpio 폴링 원샷 (디버그 강화) ======
static bool dht11_poll_once_igpio(iGpio& gpio, const std::string& chip, int line,
                                  int start_low_ms, int start_high_us,
                                  int& outT, int& outH, std::string& err)
{
    // 타이밍 상향(안정)
    if (start_low_ms  < 22) start_low_ms  = 22;   // 22ms
    if (start_high_us < 80) start_high_us = 80;   // 80us

    int gerr = 0;
    bool is_open = false;
    auto ensure_close = [&]{ if (is_open) { gpio.close(); is_open = false; } };

    DHTDBG("pin=" << line << ", start_low_ms=" << start_low_ms
           << ", start_high_us=" << start_high_us);

    // 0) OUTPUT으로 열고 유휴 HIGH 보장
    if (!gpio.open(chip, line, PinMode::OUTPUT, &gerr)) {
        err = "open(OUTPUT) failed, err=" + std::to_string(gerr); return false;
    }
    is_open = true;

    if (!gpio.write(true, &gerr)) { err = "write HIGH(init) failed"; ensure_close(); return false; }
    std::this_thread::sleep_for(std::chrono::milliseconds(1)); // settle
    DHTDBG("start: drive LOW...");
    if (!gpio.write(false, &gerr)) { err = "write LOW(start) failed"; ensure_close(); return false; }
    std::this_thread::sleep_for(std::chrono::milliseconds(start_low_ms));

    DHTDBG("start: release to HIGH for " << start_high_us << "us");
    if (!gpio.write(true, &gerr)) { err = "write HIGH(post-start) failed"; ensure_close(); return false; }
    std::this_thread::sleep_for(std::chrono::microseconds(start_high_us));

    // 1) INPUT으로 재오픈 (GpioPigpio 쪽에서 풀업/글리치필터 적용됨)
    ensure_close();
    if (!gpio.open(chip, line, PinMode::INPUT, &gerr)) {
        err = "reopen(INPUT) failed, err=" + std::to_string(gerr); return false;
    }
    is_open = true;
    // 전환/풀업 settle
    std::this_thread::sleep_for(std::chrono::microseconds(10));

    // ✅ 아이들 레벨 샘플링(20회)
    {
        bool vv=false; int gg=0; int ones=0, zeros=0;
        for (int k=0; k<20; ++k) {
            if (!gpio.read(&vv, &gg)) { err = "read() failed after INPUT"; ensure_close(); return false; }
            ones += vv ? 1 : 0; zeros += vv ? 0 : 1;
            std::this_thread::sleep_for(std::chrono::microseconds(20));
        }
        DHTDBG("idle sample: HIGH=" << ones << ", LOW=" << zeros
               << " (유휴는 HIGH가 되어야 정상; 전부 HIGH면 OK)");
    }

    auto wait_level = [&](bool wantHigh, uint64_t timeout_us)->bool {
        const uint64_t t0 = tick_us();
        bool v = false;
        while (true) {
            if (!gpio.read(&v, &gerr)) return false;
            if (v == wantHigh) return true;
            if (tick_us() - t0 > timeout_us) return false;
        }
    };

    // 2) 센서 응답 헤더: LOW(~80us) → HIGH(~80us) → LOW
    DHTDBG("wait sensor response: initial LOW");
    if (!wait_level(false, 3000)) { err = "no initial LOW"; ensure_close(); return false; }
    DHTDBG("got initial LOW");
    if (!wait_level(true,  400))  { err = "no initial HIGH"; ensure_close(); return false; }
    DHTDBG("got initial HIGH");
    if (!wait_level(false, 400))  { err = "no pre-bit LOW";  ensure_close(); return false; }
    DHTDBG("got pre-bit LOW, reading 40 bits...");

    // 3) 40비트
    uint8_t b[5]{}; // hum.int, hum.dec, temp.int, temp.dec, checksum
    for (int i=0; i<40; ++i) {
        if (!wait_level(false, 400)) { err = "bit start LOW timeout"; ensure_close(); return false; }
        if (!wait_level(true,  400)) { err = "bit rise timeout";      ensure_close(); return false; }
        const uint64_t th0 = tick_us();
        if (!wait_level(false, 400)) { err = "bit fall timeout";       ensure_close(); return false; }
        const uint64_t th = tick_us() - th0;     // HIGH 길이
        const int bit = (th > 50) ? 1 : 0;       // 필요시 45/55로 조정
        b[i/8] = static_cast<uint8_t>((b[i/8] << 1) | bit);

        if ((i % 8) == 7) DHTDBG("byte" << (i/8) << " so far");
    }

    // 4) 체크섬
    if (static_cast<uint8_t>(b[0]+b[1]+b[2]+b[3]) != b[4]) {
        err = "checksum mismatch"; ensure_close(); return false;
    }
    outH = b[0]; outT = b[2];
    DHTDBG("OK: T=" << outT << "C, H=" << outH << "%");
    ensure_close();
    return true;
}

// ====== 클래스 구현 (핵심만 발췌) ======
Dht11Sensor::Dht11Sensor(std::shared_ptr<iGpio> gpio, SensorConfig cfg, Options opt)
: gpio_(std::move(gpio)), cfg_(std::move(cfg)), opt_(opt) {}

bool Dht11Sensor::initialize(Error* err) {
    if (!gpio_) { if (err) err->message="gpio null"; state_=SensorState::Fault; return false; }
    int gerr{};
    // 입력으로 한 번 열어 두지만, readOnce에서 모드 전환을 자체 수행
    if (!gpio_->open(opt_.chip, opt_.pin, PinMode::INPUT, &gerr)) {
        if (err) err->message = "gpio open(INPUT) failed, err=" + std::to_string(gerr);
        state_=SensorState::Fault; return false;
    }
    state_ = SensorState::Ready;
    return true;
}
bool Dht11Sensor::start(Error*) {
    if (state_==SensorState::Ready || state_==SensorState::Stopped) state_=SensorState::Running;
    return true;
}
void Dht11Sensor::stop() {
    if (state_==SensorState::Running || state_==SensorState::Ready) {
        gpio_->unwatch(); gpio_->close(); state_=SensorState::Stopped;
    }
}
bool Dht11Sensor::reset(Error* err){ stop(); return initialize(err)&&start(err); }

std::optional<SensorReading> Dht11Sensor::readOnce(Error* err) {
    if (state_ != SensorState::Running && state_ != SensorState::Ready) {
        if (err) err->message = "sensor not running"; return std::nullopt;
    }
    int tC=0, h=0; std::string e;
    if (!dht11_poll_once_igpio(*gpio_, opt_.chip, opt_.pin,
                               opt_.start_low_ms, opt_.start_high_us, tC, h, e)) {
        if (err) err->message = e.empty() ? "timeout/no reading" : e;
        return std::nullopt;
    }
    SensorReading r{};
    r.name  = cfg_.name;
    r.kind  = cfg_.kind;
    r.ts_ms = now_ms();
    r.values["temperature_c"] = (double)tC;
    r.values["humidity_rh"]   = (double)h;
    if (on_read_) on_read_(r);
    return r;
}
