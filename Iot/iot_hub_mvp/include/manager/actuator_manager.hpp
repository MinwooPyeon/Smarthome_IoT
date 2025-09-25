#pragma once

#include "actuator/dht11_reader.hpp"
#include "actuator/ir_receiver.hpp"
#include "manager/port.hpp"
#include "config.hpp"
#include "types.hpp"

#include <functional>
#include <chrono>
#include <mutex>
#include <thread>


namespace manager{
    class ActuatorManager : public IEnvSource{
public:
    using EnvCallback = std::function<void(const Dht11Data&)>;
    using Clock = std::chrono::steady_clock;

    explicit ActuatorManager(const ActuatorConfig& cfg);

    // 공통 초기화/정리
    bool init() override;          // DHT11 idle 설정, IR 입력/글리치 필터 설정
    void shutdown();      // 백그라운드 중지 포함

    // 동기 API
    std::optional<Dht11Data> read_env_with_retry();          // DHT11 1회 프레임
    std::optional<IrSample>  capture_ir_once(int timeout_ms);// IR 프레임 1회 캡처

    // 백그라운드 환경 폴링
    bool start_env_loop(std::chrono::milliseconds interval, EnvCallback cb) override;
    void stop_env_loop() override;

    // IR 파라미터 재설정
    bool set_ir_glitch_us(int glitchUs) override; // 재적용 위해 init 재호출
    void set_ir_gap_us(int gapUs) override;       // 다음 캡처부터 적용(객체 내부 gap 유지)

    // 접근자
    int  ir_gap_us()    const { return cfg_.irGapUs; }
    int  ir_glitch_us() const { return cfg_.irGlitchUs; }

private:
    ActuatorConfig cfg_;
    Dht11Reader    dht_;
    IrReceiver     ir_;

    // env polling
    std::atomic<bool> polling_{false};
    std::thread       envThread_;
    std::mutex        mu_; // 동시 호출 보호

    void env_loop(std::chrono::milliseconds interval, EnvCallback cb);
};

}