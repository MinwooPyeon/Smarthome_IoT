#include "manager/actuator_manager.hpp"
#include <iostream>

namespace manager {

ActuatorManager::ActuatorManager(const ActuatorConfig& cfg)
: cfg_(cfg),
  dht_(cfg_.dhtPinBcm),
  ir_(cfg_.irPinBcm, cfg_.irGapUs) // IrReceiver(pin, gapUs)
{}

bool ActuatorManager::init() {
    std::lock_guard<std::mutex> lk(mu_);
    // DHT11: idle high 설정
    if (!dht_.init()) {
        std::cerr << "[Actuator] DHT11 init failed\n";
        return false;
    }
    // IR: 입력모드 + glitch filter 적용
    if (!ir_.init(cfg_.irGlitchUs)) {
        std::cerr << "[Actuator] IR init failed\n";
        return false;
    }
    return true;
}

void ActuatorManager::shutdown() {
    stop_env_poll();
    // 개별 드라이버는 GPIO 핀을 안전 상태로 되돌림(Dht11Reader::read_once 내부 등)
}

std::optional<Dht11Data> ActuatorManager::read_env_with_retry() {
    std::lock_guard<std::mutex> lk(mu_);
    return dht_.read_with_retry(cfg_.dhtAttempts, cfg_.dhtTimeoutMs, cfg_.dhtCooldownMs);
}

std::optional<IrSample> ActuatorManager::capture_ir_once(int timeout_ms) {
    std::lock_guard<std::mutex> lk(mu_);
    return ir_.capture_once(timeout_ms);
}

bool ActuatorManager::start_env_poll(std::chrono::milliseconds interval, EnvCallback cb) {
    if (polling_.exchange(true)) return false; // 이미 동작중
    envThread_ = std::thread([this, interval, cb]{ env_loop(interval, cb); });
    return true;
}

void ActuatorManager::stop_env_poll() {
    if (!polling_.exchange(false)) return;
    if (envThread_.joinable()) envThread_.join();
}

bool ActuatorManager::set_ir_glitch_us(int glitchUs) {
    std::lock_guard<std::mutex> lk(mu_);
    cfg_.irGlitchUs = glitchUs;
    return ir_.init(cfg_.irGlitchUs); // 재적용
}

void ActuatorManager::set_ir_gap_us(int gapUs) {
    std::lock_guard<std::mutex> lk(mu_);
    cfg_.irGapUs = gapUs;
    // 현재 IrReceiver는 gapUs를 내부에 보관. 다음 캡처부터 반영되게 하려면
    // 새 객체로 재구성하거나, IrReceiver에 setter를 추가하세요.
    ir_ = IrReceiver(cfg_.irPinBcm, cfg_.irGapUs);
    (void)ir_.init(cfg_.irGlitchUs);
}

void ActuatorManager::env_loop(std::chrono::milliseconds interval, EnvCallback cb) {
    using namespace std::chrono;
    auto next = Clock::now();
    while (polling_.load()) {
        next += interval;
        if (auto r = read_env_with_retry()) {
            if (cb) cb(*r);
        }
        std::this_thread::sleep_until(next);
    }
}

}
