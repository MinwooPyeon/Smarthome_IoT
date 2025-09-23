// Timer.cpp
#include "pch.h"
#include "Timer.h"
#include <chrono>
#include <thread>
#include <atomic>
#include <cstdio>

static void TimerTrace(const wchar_t* fmt, ...) {
    wchar_t buf[512];
    va_list ap; va_start(ap, fmt);
    _vsnwprintf_s(buf, _TRUNCATE, fmt, ap);
    va_end(ap);
    OutputDebugStringW(buf); OutputDebugStringW(L"\r\n");
}

void Timer::start(double hz, std::function<void()> fn) {
    stop();
    run_.store(true, std::memory_order_release);

    const auto period = std::chrono::milliseconds((int)std::lround(1000.0 / hz));

    // ¡Ú [this]·Î ¸í½Ã Ä¸Ã³ + fnÀº °ª Ä¸Ã³
    th_ = std::thread([this, fn, period] {
        TimerTrace(L"[Timer] thread start (period=%lldms)", (long long)period.count());
        while (run_.load(std::memory_order_acquire)) {
            const auto t0 = std::chrono::steady_clock::now();
            try {
                fn(); // ¡Ú ¿¹¿Ü º¸È£
            }
            catch (const std::exception& e) {
                TimerTrace(L"[Timer] fn() exception: %S", e.what());
                // ¿¹¿Ü°¡ ¹Ýº¹µÇ¸é ÆøÁÖÇÏ¹Ç·Î Àá±ñ ½°
                std::this_thread::sleep_for(std::chrono::milliseconds(50));
            }
            catch (...) {
                TimerTrace(L"[Timer] fn() unknown exception");
                std::this_thread::sleep_for(std::chrono::milliseconds(50));
            }
            const auto dt = std::chrono::steady_clock::now() - t0;
            if (dt < period) std::this_thread::sleep_for(period - dt);
        }
        TimerTrace(L"[Timer] thread exit");
        });
}

void Timer::stop() {
    if (run_.exchange(false, std::memory_order_acq_rel)) {
        if (th_.joinable()) th_.join();
    }
}
