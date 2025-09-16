#include "interface/gpioPigpio.hpp"
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
    
    
    if (mode == PinMode::INPUT) {
        gpioSetPullUpDown(pin_, PI_PUD_UP); // ✅ 풀업 필수
        gpioGlitchFilter(pin_, 5);         // 5~20us 정도 권장
    } else {
        gpioSetPullUpDown(pin_, PI_PUD_OFF);
        gpioGlitchFilter(pin_, 0);
    }
    
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
    if (!opened_) { if (err) *err = -1; return false; }

    edge_ = edge;
    cb_ = std::move(cb);

    // 현재 레벨 저장 (에지 판별 기준)
    int cur = gpioRead(pin_);
    last_level_ = (cur < 0) ? -1 : cur;

    // 알림 콜백 등록 (엣지 타입은 pigpio가 Alert로 모두 줌)
    gpioSetAlertFuncEx(pin_, &GpioPigpio::onAlert, this);

    // (선택) 글리치 필터: 바운스/노이즈 제거
    // gpioGlitchFilter(pin_, 5); // 5us 등 상황에 맞게

    if (err) *err = 0;
    return true;
}

void GpioPigpio::unwatch() {
    if (!opened_) return;

    // ✅ alert 콜백 해제 (정확한 시그니처: (pin, func, userdata))
    gpioSetAlertFuncEx(pin_, nullptr, nullptr);

    // ✅ ISR 콜백도 사용했다면 같이 해제 (f=nullptr이면 취소됨)
    gpioSetISRFuncEx(pin_, EITHER_EDGE, 0, nullptr, nullptr);

    cb_ = nullptr;
    edge_ = Edge::NONE;
    last_level_.store(-1);
}

void GpioPigpio::onAlert(int gpio, int level, uint32_t tick, void* userdata) {
    auto* self = static_cast<GpioPigpio*>(userdata);
    if (!self) return;
    if (!self->cb_) return;
    if (level == PI_TIMEOUT) return; // 타임아웃 이벤트는 무시

    // 에지 필터링 (선택): 이전 레벨과 비교
    int prev = self->last_level_.exchange(level);

    if (self->edge_ == Edge::NONE) return;
    if (self->edge_ == Edge::RISING   && !(prev == PI_LOW  && level == PI_HIGH)) return;
    if (self->edge_ == Edge::FALLING  && !(prev == PI_HIGH && level == PI_LOW )) return;
    // BOTH 는 항상 통과

    const bool high = (level == PI_HIGH);
    // ✅ 인자 3개 전달해서 호출!
    self->cb_(gpio, high, tick);
}


} // namespace sensors
