#include "manager/mqtt_manager.hpp"
#include "util.hpp"
#include <chrono>
#include <iostream>

using namespace std::chrono;
using json = nlohmann::json;
using manager::MqttManager;

MqttManager::MqttManager(const AppConfig &cfg)
    : cfg_(cfg),
      dht_(cfg_.dhtPinBcm),
      dataMgr_(/*max_metrics*/ 20000, /*max_ir*/ 50000),
      csvMgr_([this]{
        CsvOptions o;
        o.base_dir = "./logs";
        o.device_id = cfg_.deviceId;
        o.rotate_daily = true;
        o.flush_every_n = 200;
        o.flush_interval_ms = 1000;
        o.max_queue = 20000;
        o.drop_oldest_on_full = true;
    return o; }())
{
    az_.setAlpha(cfg_.ewmaAlphaT, cfg_.ewmaAlphaH);
    az_.setComfort(cfg_.comfortClo, cfg_.comfortMet, cfg_.comfortTr, cfg_.comfortVel);
}

MqttManager::~MqttManager() { stop(); }

void MqttManager::setup_handlers()
{
    handlers_.emplace(cfg_.topicOrderEnv,
                      [this](const json &j)
                      { h_env_request(j); });

    handlers_.emplace(cfg_.topicOrderIrReq,
                      [this](const json &j)
                      { h_ir_req(j); });

    handlers_.emplace(cfg_.topicRegisDevice,
                      [this](const json &j)
                      { h_regist_send(j); });
}

bool MqttManager::start()
{
    if (!mqtt_.init(cfg_, "hub_agent_" + cfg_.deviceId))
        return false;

    // 구독
    mqtt_.subscribe(cfg_.topicOrderEnv, 1);
    mqtt_.subscribe(cfg_.topicOrderIrReq, 1);
    mqtt_.subscribe(cfg_.topicRegisDevice, 1);

    setup_handlers();

    // 센서 준비
    dht_.init();

    mqtt_.set_message_handler([this](const std::string &topic, const std::string &payload)
                              { on_mqtt_message(topic, payload); });

    // CSV 비동기 시작
    csvMgr_.start();

    // === 이벤트 구독자 배선 ===
    // (1) Metrics → 메모리/CSV + (옵션) MQTT 재발행 (cfg_.envStreamOn일 때만)
    bus_.subscribe<Metrics>([this](const Metrics &m)
                            {
        dataMgr_.add(m);
        csvMgr_.post(m);

        if (cfg_.envStreamOn) {
            json env = {
                {"temperature", m.tAvg},
                {"humidity",    m.hAvg},
                {"dew_point",   m.dewPoint},
                {"head_index",  m.heatIndex},
                {"abs_humidity",m.absHumidity},
                {"pmv",         m.pmv},
                {"ppd",         m.ppd},
                {"wbgt",        m.wbgt}
            };
            mqtt_.publish(cfg_.topicEnv, env.dump(), cfg_.defaultQos, cfg_.defaultRetain);
        } });

    // (2) IR 로그 → 메모리/CSV + 기존 토픽 relay 유지
    bus_.subscribe<IrSignalLog>([this](const IrSignalLog &l)
                                {
        dataMgr_.add(l);
        csvMgr_.post(l);

        // 기존 코드와 동일: hub/{deviceId}/irSignal 에 raw 전송
        json j = {
            {"brand", ""},                        // (필요하면 상위에서 채워서 넘겨)
            {"device", l.send_device_id},
            {"raw_data", l.raw_data},
            {"function", l.function_label}
        };
        mqtt_.publish(cfg_.topicIrSignal, j.dump(), cfg_.defaultQos, cfg_.defaultRetain); });

    running_ = true;
    loopThread_ = std::thread([this]
                              { run_loop(); });

    std::cout << "[mqtt] manager started\n";
    return true;
}

void MqttManager::stop()
{
    if (!running_)
        return;
    running_ = false;
    if (loopThread_.joinable())
        loopThread_.join();

    csvMgr_.stop();
    mqtt_.cleanup();
}

