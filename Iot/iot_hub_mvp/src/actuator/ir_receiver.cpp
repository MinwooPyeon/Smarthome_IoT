#include "actuator/ir_receiver.hpp"
#include <pigpio.h>
#include <chrono>
#include <thread>
#include <iostream>

static inline int tick_diff(uint32_t newer, uint32_t older) {
    // unsigned wrap-around-safe
    return static_cast<int>(newer - older);
}

bool IrReceiver::init(int glitchUs){
    if (pin_ < 0 || pin_ > 53) {
        std::cerr << "[IR] bad pin number: " << pin_ << "\n";
        return false;
    }

    int rc = gpioSetMode(pin_, PI_INPUT);
    if (rc != 0) { std::cerr << "[IR] gpioSetMode rc="<<rc<<" pin="<<pin_<<"\n"; return false; }

    // VS1838B 출력은 풀업 필요(내부 풀업으로 충분)
    rc = gpioSetPullUpDown(pin_, PI_PUD_UP);
    if (rc != 0) { std::cerr << "[IR] gpioSetPullUpDown rc="<<rc<<" pin="<<pin_<<"\n"; return false; }

    rc = gpioGlitchFilter(pin_, glitchUs); // e.g., 50us
    if (rc != 0) { std::cerr << "[IR] gpioGlitchFilter rc="<<rc<<" pin="<<pin_<<"\n"; return false; }

    // 초기 상태 스냅샷
    int v = gpioRead(pin_);
    if (v < 0) { std::cerr << "[IR] gpioRead init rc="<<v<<" pin="<<pin_<<"\n"; return false; }
    lastLevel_ = v;
    lastTick_  = gpioTick();
    return true;
}

void IrReceiver::alertThunk(int gpio, int level, uint32_t tick, void* userdata){
    auto* self = static_cast<IrReceiver*>(userdata);
    if (!self || !self->running_) return;

    if (self->lastLevel_ == -1) { // 첫 샘플
        self->lastLevel_ = level;
        self->lastTick_  = tick;
        return;
    }

    if (level != self->lastLevel_) {
        int dt = tick_diff(tick, self->lastTick_); // us
        self->seq_.push_back(dt);
        self->lastTick_  = tick;
        self->lastLevel_ = level;

        // 프레임 경계 감지: 긴 무신호(gap) 도달
        if (dt >= self->gapUs_) {
            // gap 항목은 제거
            if (!self->seq_.empty()) self->seq_.pop_back();
            self->running_ = false;
        }
    }
}

std::optional<IrSample> IrReceiver::capture_once(int timeout_ms){
    using namespace std::chrono;

    // 방어: pigpio 초기화/핀 범위
    if (pin_ < 0 || pin_ > 53) {
        std::cerr << "[IR] capture_once bad pin "<<pin_<<"\n";
        return std::nullopt;
    }
    // 상태 초기화
    seq_.clear();
    lastLevel_ = gpioRead(pin_);
    lastTick_  = gpioTick();

    running_ = true;
    gpioSetAlertFuncEx(pin_, &IrReceiver::alertThunk, this);
    alertSet_ = 1;

    const auto deadline = steady_clock::now() + milliseconds(std::max(1, timeout_ms));

    // 대기 루프: running_이 false가 되거나 timeout
    while (running_ && steady_clock::now() < deadline) {
        std::this_thread::sleep_for(milliseconds(1));
    }

    // 종료 정리
    stop();

    if (!seq_.empty()) {
        IrSample f; f.rawUs = std::move(seq_); f.gapUs = gapUs_;
        return f;
    }
    return std::nullopt; // timeout or noise
}

void IrReceiver::stop(){
    if (alertSet_) {
        gpioSetAlertFunc(pin_, nullptr);
        alertSet_ = 0;
    }
    running_ = false;
}
