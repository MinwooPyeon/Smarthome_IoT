#include "sensors/Dht11Sensor.hpp"
#include <stdexcept>
#include <cstring>

namespace sensors {

using namespace std::chrono_literals;

// 타임아웃 상수(마이크로초)
static constexpr int TIMEOUT_US = 1000000; // 1s (충분히 큼)
static constexpr int FRAME_TIMEOUT_US = 100000; // 프레임 내 플립 대기 (100ms)
static constexpr int BIT_COUNT = 40;

static inline void setErrorMsg(Error* err, const std::string& msg, int code = 0) {
    if (!err) return;
    // TODO: Error 타입 정의에 맞게 설정하세요.
    // 예시:
    err->message = msg;
    err->code = code;
}

Dht11Sensor::Dht11Sensor(const SensorConfig& cfg, const Options& opt)
: cfg_(cfg), opt_(opt) {}

Dht11Sensor::~Dht11Sensor() {
    stop();
    if (opt_.ownPigpioLifecycle && pigpioInitedHere_) {
        gpioTerminate();
        pigpioInitedHere_ = false;
    }
}

bool Dht11Sensor::initialize(Error* err) {
    if (state_ != SensorState::Created && state_ != SensorState::Fault) {
        return true;
    }

    if (opt_.ownPigpioLifecycle) {
        int rc = gpioInitialise();
        if (rc < 0) {
            setErrorMsg(err, "pigpio initialize failed", rc);
            state_ = SensorState::Fault;
            return false;
        }
        pigpioInitedHere_ = true;
    }

    // DHT11의 아이들 상태: 출력 HIGH
    gpioSetMode(opt_.gpioPin, PI_OUTPUT);
    gpioWrite(opt_.gpioPin, 1);

    state_ = SensorState::Ready;
    return true;
}

bool Dht11Sensor::start(Error* err) {
    if (state_ != SensorState::Ready && state_ != SensorState::Stopped) {
        return true;
    }
    running_ = true;
    worker_ = std::thread(&Dht11Sensor::runLoop, this);
    state_ = SensorState::Running;
    (void)err;
    return true;
}

void Dht11Sensor::stop() {
    if (!running_) {
        if (state_ == SensorState::Running) state_ = SensorState::Stopped;
        return;
    }
    running_ = false;
    if (worker_.joinable()) worker_.join();
    if (state_ == SensorState::Running) state_ = SensorState::Stopped;
}

std::optional<SensorReading> Dht11Sensor::readOnce(Error* err) {
    if (state_ != SensorState::Ready && state_ != SensorState::Running && state_ != SensorState::Stopped) {
        setErrorMsg(err, "sensor not initialized");
        return std::nullopt;
    }

    try {
        auto [tC, h] = measureRawTH(err);

        SensorReading rd{}; // 기본 생성 가능한 전제
        // TODO: 프로젝트의 SensorReading 스키마에 맞춰 채우세요.
        // 아래는 흔히 쓰는 예시입니다.
        rd.type = "DHT11";
        rd.timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
                           std::chrono::system_clock::now().time_since_epoch()
                       ).count();
        rd.temperatureC = static_cast<double>(tC);
        rd.humidity = static_cast<double>(h);

        return rd;
    } catch (const std::exception& ex) {
        setErrorMsg(err, ex.what());
        return std::nullopt;
    }
}

// ----------------- 콜백 등록 -----------------
void Dht11Sensor::setReadingCallback(ReadingCallback cb) {
    std::lock_guard<std::mutex> lk(cbMutex_);
    readingCb_ = std::move(cb);
}
void Dht11Sensor::setErrorCallback(ErrorCallback cb) {
    std::lock_guard<std::mutex> lk(cbMutex_);
    errorCb_ = std::move(cb);
}

bool Dht11Sensor::reset(Error* err) {
    // 단순 재초기화 로직
    try {
        stop();
        state_ = SensorState::Created;
        return initialize(err);
    } catch (...) {
        setErrorMsg(err, "reset failed");
        state_ = SensorState::Fault;
        return false;
    }
}

