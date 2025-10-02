#pragma once
#include <optional>
#include <cstdint>
#include <types.hpp>

class Dht11Reader {
public:
    // BCM 핀 번호
    explicit Dht11Reader(int bcmPin) : pin_(bcmPin) {}

    // 센서 초기화(Idle: High)
    bool init();

    // 한 번 읽기 (timeout_ms 내에 실패 시 std::nullopt)
    std::optional<EnvSample> read_once(int timeout_ms = 1500);
    std::optional<EnvSample> read_with_retry(int attempts = 3,
                                         int timeout_ms = 1500,
                                         int cool_down_ms = 1200);

private:
    int pin_;

    // CDht11 논리 기반 유틸
    int wait_for_low(int max_us);
    int wait_for_high(int max_us);
    void send_start_signal();

    static uint8_t calc_checksum(uint8_t hh, uint8_t hl, uint8_t th, uint8_t tl);

    // 40비트 raw를 해석해 값 리턴 (체크섬 불일치 시 nullopt)
    std::optional<EnvSample> process_data(uint64_t data);
};
