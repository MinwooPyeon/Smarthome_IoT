// src/sensors/Dht11Sensor.cpp
#include "sensors/Dht11Sensor.hpp"
#include <thread>

using namespace sensors;

Dht11Sensor::Dht11Sensor(std::shared_ptr<iGpio> gpio, SensorConfig cfg, Options opt)
: gpio_(std::move(gpio)), cfg_(std::move(cfg)), opt_(opt) {}

bool Dht11Sensor::initialize(Error* err) {
    if (!gpio_) { if (err) err->message = "gpio null"; state_ = SensorState::Fault; return false; }
    int gerr{};
    if (!gpio_->open(opt_.chip, opt_.pin, PinMode::INPUT, &gerr)) {
        if (err) err->message = "gpio open(INPUT) failed, err=" + std::to_string(gerr);
        state_ = SensorState::Fault; return false;
    }
    state_ = SensorState::Ready;
    return true;
}

bool Dht11Sensor::start(Error* err) {
    if (state_ != SensorState::Ready && state_ != SensorState::Stopped) return true;
    int gerr{};
    if (!gpio_->watch(Edge::BOTH, [this](int /*pin*/, bool level, uint32_t tick){
        this->on_edge(level, tick);
    }, &gerr)) {
        if (err) err->message = "watch failed, err=" + std::to_string(gerr);
        state_ = SensorState::Fault; return false;
    }
    state_ = SensorState::Running;
    return true;
}

void Dht11Sensor::stop() {
    if (state_ == SensorState::Running || state_ == SensorState::Ready) {
        gpio_->unwatch();
        gpio_->close();
        state_ = SensorState::Stopped;
    }
}

bool Dht11Sensor::reset(Error* err) {
    stop();
    return initialize(err) && start(err);
}

void Dht11Sensor::clear_rx_state() {
    std::lock_guard lk(rx_m_);
    high_us_.clear();
    last_tick_ = 0;
    last_level_high_ = false;
}

bool Dht11Sensor::trigger_measurement(Error* err) {
    // OUTPUT으로 전환 → 18ms LOW → 20~40us HIGH → 입력/감시 복귀
    int gerr{};
    gpio_->unwatch();
    gpio_->close();
    if (!gpio_->open(opt_.chip, opt_.pin, PinMode::OUTPUT, &gerr)) {
        if (err) err->message = "open(OUTPUT) failed, err=" + std::to_string(gerr);
        return false;
    }
    if (!gpio_->write(false, &gerr)) { if (err) err->message = "write LOW failed"; return false; }
    std::this_thread::sleep_for(std::chrono::milliseconds(opt_.start_low_ms));
    if (!gpio_->write(true, &gerr))  { if (err) err->message = "write HIGH failed"; return false; }
    std::this_thread::sleep_for(std::chrono::microseconds(opt_.start_high_us));

    gpio_->close();
    if (!gpio_->open(opt_.chip, opt_.pin, PinMode::INPUT, &gerr)) {
        if (err) err->message = "reopen(INPUT) failed, err=" + std::to_string(gerr);
        return false;
    }
    if (!gpio_->watch(Edge::BOTH, [this](int /*pin*/, bool level, uint32_t tick){
        this->on_edge(level, tick);
    }, &gerr)) {
        if (err) err->message = "watch failed, err=" + std::to_string(gerr);
        return false;
    }
    clear_rx_state();
    receiving_ = true;
    return true;
}

void Dht11Sensor::on_edge(bool level, uint32_t tick) {
    uint32_t prev = last_tick_.exchange(tick);
    bool prev_high = last_level_high_.exchange(level);
    if (prev == 0) return;

    uint32_t dt = tick - prev;
    if (prev_high) { // 직전 구간이 HIGH였다면 길이를 기록
        std::lock_guard lk(rx_m_);
        high_us_.push_back(dt);

        // 충분히 쌓였으면 디코드 시도
        if (high_us_.size() >= 40 + 10) {
            Error e{};
            decode_and_publish(&e);
            receiving_ = false;
        }
    }
}

bool Dht11Sensor::decode_and_publish(Error* err) {
    std::vector<uint32_t> high;
    { std::lock_guard lk(rx_m_); high = high_us_; }
    if (high.size() < 40) { if (err) err->message = "too few pulses"; return false; }

    // 끝에서 40개 비트 구간 사용 (간단한 방법)
    std::vector<uint32_t> bits_us(high.end() - 40, high.end());
    uint8_t bytes[5]{};
    for (int i = 0; i < 40; ++i) {
        uint32_t us = bits_us[i];
        int bit = (us > 50) ? 1 : 0; // 50us 임계
        bytes[i/8] <<= 1;
        bytes[i/8] |= (bit & 1);
    }

    uint8_t sum = (uint8_t)(bytes[0] + bytes[1] + bytes[2] + bytes[3]);
    if (sum != bytes[4]) { if (err) err->message = "checksum mismatch"; return false; }

    // DHT11: 정수부만 유효
    SensorReading r{};
    r.name = cfg_.name;
    r.kind = cfg_.kind;
    r.ts_ms = now_ms();
    r.values["temperature_c"] = static_cast<double>(bytes[2]);
    r.values["humidity_rh"]   = static_cast<double>(bytes[0]);

    last_reading_ = r;
    if (on_read_) on_read_(r);
    return true;
}

std::optional<SensorReading> Dht11Sensor::readOnce(Error* err) {
    if (state_ != SensorState::Running) {
        if (err) err->message = "sensor not running";
        return std::nullopt;
    }
    if (!trigger_measurement(err)) return std::nullopt;

    // 간단 대기(200ms) 후 마지막 측정 사용
    std::this_thread::sleep_for(std::chrono::milliseconds(200));
    std::lock_guard lk(once_m_);
    auto out = last_reading_;
    last_reading_.reset();
    if (!out.has_value() && err) err->message = "timeout/no reading";
    return out;
}
