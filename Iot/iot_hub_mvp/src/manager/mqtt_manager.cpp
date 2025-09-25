#include "manager/mqtt_manager.hpp"
#include "manager/csv_manager.hpp"
#include "manager/data_manager.hpp"
#include "manager/actuator_manager.hpp"

#include "util.hpp"     // now_ms()
#include <chrono>
#include <iostream>
#include <functional>

using namespace std::chrono;
using json = nlohmann::json;

namespace manager {

MqttManager::MqttManager(const AppConfig &cfg, const ActuatorConfig &actCfg,
                         ActuatorManager& act, DataManager& data, CsvManager& csv)
: cfg_(cfg), actCfg_(actCfg_),actMgr_(act), dataMgr_(data), csvMgr_(csv), evh_(mqtt::Deps{&dataMgr_, &csvMgr_, &mqtt_, &cfg_ })
{
    az_.setAlpha(cfg_.ewmaAlphaT, cfg_.ewmaAlphaH);
    az_.setComfort(cfg_.comfortClo, cfg_.comfortMet, cfg_.comfortTr, cfg_.comfortVel);
}

MqttManager::~MqttManager() { stop(); }

void MqttManager::setup_handlers()
{
    // 정적 구독 토픽 라우팅
    handlers_.emplace(cfg_.topicOrderEnv,   [this](const json &j){ h_env_request(j); });
    handlers_.emplace(cfg_.topicOrderIrReq, [this](const json &j){ h_ir_req(j); });
    handlers_.emplace(cfg_.topicRegisDevice,[this](const json &j){ h_regist_send(j); });

    // ⚠️ 디바이스별 control 토픽은 동적 등록/해제 (bind_control_route / unbind_control_route)
}

bool MqttManager::start()
{
    if (!mqtt_.init(cfg_, "hub_agent_" + cfg_.deviceId))
        return false;

    // 정적 구독
    mqtt_.subscribe(cfg_.topicOrderEnv, 1);
    mqtt_.subscribe(cfg_.topicOrderIrReq, 1);
    mqtt_.subscribe(cfg_.topicRegisDevice, 1);

    setup_handlers();

    // ★ Actuator 초기화
    if (!actMgr_.init()) {
        std::cerr << "[mqtt] actuator init failed\n";
        return false;
    }

    mqtt_.set_message_handler([this](const std::string &topic, const std::string &payload)
    { on_mqtt_message(topic, payload); });

    // CSV 비동기 시작
    csvMgr_.start();

    running_ = true;
    loopThread_ = std::thread([this]{ run_loop(); });

    std::cout << "[mqtt] manager started\n";
    return true;
}

void MqttManager::stop()
{
    if (!running_) return;
    running_ = false;
    if (loopThread_.joinable()) loopThread_.join();

    // 동적 구독 해제(선택: 브로커 정리)
    for (const auto& topic : active_control_topics_) {
        mqtt_.unsubscribe(topic);
    }
    active_control_topics_.clear();
    handlers_.clear();

    csvMgr_.stop();
    mqtt_.cleanup();

    // ★ Actuator shutdown
    actMgr_.shutdown();
}

void MqttManager::on_mqtt_message(const std::string &topic, const std::string &payload)
{
    auto it = handlers_.find(topic);
    if (it == handlers_.end())
        return;

    json j;
    try {
        if (payload == "true" || payload == "false" || payload == "1" || payload == "0")
            j = json::parse((payload == "true" || payload == "1") ? "true" : "false");
        else
            j = json::parse(payload);
    } catch (...) {
        j = json{{"raw", payload}};
    }
    it->second(j);
}

void MqttManager::run_loop()
{
    auto nextTick = steady_clock::now();

    while (running_) {
        mqtt_.loop_for_ms(10);

        // 일정 주기마다 환경 측정/분석
        if (steady_clock::now() >= nextTick) {
            nextTick = steady_clock::now() + milliseconds(cfg_.envIntervalMs);

            if (auto r = actMgr_.read_env_with_retry()) {
                // Analyzer는 단일 샘플도 처리
                std::vector<EnvSample> one;
                one.emplace_back(EnvSample{ now_ms(), r->tempC, r->hum });
                ::Metrics mcalc = az_.compute(one);

                // 전역 Metrics로 이벤트/저장/퍼블리시
                ::Metrics ev{};
                ev.ts          = std::chrono::system_clock::now();
                ev.tAvg        = mcalc.tAvg;
                ev.hAvg        = mcalc.hAvg;
                ev.tEwma       = mcalc.tEwma;
                ev.hEwma       = mcalc.hEwma;
                ev.dewPoint    = mcalc.dewPoint;
                ev.heatIndex   = mcalc.heatIndex;
                ev.absHumidity = mcalc.absHumidity;
                ev.wbgt        = mcalc.wbgt;
                ev.pmv         = mcalc.pmv;
                ev.ppd         = mcalc.ppd;
                ev.spike       = mcalc.spike;

                // 분리된 핸들러에 위임 (DataManager/CSV/MQTT 퍼블리시 포함)
                evh_.on_metrics(ev);
            }
        }

        std::this_thread::sleep_for(milliseconds(5));
    }
}

// ---------- handlers ----------

// ENV 스트리밍 on/off
void MqttManager::h_env_request(const json &j)
{
    bool req = false;
    if (j.is_boolean()) req = j.get<bool>();
    else req = j.value("streaming", false);

    cfg_.envStreamOn = req;
    std::cout << "[env] stream : " << (req ? "ON" : "OFF") << "\n";
}

// 허브 IR 캡처 요청 → raw IR 수신/서버 relay/CSV 로깅
void MqttManager::h_ir_req(const json &j)
    {

        std::string brand = j.value("brand", "UNKNOWN");
        std::string device = j.value("device", "UNKNOWN");
        std::string func = j.value("function", "UNKNOWN");

        IrReceiver ir(actCfg_.irPinBcm, actCfg_.irGapUs);
        ir.init(50);
        auto fr = ir.capture_once(5000);
        if (!fr)
        {
            publish_error(-1, "ir_capture_timeout");
            return;
        }

        IrSignal ev{};
        ev.brand = brand;
        ev.device = device;
        ev.function = func;
        ev.raw_us = fr->rawUs;

        // 분리된 핸들러에 위임 (DataManager/CSV/MQTT relay 포함)
        evh_.on_ir_capture(ev);

        std::cout << "[ir capture] device=" << device
                  << " func=" << func
                  << " len=" << ev.raw_us.size() << "\n";
    }


// IR Send Device 등록/삭제 → control 토픽 동적 구독/해제
void MqttManager::h_regist_send(const json &j)
{
    const int tx_id = j.value("tx_id", 0);

    const std::string deviceId   = j.value("deviceId", std::string{});
    const std::string deviceType = j.value("device_type", std::string{"UNKNOWN"});
    const double      consumption= j.value("consumption", 0.0); // kWh, 지금은 저장만 가능
    const bool        add_rm     = j.value("add_rm", true);     // true: Add, false: Remove
    (void)consumption;

    if (deviceId.empty()) {
        publish_error(tx_id, "Send Device Id NULL Reference Exception");
        return;
    }

    try {
        if (add_rm) {
            // 등록(Add) — 상태 갱신 + 동적 구독
            ir_devices_[deviceId] = deviceType; // 필요시 consumption 별도 보관
            bind_control_route(deviceId);
            std::cout << "[SendDevice Add] id=" << deviceId
                      << " type=" << deviceType << " (control subscribed)\n";
        } else {
            // 삭제(Remove) — 동적 구독 해제 + 상태 제거
            ir_devices_.erase(deviceId);
            unbind_control_route(deviceId);
            std::cout << "[SendDevice Remove] id=" << deviceId
                      << " (control unsubscribed)\n";
        }
    } catch (const std::exception& e) {
        publish_error(tx_id, std::string("regist_send_exception: ") + e.what());
    }
}

// 서버→허브 제어 로그(디바이스별 동적 토픽) : raw IR 아님
void MqttManager::h_control(const json &j)
    {
        const int tx_id = j.value("tx_id", 0);
        const std::string deviceId = j.value("deviceId", "");
        const std::string devType = j.value("device_type", "UNKNOWN");
        const std::string func = j.value("function", "NULL");
        const std::string meta = j.dump();

        if (deviceId.empty())
        {
            publish_error(tx_id, "Control deviceId empty");
            return;
        }

        // 분리된 핸들러에 위임 (control 전용 CSV append/ACK 등은 핸들러에서)
        IrSignalLog ev{};
        ev.ts = std::chrono::system_clock::now();
        ev.tx_id = tx_id;
        ev.send_device_id = deviceId;
        ev.device_type = devType;
        ev.function_label = func;
        ev.meta_data = meta;

        evh_.on_ir_control(ev);

        std::cout << "[control] id=" << deviceId
                  << " type=" << devType
                  << " func=" << func << "\n";
    }


// ---------- dynamic route helpers ----------
void MqttManager::bind_control_route(const std::string& deviceId)
{
    const std::string topic = "hub/" + deviceId + "/order/control";
    if (active_control_topics_.find(topic) != active_control_topics_.end())
        return; // 이미 구독 중

    mqtt_.subscribe(topic, cfg_.defaultQos);
    // 해당 토픽으로 들어온 메시지는 "제어 로그"로 처리
    handlers_.emplace(topic, [this](const json &jj){ h_control(jj); });
    active_control_topics_.insert(topic);
}

void MqttManager::unbind_control_route(const std::string& deviceId)
{
    const std::string topic = "hub/" + deviceId + "/order/control";
    auto a = active_control_topics_.find(topic);
    if (a == active_control_topics_.end())
        return; // 구독 안 됨

    mqtt_.unsubscribe(topic);
    active_control_topics_.erase(a);

    auto it = handlers_.find(topic);
    if (it != handlers_.end()) handlers_.erase(it);
}

// ---------- utils ----------
void MqttManager::publish_error(int tx_id, const std::string &reason)
{
    json ej = { {"tx_id", tx_id}, {"error", reason} };
    std::cout << "[error publish] " << reason << "\n";
    mqtt_.publish(cfg_.topicError, ej.dump(), cfg_.defaultQos, cfg_.defaultRetain);
}

} // namespace manager
