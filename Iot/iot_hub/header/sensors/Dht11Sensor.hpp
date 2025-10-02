#pragma once
#include "util/sensorTypes.hpp"   // SensorReading, SensorConfig, SensorState, Error
#include "sensors/iSensor.hpp"     // 질문에 올려준 인터페이스 헤더(경로는 프로젝트에 맞게)
#include <pigpio.h>

#include <atomic>
#include <chrono>
#include <cstdint>
#include <functional>
#include <memory>
#include <mutex>
#include <optional>
#include <thread>
#include <tuple>

namespace sensors {

class Dht11Sensor final : public iSensor {
public:
    struct Options {
        uint8_t gpioPin = 4;                 // 기본 BCM4
        std::chrono::milliseconds period{2000}; // 주기 측정 (DHT11 스펙상 1Hz 근처 권장)
        bool ownPigpioLifecycle = false;     // true면 내부에서 gpioInitialise/gpioTerminate
    };

    Dht11Sensor(const SensorConfig& cfg, const Options& opt);
    ~Dht11Sensor() override;

    // 구성 & 메타
    SensorState state() const override { return state_; }
    const SensorConfig& config() const override { return cfg_; }

    // 생명주기
    bool initialize(Error* err = nullptr) override;
    bool start(Error* err = nullptr) override;
    void stop() override;

    // 동기 1회 읽기
    std::optional<SensorReading> readOnce(Error* err = nullptr) override;

    // 콜백 등록(비동기)
    void setReadingCallback(ReadingCallback cb) override;
    void setErrorCallback(ErrorCallback cb) override;

    bool reset(Error* err = nullptr) override;

private:
    // DHT11 로우레벨
    void sendStartSignal();
    int  waitForLow(int timeout_us);
    int  waitForHigh(int timeout_us);
    std::tuple<uint8_t,uint8_t> measureRawTH(Error* err); // (tempC, hum%)

    // 쓰레드 루프
    void runLoop();

    // 내부 헬퍼
    static uint8_t calcParity(uint8_t hH, uint8_t hL, uint8_t tH, uint8_t tL);
    static uint64_t nowMicros();

private:
    SensorConfig   cfg_{};
    Options        opt_{};
    std::atomic<SensorState> state_{SensorState::Created};

    std::mutex     cbMutex_;
    ReadingCallback readingCb_;
    ErrorCallback   errorCb_;

    std::thread    worker_;
    std::atomic<bool> running_{false};

    bool pigpioInitedHere_{false}; // ownPigpioLifecycle=true일 때만 true가 됨
};

} // namespace sensors