// ----------------- 내부 구현: DHT11 프로토콜 -----------------
void Dht11Sensor::sendStartSignal() {
    // MCU -> DHT11 시작 신호: LOW 18~20ms, 이후 HIGH로 전환
    gpioSetMode(opt_.gpioPin, PI_OUTPUT);
    gpioWrite(opt_.gpioPin, 0);
    std::this_thread::sleep_for(20ms);
    gpioWrite(opt_.gpioPin, 1);
    // 버스 해제 후 입력으로 바꿔서 DHT 응답 대기
    gpioSetMode(opt_.gpioPin, PI_INPUT);
}

int Dht11Sensor::waitForLow(int timeout_us) {
    unsigned start = gpioTick();
    while (gpioRead(opt_.gpioPin)) {
        if ((int)(gpioTick() - start) > timeout_us) {
            throw std::runtime_error("timeout waiting LOW");
        }
    }
    return (int)(gpioTick() - start);
}

int Dht11Sensor::waitForHigh(int timeout_us) {
    unsigned start = gpioTick();
    while (!gpioRead(opt_.gpioPin)) {
        if ((int)(gpioTick() - start) > timeout_us) {
            throw std::runtime_error("timeout waiting HIGH");
        }
    }
    return (int)(gpioTick() - start);
}

std::tuple<uint8_t,uint8_t> Dht11Sensor::measureRawTH(Error* err) {
    // 시퀀스는 CDht11 로직과 동일 컨셉
    // 수신 비트 40개: HH, HL, TH, TL, Parity
    sendStartSignal();

    uint64_t data = 0;

    try {
        // DHT 응답 시퀀스: LOW(80us) -> HIGH(80us) -> 데이터 비트들
        waitForLow(FRAME_TIMEOUT_US);
        waitForHigh(FRAME_TIMEOUT_US);
        waitForLow(FRAME_TIMEOUT_US);

        for (int i = 0; i < BIT_COUNT; ++i) {
            data <<= 1;
            int lowTime  = waitForHigh(FRAME_TIMEOUT_US);
            int highTime = waitForLow(FRAME_TIMEOUT_US);
            // DHT11은 '1'이 더 긴 HIGH
            if (lowTime < highTime) {
                data |= 0x1ULL;
            }
        }

        waitForHigh(FRAME_TIMEOUT_US);
    } catch (...) {
        // 라인 원복
        gpioSetMode(opt_.gpioPin, PI_OUTPUT);
        gpioWrite(opt_.gpioPin, 1);
        throw; // 위에서 에러로 처리
    }

    // 라인 원복
    gpioSetMode(opt_.gpioPin, PI_OUTPUT);
    gpioWrite(opt_.gpioPin, 1);

    // 파싱
    uint8_t hH = (data >> 32) & 0xFF;
    uint8_t hL = (data >> 24) & 0xFF;
    uint8_t tH = (data >> 16) & 0xFF;
    uint8_t tL = (data >> 8)  & 0xFF;
    uint8_t p  =  data        & 0xFF;

    if (static_cast<uint8_t>(hH + hL + tH + tL) != p) {
        setErrorMsg(err, "DHT11 parity checksum failed");
        throw std::runtime_error("parity check failed");
    }

    // DHT11은 정수부만 유효 (소수부는 항상 0)
    uint8_t humidity    = hH;
    uint8_t temperature = tH;
    return {temperature, humidity};
}

uint8_t Dht11Sensor::calcParity(uint8_t hH, uint8_t hL, uint8_t tH, uint8_t tL) {
    return static_cast<uint8_t>(hH + hL + tH + tL);
}

uint64_t Dht11Sensor::nowMicros() {
    return static_cast<uint64_t>(gpioTick());
}

// ----------------- 쓰레드 루프 -----------------
void Dht11Sensor::runLoop() {
    // DHT11은 너무 자주 읽으면 오류가 잦음(≥1s 권장)
    const auto period = opt_.period.count() < 1000 ? 1000ms : opt_.period;

    while (running_) {
        Error err{};
        auto reading = readOnce(&err);
        {
            std::lock_guard<std::mutex> lk(cbMutex_);
            if (!reading) {
                if (errorCb_) errorCb_(err);
            } else {
                if (readingCb_) readingCb_(*reading);
            }
        }
        std::this_thread::sleep_for(period);
    }
}

} // namespace sensors

