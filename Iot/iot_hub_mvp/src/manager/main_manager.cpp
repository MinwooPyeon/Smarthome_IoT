
#include "manager/main_manager.hpp"
#include "manager/service.hpp"
#include "manager/actuator_manager.hpp"
#include "manager/data_manager.hpp"
#include "manager/csv_manager.hpp"
#include "manager/mqtt_manager.hpp"

namespace manager {

MainManager::MainManager(const AppConfig& app, const ActuatorConfig& act)
: app_(app), act_(act)
{
    // 1) 구현체 구성
    env_   = std::make_unique<ActuatorManager>(act_);
    store_ = std::make_unique<DataManager>(/*capacity 옵션*/);
    sink_  = std::make_unique<CsvManager>(/*csv 옵션*/);

    // DI 버전: mqtt_에 참조 주입
    mqtt_  = std::make_unique<MqttManager>(app_, act_, *env_, *store_, *sink_);

    // 2) 전역 레지스트리에 인터페이스 등록 (한 번만)
    Service::register_instance<IEnvSource>(std::shared_ptr<IEnvSource>(env_.get(), [](IEnvSource*){}));
    Service::register_instance<IDataStore>(std::shared_ptr<IDataStore>(store_.get(), [](IDataStore*){}));
    Service::register_instance<IEventSink>(std::shared_ptr<IEventSink>(sink_.get(), [](IEventSink*){}));
    Service::register_instance<IMqttBus>(std::shared_ptr<IMqttBus>(mqtt_.get(), [](IMqttBus*){}));
    // 주의: shared_ptr 커스텀 deleter로 실제 삭제는 unique_ptr이 담당
}

MainManager::~MainManager() {
    stop();
    Service::reset(); // 전역 포인터 제거 (메모리/댕글링 방지)
}

bool MainManager::start() {
    if (running_) return true;
    if (!env_->init()) return false;
    running_ = mqtt_->start();
    return running_;
}
void MainManager::stop() {
    if (!running_) return;
    mqtt_->stop();
    env_->stop_env_loop();
    running_ = false;
}

// 편의 API들
void MainManager::setEnvStreaming(bool on) {
    // 필요하면 mqtt_에 토글 전달 or store_/sink_ 정책 변경 등
    // 간단히 레지스트리에서 가져다 써도 됨:
    (void)on;
}
bool MainManager::isEnvStreaming() const { return true; }

std::vector<Metrics> MainManager::lastMetrics(size_t n) const {
    return store_->last_metrics(n);
}
std::vector<IrSignalLog> MainManager::lastIrLogs(size_t n) const {
    return store_->last_log(n);
}
bool MainManager::setIrGlitchUs(int us) { return env_->set_ir_glitch_us(us); }
void MainManager::setIrGapUs(int us)    { env_->set_ir_gap_us(us); }

} 
