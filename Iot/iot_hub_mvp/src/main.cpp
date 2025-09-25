// app/main.cpp
#include <csignal>
#include <chrono>
#include <cstdlib>
#include <cstring>
#include <iostream>
#include <thread>

#include "config.hpp"                 // AppConfig, ActuatorConfig
#include "manager/actuator_manager.hpp"
#include "manager/data_manager.hpp"
#include "manager/csv_manager.hpp"
#include "manager/mqtt_manager.hpp"

using namespace manager;

// 전역 포인터(간단한 안전 종료용)
static MqttManager* g_mqtt = nullptr;

static void on_signal(int) {
    std::cerr << "\n[main] signal received, stopping...\n";
    if (g_mqtt) {
        g_mqtt->stop();
        g_mqtt = nullptr;
    }
}

// 간단한 환경변수 헬퍼
static std::string getenv_or(const char* key, const std::string& defv) {
    const char* v = std::getenv(key);
    return (v && *v) ? std::string(v) : defv;
}


int main(int argc, char** argv) {
    // 1) 기본 설정(필요에 맞게 수정)
    AppConfig app{};
    // Actuator(핀/IR 파라미터)
    ActuatorConfig act{};

    // 3) 의존성 생성(외부 → DI)
    ActuatorManager actMgr(act);
    DataManager     dataMgr(/*max_metrics=*/20000, /*max_ir=*/50000);

    CsvOptions csvOpt{};
    CsvManager csvMgr(csvOpt);
    
    MqttManager mqtt(app, act, actMgr, dataMgr, csvMgr);
    g_mqtt = &mqtt;

    // 5) 시그널 핸들러
    std::signal(SIGINT,  on_signal);
    std::signal(SIGTERM, on_signal);

    // 6) 시작
    if (!mqtt.start()) {
        std::cerr << "[main] failed to start MqttManager\n";
        return 1;
    }

    std::cout << "[main] running (deviceId=" << app.deviceId << "). Press Ctrl+C to exit.\n";

    // 7) 간단 대기 루프(여기선 sleep만; 내부는 MqttManager가 쓰레드 운용)
    while (g_mqtt) {
        std::this_thread::sleep_for(std::chrono::seconds(2));
        // 필요하면 주기적 상태 출력 or 헬스체크
        // auto last = dataMgr.last_metrics(3);
        // std::cout << "[main] last metrics: " << last.size() << "\n";
    }

    std::cout << "[main] exited.\n";
    return 0;
}
