#include "interface/GpioPigpio.hpp"
#include <pigpio.h>

namespace sensors {

// 정적 멤버 정의
std::mutex GpioPigpio::s_init_m_;
std::atomic<int> GpioPigpio::s_refcount_{0};

int GpioPigpio::toPigpioMode(PinMode m) {
    return (m == PinMode::INPUT) ? PI_INPUT : PI_OUTPUT;
}

int GpioPigpio::toPigpioEdge(Edge e) {
    switch (e) {
        case Edge::RISING:  return RISING_EDGE;
        case Edge::FALLING: return FALLING_EDGE;
        case Edge::BOTH:    return EITHER_EDGE;
        case Edge::NONE:
        default:            return EITHER_EDGE; // 사용 안 함
    }
}

bool GpioPigpio::open(const std::string& chip, int line, PinMode mode, int* err) {
    // 전역 초기화 (여러 인스턴스 대비 ref-count)
    {
        std::lock_guard<std::mutex> lk(s_init_m_);
        if (s_refcount_.load() == 0) {
            int rc = gpioInitialise();
            if (rc < 0) { if (err) *err = rc; return false; }
        }
        s_refcount_.fetch_add(1);
    }

    pin_ = line; // BCM 번호
    int rc = gpioSetMode(pin_, toPigpioMode(mode));
    if (rc != 0) { if (err) *err = rc; return false; }

    opened_ = true;
    return true;
}

void GpioPigpio::close() {
    unwatch();
    if (opened_) {
        // 옵션: 입력으로 되돌리기
        gpioSetMode(pin_, PI_INPUT);
        opened_ = false;
    }
    // 전역 종료
    {
        std::lock_guard<std::mutex> lk(s_init_m_);
        if (s_refcount_.load() > 0 && s_refcount_.fetch_sub(1) == 1) {
            gpioTerminate();
        }
    }
}

bool GpioPigpio::read(bool* level, int* err) {
    if (!opened_) { if (err) *err = PI_NOT_INITIALISED; return false; }
    int v = gpioRead(pin_);
    if (v < 0) { if (err) *err = v; return false; }
    *level = (v != 0);
    return true;
}

bool GpioPigpio::write(bool level, int* err) {
    if (!opened_) { if (err) *err = PI_NOT_INITIALISED; return false; }
    int rc = gpioWrite(pin_, level ? 1 : 0);
    if (rc != 0) { if (err) *err = rc; return false; }
    return true;
}

bool GpioPigpio::watch(Edge edge, EdgeCallback cb, int* err) {
    if (!opened_) { if (err) *err = PI_NOT_INITIALISED; return false; }

    // 기존 ISR 해제
    unwatch();

    edge_ = edge;
    cb_   = std::move(cb);
    last_level_.store(-1);

    // pigpio ISR 등록: (gpio, edge, timeout(ms), func, userdata)
    int rc = gpioSetISRFuncEx(pin_, toPigpioEdge(edge), 0, &GpioPigpio::onAlert, this);
    if (rc != 0) {
        if (err) *err = rc;
        cb_ = nullptr;
        edge_ = Edge::NONE;
        return false;
    }
    return true;
}

void GpioPigpio::unwatch() {
    if (!opened_) return;
    // 등록 해제: func=nullptr 전달
    gpioSetISRFuncEx(pin_, EITHER_EDGE, 0, nullptr, nullptr);
    cb_ = nullptr;
    edge_ = Edge::NONE;
    last_level_.store(-1);
}

/* static */ void GpioPigpio::onAlert(int gpio, int level, uint32_t /*tick*/, void* userdata) {
    auto* self = static_cast<GpioPigpio*>(userdata);
    if (!self || gpio != self->pin_) return;

    if (level == PI_TIMEOUT) return; // watchdog 타임아웃 이벤트 무시

    // 엣지 필터링
    int prev = self->last_level_.exchange(level);
    if (prev == -1) {
        // 첫 샘플에서는 이전 상태가 없으므로 just record
        return;
    }
    bool rising  = (prev == 0 && level == 1);
    bool falling = (prev == 1 && level == 0);

    bool fire = false;
    switch (self->edge_) {
        case Edge::BOTH:    fire = (rising || falling); break;
        case Edge::RISING:  fire = rising;  break;
        case Edge::FALLING: fire = falling; break;
        case Edge::NONE:    fire = false;   break;
    }

    if (fire && self->cb_) self->cb_(); // 콜백은 pigpio 내부 스레드에서 호출됨
}

} // namespace sensors
