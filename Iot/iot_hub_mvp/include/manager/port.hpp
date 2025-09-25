#pragma once
#include <chrono>
#include <functional>
#include <vector>
#include "types.hpp"
namespace manager {

struct IEnvSource {
    virtual ~IEnvSource() = default;
    virtual bool init() = 0;
    virtual bool start_env_loop(std::chrono::milliseconds interval,
                                std::function<void(const EnvSample&)> cb) = 0;
    virtual void stop_env_loop() = 0;

    virtual bool set_ir_glitch_us(int) = 0;
    virtual void set_ir_gap_us(int) = 0;
};

struct IDataStore {
    virtual ~IDataStore() = default;
    virtual void add(const Metrics&) = 0;
    virtual void add(const IrSignalLog&) = 0;
    virtual void add(const IrSendDevice&) = 0;

    virtual std::vector<Metrics>     last_metrics(size_t n) const = 0;
    virtual std::vector<IrSignalLog> last_log(size_t n) const = 0;
};

struct IEventSink {
    virtual ~IEventSink() = default;
    virtual void post(const Metrics&) = 0;
    virtual void post(const IrSignalLog&) = 0;
    virtual void post(const IrSendDevice&) = 0;
};

struct IMqttBus {
    virtual ~IMqttBus() = default;
    virtual bool start() = 0;
    virtual void stop() = 0;
    // 필요시: publish(topic,payload), subscribe(topic,cb) 등
};

}
