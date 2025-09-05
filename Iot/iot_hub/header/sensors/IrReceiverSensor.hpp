// include/sensors/IrReceiverSensor.hpp
#pragma once
#include "sensors/iSensor.hpp"
#include "interface/iGpio.hpp"
#include "util/sensorTypes.hpp"
#include <atomic>
#include <mutex>
#include <optional>
#include <vector>

namespace sensors {

class IrReceiverSensor : public iSensor {
public:
    struct Options {
        std::string chip = "gpio";
        int pin = -1;   // BCM
        int tol = 200;  // us 허용 오차
    };

    IrReceiverSensor(std::shared_ptr<iGpio> gpio, SensorConfig cfg, Options opt);
    ~IrReceiverSensor() override { stop(); }

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
    void on_edge(bool level, uint32_t tick);
    void clear_decoder();
    bool try_decode_frame();

    static bool near(int v, int target, int tol) { return std::abs(v - target) <= tol; }

private:
    std::shared_ptr<iGpio> gpio_;
    SensorConfig           cfg_;
    Options                opt_;
    std::atomic<SensorState> state_{SensorState::Uninitialized};

    ReadingCallback on_read_{};
    ErrorCallback   on_err_{};

    std::mutex m_;
    uint32_t last_tick_{0};
    bool last_level_{true};
    std::vector<int> seq_; // LOW/HIGH 구간 길이(us) 교대로 저장

    std::optional<SensorReading> last_frame_;
};

} // namespace sensors
