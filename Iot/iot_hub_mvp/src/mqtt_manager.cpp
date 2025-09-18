// mqtt_manager.cpp
#include "mqtt_manager.hpp"
#include "util.hpp"
#include <chrono>
#include <iostream>

using json = nlohmann::json;
using namespace std::chrono;

MqttManager::MqttManager(const AppConfig& cfg)
: cfg_(cfg), dht_(cfg_.dhtPinBcm) {
    az_.setAlpha(cfg_.ewmaAlphaT, cfg_.ewmaAlphaH);
    az_.setComfort(cfg_.comfortClo, cfg_.comfortMet, cfg_.comfortTr, cfg_.comfortVel);
}

MqttManager::~MqttManager(){ stop(); }

void MqttManager::setup_handlers(){
    // 토픽 → 핸들러 등록 (가독성 ↑)
    handlers_.emplace(cfg_.topicEnv,        [this](const json& j){ h_env_request(j); });
    handlers_.emplace(cfg_.topicOrderIrReq, [this](const json& j){ h_ir_req(j); });
}

bool MqttManager::start(){
    if(!mqtt_.init(cfg_, "hub_agent_" + cfg_.deviceId)) return false;

    irMgr_.loadData();

    // 구독
    mqtt_.subscribe(cfg_.topicEnv, 1);
    mqtt_.subscribe(cfg_.topicOrderIrReq, 1);
    mqtt_.subscribe(cfg_.topicRegisDebice, 1);
    // 라우터 준비
    setup_handlers();

    // 센서 준비
    dht_.init();

    mqtt_.set_message_handler([this](const std::string& topic, const std::string& payload){
        on_mqtt_message(topic, payload);
    });

    running_ = true;
    loopThread_ = std::thread([this]{ run_loop(); });
    std::cout << "[mqtt] manager started\n";
    return true;
}

void MqttManager::stop(){
    if(!running_) return;
    running_ = false;
    if(loopThread_.joinable()) loopThread_.join();
    mqtt_.cleanup();
}

void MqttManager::on_mqtt_message(const std::string& topic, const std::string& payload){
    auto it = handlers_.find(topic);
    if(it == handlers_.end()){
        // 등록되지 않은 토픽은 무시
        return;
    }
    // payload → json 관용 파싱: bool/문자열/객체 모두 허용
    json j;
    try{
        if(payload == "true" || payload == "false" || payload == "1" || payload == "0")
            j = json::parse(payload == "true" || payload == "1" ? "true" : "false");
        else
            j = json::parse(payload);
    }catch(...){
        // 파싱 실패 시, {"raw": "<payload>"}로 전달
        j = json{{"raw", payload}};
    }
    it->second(j); // 해당 핸들러 호출
}

void MqttManager::run_loop(){
    auto lastEnv = steady_clock::now();
    while(running_){
        mqtt_.loop_for_ms(50);
        auto r = dht_.read_with_retry(1, 1500, 1200);
        envMgr_.addData(EnvSample{now_ms(), r->tempC, r->hum});
        
        if(cfg_.envStreamOn){    
            auto now = steady_clock::now();
            if(now - lastEnv >= milliseconds(cfg_.envIntervalMs)){
                lastEnv = now;
                if(r) publish_env(r->tempC, r->hum, now_ms());
                else  publish_error(0, "env_read_failed");
            }
        }
        std::this_thread::sleep_for(milliseconds(10));
    }
}

// ---------- 개별 핸들러 ----------
void MqttManager::h_env_request(const json& j){
    // true/false 요청 형식 여러가지 허용
    bool req=false;
    if(j.is_boolean()) req=j.get<bool>();
    else if(j.contains("Request")) req=j["Request"].get<bool>();
    else if(j.contains("request")) req=j["request"].get<bool>();
    else if(j.contains("raw")) {               // 문자열로 온 경우
        auto s = j["raw"].get<std::string>();
        req = (s=="true"||s=="1");
    }
    cfg_.envStreamOn = req;
    std::cout << "[env] stream=" << (req?"ON":"OFF") << "\n";
}

void MqttManager::h_ir_req(const json& j){
    int tx_id = j.value("tx_id", 0);
    std::string brand   = j.value("brand","UNKNOWN");
    std::string device  = j.value("device","UNKNOWN");
    std::string func    = j.value("function","");

    IrReceiver ir(cfg_.irPinBcm, cfg_.irGapUs);
    ir.init(50);
    auto fr = ir.capture_once(5000);
    if(!fr){ publish_error(tx_id, "ir_capture_timeout"); return; }

    // hub/{deviceId}/irsignal 로 raw 전송 (protocol topic은 사용 안 함)
    json irsig = {
        {"brand",   brand},
        {"device",  device},
        {"raw_data", fr->rawUs},
        {"function", func}
    };
    mqtt_.publish(cfg_.topicIrSignal, irsig.dump(), 1, false);
}

void MqttManager::h_control(const json& j){
    Log log;

    log.tx_id = j.value("tx_id", 0);
    log.deviceId = j.value("deviceId", "");
    if(log.deviceId == "") {
        publish_error(log.tx_id, "Send Device Id NULL Reference Exception");
    }
    log.deviceType = j.value("device_type", "UNKNOWN");
    log.function = j.value("function", "NULL");
    log.metaData = j.value("metaData", "NULL");

    logMgr_.addLog(log);
}

void MqttManager::h_regist_send(const json& j){
    int tx_id = j.value("tx_id", 0);
    IrSendDevice device;
    device.deviceId = j.value("deviceId", "");
    if(device.deviceId == "") {
        publish_error(tx_id, "Send Device Id NULL Reference Exception");
    }
    device.deviceType = j.value("device_type", "UNKNOWN");
    device.consumption = j.value("consumption", 0);
    
    bool is_add = j.value("add_rm", true);
    std::string topic = "hub/"+device.deviceId+"/order/control";
    if(is_add){
        irMgr_.addData(device);
        mqtt_.subscribe(topic, 1);
        handlers_.emplace(topic, [this](const json& j){ h_ir_req(j); });
    }else{
        irMgr_.deleteData(device);
        mqtt_.unsubscribe(topic);
        handlers_.erase(topic);
    }
}

// ---------- 공용 유틸 ----------
void MqttManager::publish_env(double T, double RH, int64_t ts){
    // ★ 한 프레임만 계산: 임시 벡터에 현재 샘플만 넣어서 Analyzer 호출
    std::vector<EnvSample> one;
    one.reserve(1);
    one.push_back(EnvSample{ts, T, RH});

    Metrics m = az_.compute(one);  // Analyzer는 단일 샘플로 계산

    json env = {
        {"temperature",  T},
        {"humidity",     RH},
        {"dew_point",    m.dewPoint},
        {"head_index",   m.heatIndex},
        {"abs_humidity", m.absHumidity},
        {"pmv",          m.pmv},
        {"ppd",          m.ppd},
        {"wbgt",         m.wbgt}
    };
    mqtt_.publish(cfg_.topicEnv, env.dump(), 1, false);
}

void MqttManager::publish_error(int tx_id, const std::string& reason){
    json ej = { {"tx_id", tx_id}, {"error", reason} };
    mqtt_.publish(cfg_.topicError, ej.dump(), 1, false);
}
