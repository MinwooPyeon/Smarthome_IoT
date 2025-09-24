#include "mqtt/mqtt_handler.hpp"
#include "types.hpp"
#include <iostream>

using namespace mqtt;

void MqttHandler::on_env_require(const bool streaming)
{
    d_.cfg->envStreamOn = streaming;
}

void MqttHandler::on_metrics(const Metrics &met)
{
    if (d_.dataMgr) d_.dataMgr->add(met);
    if (d_.csvMgr)  d_.csvMgr->post(met);
    publish_env_if_enabled(met);
}

void MqttHandler::on_ir_control(const IrSignalLog &log)
{
    if (d_.dataMgr) d_.dataMgr->add(log);
    if (d_.csvMgr)  d_.csvMgr->post(log);
}
void MqttHandler::on_ir_regist(const IrSendDevice &device)
{
    if (d_.dataMgr) d_.dataMgr->add(device);
    if (d_.csvMgr)  d_.csvMgr->post(device);
    d_.mqtt->subscribe("hub/" + device.deviceId + "/order/control", 1);
}

void MqttHandler::on_ir_capture(const IrSignal &signal)
{
    if (!d_.cfg || !d_.mqtt) return;
    nlohmann::json j = {
        {"brand",   signal.brand},
        {"device",   signal.device},
        {"raw_data", signal.raw_us},
        {"function", signal.function}
    };
    d_.mqtt->publish(d_.cfg->topicIrSignal, j.dump(), d_.cfg->defaultQos, d_.cfg->defaultRetain);
}

void MqttHandler::publish_env_if_enabled(const Metrics &m)
{
    if(!d_.cfg || !d_.mqtt) return;
    if(!d_.cfg->envStreamOn) return;

    nlohmann::json env = {
        {"temperature", m.tAvg},
        {"humidity",    m.hAvg},
        {"dew_point",   m.dewPoint},
        {"head_index",  m.heatIndex},
        {"abs_humidity",m.absHumidity},
        {"pmv",         m.pmv},
        {"ppd",         m.ppd},
        {"wbgt",        m.wbgt}
    };
    d_.mqtt->publish(d_.cfg->topicEnv, env.dump(), d_.cfg->defaultQos, d_.cfg->defaultRetain);
}
