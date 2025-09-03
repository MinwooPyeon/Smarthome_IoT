#pragma once
#include <chrono>
#include <cstdint>
#include <map>
#include <optional>
#include <string>
#include <variant>
#include <vector>

namespace sensors{

    using Clock = std::chrono::steady_clock;
    using TimePoint = std::chrono::time_point<Clock>;

    enum class SensorKind{
        UNKNOWN,
        DHT11,
        VS1813,
    };

    enum class SensorState{
        CREATED,
        INITIALIZED,
        RUNNING,
        STOPPED,
        ERROR
    };

    struct Error{
        int code = 0;
        std::string message;
    };

    struct SensorConfig{
        std::string name;
        SensorKind kind = SensorKind::UNKNOWN;
        int sample_hz = 1;
        std::map<std::string, std::string> kv;
    };

    struct ScalarReading{
        double value = 0.0;
        std::string unit;
    }

    struct VectorReading{
        vector<double> values;
    }

    using payload = std::variant<ScalarReading, VectorReading, std::vector<double>, std::monostate>;

    struct SensorReading{
        std::string sensor_name;
        SensorKind kind = SensorKind::UNKNOWN;
        TimePoint ts = Clock::now();
        payload data;
        std::vector<uint8_t> raw;
    };
}