#pragma once
#include <chrono>
#include <cstdint>
#include <optional>
#include <string>
#include <unordered_map>
#include <vector>

namespace sensors {

enum class SensorState { Uninitialized, Ready, Running, Stopped, Fault };

struct Error {
    int code = 0;
    std::string message;
};

struct SensorConfig {
    std::string name;              // 고유 이름 (manager 키)
    std::string kind;              // "dht11", "vs1838" 등
    int         pin = -1;          // BCM pin
    int         sample_interval_ms = 1000; // DHT11 등 폴링 주기(기본 1s)
};

// 읽기 결과: key/value 중심. 필요시 text에 부가정보 저장.
struct SensorReading {
    std::string name;
    std::string kind;
    int64_t     ts_ms = 0; // epoch milliseconds
    std::unordered_map<std::string, double> values;
    std::optional<std::string> text;
};

inline int64_t now_ms() {
    using namespace std::chrono;
    return duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count();
}

} // namespace sensors