void MqttManager::on_mqtt_message(const std::string &topic, const std::string &payload)
{
    auto it = handlers_.find(topic);
    if (it == handlers_.end())
        return;

    json j;
    try
    {
        if (payload == "true" || payload == "false" || payload == "1" || payload == "0")
            j = json::parse((payload == "true" || payload == "1") ? "true" : "false");
        else
            j = json::parse(payload);
    }
    catch (...)
    {
        j = json{{"raw", payload}};
    }
    it->second(j);
}

void MqttManager::run_loop()
{
    auto nextTick = steady_clock::now();

    while (running_)
    {
        mqtt_.loop_for_ms(10);

        if (steady_clock::now() >= nextTick)
        {
            nextTick = steady_clock::now() + milliseconds(cfg_.envIntervalMs);

            if (auto r = dht_.read_with_retry(/*tries*/ 1, /*interval_us*/ 1500, /*timeout_us*/ 1200))
            {
                std::vector<EnvSample> one;
                one.emplace_back(EnvSample{now_ms(), r->tempC, r->hum});
                Metrics m = az_.compute(one);

                Metrics ev{};
                ev.ts = std::chrono::system_clock::now();
                ev.tAvg = m.tAvg;
                ev.hAvg = m.hAvg;
                ev.tEwma = m.tEwma;
                ev.hEwma = m.hEwma;
                ev.dewPoint = m.dewPoint;
                ev.heatIndex = m.heatIndex;
                ev.absHumidity = m.absHumidity;
                ev.wbgt = m.wbgt;
                ev.pmv = m.pmv;
                ev.ppd = m.ppd;
                ev.spike = m.spike;

                bus_.publish<Metrics>(ev);
            }
        }

        std::this_thread::sleep_for(milliseconds(5));
    }
}

// ---------- handlers ----------
void MqttManager::h_env_request(const json &j)
{
    // 여러 형태 허용: {"streaming": true}, true, "true" 등
    bool req = false;
    if (j.is_boolean())
        req = j.get<bool>();
    else
        req = j.value("streaming", false);

    cfg_.envStreamOn = req;
    std::cout << "[env] stream : " << (req ? "ON" : "OFF") << "\n";
}

void MqttManager::h_ir_req(const json &j)
{
    int tx_id = j.value("tx_id", 0);
    std::string devId = j.value("deviceId", "");
    std::string devType = j.value("device_type", "UNKNOWN");
    std::string func = j.value("function", "");

    IrReceiver ir(cfg_.irPinBcm, cfg_.irGapUs);
    ir.init(50);
    auto fr = ir.capture_once(5000);
    if (!fr)
    {
        publish_error(tx_id, "ir_capture_timeout");
        return;
    }

    IrSignalLog ev{};
    ev.ts = std::chrono::system_clock::now();
    ev.tx_id = tx_id;
    ev.send_device_id = devId;
    ev.device_type = devType;
    ev.function_label = func;
    ev.meta_data = {"src=hub", "reason=req"};

    bus_.publish<IrSignalLog>(ev);

    std::cout << "[ir] captured len=" << fr->rawUs.size()
              << " device=" << devId << " func=" << func << "\n";
}

void MqttManager::h_regist_send(const json &j)
{
    int tx_id = j.value("tx_id", 0);
    std::string dev = j.value("deviceId", "");
    if (dev.empty())
    {
        publish_error(tx_id, "Send Device Id NULL Reference Exception");
        return;
    }

    bool is_add = j.value("add_rm", true);
    std::string topic = "hub/" + dev + "/order/control";

    if (is_add)
    {
        mqtt_.subscribe(topic, 1);
        handlers_.emplace(topic, [this](const json &jj)
                          { h_ir_req(jj); });
        std::cout << "[SendDevice Add] " << dev << " (sub " << topic << ")\n";
    }
    else
    {
        mqtt_.unsubscribe(topic);
        handlers_.erase(topic);
        std::cout << "[SendDevice Remove] " << dev << " (unsub)\n";
    }
}

// ---------- utils ----------
void MqttManager::publish_error(int tx_id, const std::string &reason)
{
    json ej = {{"tx_id", tx_id}, {"error", reason}};
    std::cout << "[error publish] " << reason << "\n";
    mqtt_.publish(cfg_.topicError, ej.dump(), cfg_.defaultQos, cfg_.defaultRetain);
}
