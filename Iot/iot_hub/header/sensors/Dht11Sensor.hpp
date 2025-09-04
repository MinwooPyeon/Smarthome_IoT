// include/sensors/Dht11Sensor.hpp
#pragma once
#include "sensors/iSensor.hpp"
#include "interface/iGpio.hpp"
#include "util/sensorTypes.hpp"
#include <atomic>
#include <condition_variable>
#include <mutex>
#include <optional>
#include <vector>

namespace sensors {

class Dht11Sensor : public iSensor {
public:
    struct Options {
        std::string chip = "gpio"; // pigpio에선 의미 없음
        int pin = -1;              // BCM pin
        int min_interval_ms = 1100; // DHT11 권장 1s 이상
        int start_low_ms = 18;       // 스타트 신호(LOW, ms)
        int start_high_us = 30;      // 스타트 후 HIGH 유지(us) 20~40
    };

    Dht11Sensor(std::shared_ptr<iGpio> gpio, SensorConfig cfg, Options opt);
    ~Dht11Sensor() override { stop(); }

    // iSensor
    SensorState state() const override { return state_.load(); }
    const SensorConfig& config() const override { return cfg_; }
    bool initialize(Error* err = nullptr) override;
    bool start(Error* err = nullptr) override;
    void stop() override;
    std::optional<SensorReading> readOnce(Error* err = nullptr) override;

    void setReadingCallback(ReadingCallback cb) override { on_read_ = std::move(cb); }
    void setErrorCallback(ErrorCallback cb) override     { on_err_  = std::move(cb); }
    bool reset(Error* err = nullptr) override;

private:
    bool trigger_measurement(Error* err);
    void on_edge(bool level, uint32_t tick);
    void clear_rx_state();
    bool decode_and_publish(Error* err);

private:
    std::shared_ptr<iGpio> gpio_;
    SensorConfig           cfg_;
    Options                opt_;
    std::atomic<SensorState> state_{SensorState::Uninitialized};

    ReadingCallback on_read_{};
    ErrorCallback   on_err_{};

    std::mutex rx_m_;
    std::vector<uint32_t> high_us_; // HIGH 구간(us) 기록
    std::atomic<bool> receiving_{false};
    std::atomic<uint32_t> last_tick_{0};
    std::atomic<bool> last_level_high_{false};

    // readOnce 임시 저장
    std::mutex once_m_;
    std::optional<SensorReading> last_reading_;
};

} // namespace sensors
