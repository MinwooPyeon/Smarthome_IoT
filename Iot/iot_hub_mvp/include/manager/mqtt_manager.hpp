#pragma once
#include <unordered_map>
#include <unordered_set>
#include <functional>
#include <atomic>
#include <thread>
#include <nlohmann/json.hpp>

#include "config.hpp"
#include "mqtt/mqtt_client.hpp"
#include "actuator/dht11_reader.hpp"
#include "actuator/ir_receiver.hpp"
#include "analyzer.hpp"
#include "types.hpp"              
#include "manager/data_manager.hpp"
#include "manager/csv_manager.hpp"
#include "mqtt/mqtt_handler.hpp" 

namespace manager {

class MqttManager {
public:
    explicit MqttManager(const AppConfig &cfg);
    ~MqttManager();

    bool start();
    void stop();

private:
    using json = nlohmann::json;

    // --- deps ---
    AppConfig        cfg_;
    
    mqtt::MqttClient mqtt_;
    Analyzer         az_;
    Dht11Reader      dht_;

    // --- in-proc storage / csv ---
    manager::DataManager dataMgr_;
    manager::CsvManager  csvMgr_;

    // --- 분리된 이벤트 핸들러(의존성 주입) ---
    mqtt::MqttHandler evh_;

    // --- runtime ---
    std::atomic<bool> running_{false};
    std::thread       loopThread_;

    // topic → handler(json)
    std::unordered_map<std::string, std::function<void(const json &)>> handlers_;

    // ====== 동적 구독 관리 상태 ======
    // 등록된 IR Send Device 목록
    std::unordered_map<std::string, std::string> ir_devices_;   // deviceId -> device_type
    // 현재 MQTT 구독 중인 control 토픽
    std::unordered_set<std::string> active_control_topics_;     // "hub/<deviceId>/order/control"

private:
    // router
    void setup_handlers(); // 초기 라우터 등록(정적 토픽)
    void on_mqtt_message(const std::string &topic, const std::string &payload);

    // handlers
    void h_env_request(const json &j);  // 환경 스트리밍 on/off
    void h_ir_req(const json &j);       // 허브 IR 캡처 요청(서버가 시킴)
    void h_regist_send(const json &j);  // IR Send Device 등록/삭제
    void h_control(const json &j);      // 서버→허브 제어 로그(디바이스별 동적 토픽)

    // loop
    void run_loop();

    // dynamic route helpers
    void bind_control_route(const std::string& deviceId);
    void unbind_control_route(const std::string& deviceId);

    // utils
    void publish_error(int tx_id, const std::string &reason);
};

} // namespace manager
