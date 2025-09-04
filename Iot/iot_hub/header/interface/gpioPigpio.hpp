#pragma once
#include "interface/iGpio.hpp"
#include <atomic>
#include <mutex>

namespace sensors {

class GpioPigpio final : public iGpio {
public:
    GpioPigpio() = default;
    ~GpioPigpio() override { close(); }

    bool open(const std::string& chip, int line, PinMode mode, int* err = nullptr) override;
    void close() override;

    bool read(bool* level, int* err = nullptr) override;
    bool write(bool level, int* err = nullptr) override;

    bool watch(Edge edge, EdgeCallback cb, int* err = nullptr) override;
    void unwatch() override;

private:
    // pigpio 콜백
    static void onAlert(int gpio, int level, uint32_t tick, void* userdata);

    // 변환 유틸 (헤더에 pigpio.h 노출 안 하려고 분리)
    static int toPigpioMode(PinMode m);
    static int toPigpioEdge(Edge e);

private:
    int  pin_{-1};             // BCM 번호
    bool opened_{false};
    Edge edge_{Edge::NONE};
    EdgeCallback cb_{};
    std::atomic<int> last_level_{-1}; // 첫 샘플 구분용

    // 전역 초기화 ref-count
    static std::mutex s_init_m_;
    static std::atomic<int> s_refcount_;
};

} // namespace sensors
