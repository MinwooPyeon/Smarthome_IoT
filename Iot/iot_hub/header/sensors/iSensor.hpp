#pragma once
#include "util/sensorTypes.hpp"
#include <atomic>
#include <functional>
#include <optional>
#include <memory>
#include <mutex>

namespace sensors{

    class iSensor{
    public:
        using ReadingCallback = std::function<void(const SensorReading&)>;
        using ErrorCallback = std::function<void(const Error&)>;

        virtual ~iSensor() = default;

        // 구성 & 메타
        virtual SensorState state() const = 0;
        virtual const SensorConfig& config() const = 0;

        // 생명 주기
        virtual bool initialize(Error* err  = nullptr) = 0;
        virtual bool start(Error* err = nullptr) = 0;
        virtual void stop() = 0;

        // synchronous read
        virtual std::optional<SensorReading> readOnce(Error* err = nullptr) = 0;

        // 콜백 함수 등록 : 비동기
        virtual void setReadingCallback(ReadingCallback callback) = 0;
        virtual void setErrorCallback(ErrorCallback callback) = 0;

        virtual bool reset(Error* err = nullptr) {(void)err; return false;}
    };
    using ISensorPtr = std::shared_ptr<iSensor>;
}