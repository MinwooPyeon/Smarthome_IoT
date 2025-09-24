#pragma once
#include <memory>
#include "manager/port.hpp"
#include "config.hpp"
#include "types.hpp"

namespace manager {

class MainManager {
public:
    MainManager(const AppConfig& app, const ActuatorConfig& act);
    ~MainManager();

    bool start();
    void stop();

    // 편의 API
    void setEnvStreaming(bool on);
    bool isEnvStreaming() const;
    std::vector<Metrics>     lastMetrics(size_t n) const;
    std::vector<IrSignalLog> lastIrLogs(size_t n) const;
    bool setIrGlitchUs(int us);
    void setIrGapUs(int us);

private:
    // 구현체 소유 (unique_ptr)
    std::unique_ptr<IEnvSource> env_;   // = std::make_unique<ActuatorManager>(...)
    std::unique_ptr<IDataStore> store_; // = std::make_unique<DataManager>(...)
    std::unique_ptr<IEventSink> sink_;  // = std::make_unique<CsvManager>(...)
    std::unique_ptr<IMqttBus>   mqtt_;  // = std::make_unique<MqttManager>(...)

    // 설정
    AppConfig      app_;
    ActuatorConfig act_;
    bool running_ = false;
};

} // namespace manager
