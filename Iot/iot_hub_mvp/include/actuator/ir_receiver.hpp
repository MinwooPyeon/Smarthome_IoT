#pragma once
#include <optional>
#include <vector>
#include <functional>
#include <atomic>
#include <cstdint>

struct IrSample {
    std::vector<int> rawUs;
    int              gapUs{};
};

class IrReceiver {
public:
    IrReceiver(int pinBcm, int gapUs) : pin_(pinBcm), gapUs_(gapUs) {}
    bool init(int glitchUs);
    std::optional<IrSample> capture_once(int timeout_ms);
    void stop();

private:
    static void alertThunk(int gpio, int level, uint32_t tick, void* userdata);

    const int pin_ = 17;
    const int gapUs_;
    int       alertSet_{0};
    std::atomic<bool> running_{false};

    // 캡처 상태
    std::vector<int> seq_;
    uint32_t lastTick_{0};
    int lastLevel_{-1};
};
