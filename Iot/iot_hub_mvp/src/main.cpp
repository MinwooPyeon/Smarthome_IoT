#include "config.hpp"
#include "mqtt_client.hpp"
#include "dht11_reader.hpp"
#include "ir_receiver.hpp"
#include "util.hpp"

#include <pigpio.h>
#include <nlohmann/json.hpp>
#include <iostream>
#include <cmath>

using json = nlohmann::json;

int main(int argc, char **argv)
{
    // 0) 기본 설정
    AppConfig cfg;
    if (gpioInitialise() < 0)
    {
        std::cerr << "pigpio init failed\n";
        return 1;
    }

    MqttClient mqtt;
    std::string clientId = "hub_agent_" + cfg.deviceId;
    if (!mqtt.init(clientId, cfg.mqttHost, cfg.mqttPort, cfg.mqttUser, cfg.mqttPass))
    {
        std::cerr << "mqtt init failed\n";
        return 2;
    }

    std::string reqTopic = "hub/" + cfg.deviceId + "/measure/req";
    std::string respTopic = "hub/" + cfg.deviceId + "/measure/resp";
    mqtt.subscribe(reqTopic, 0);

    mqtt.set_message_handler([&](const std::string &topic, const std::string &payload)
                             {
        try{
            auto j = json::parse(payload);
            std::string sensor = j.value("sensor", "");
            std::string msgId  = j.value("msgId", "");
            int timeoutMs = j.value("timeoutMs", 1500);

            if(sensor == "dht11"){
                int pin = cfg.dhtPinBcm;
                if(j.contains("dht") && j["dht"].contains("pin")) pin = j["dht"]["pin"].get<int>();

                Dht11Reader dht(pin);
                dht.init();
                auto r = dht.read_once(timeoutMs);

                json out;
                out["msgId"]    = msgId;
                out["deviceId"] = cfg.deviceId;
                out["sensor"]   = "dht11";
                out["ts"]       = now_ms();

                if(r){
                    float T  = r->tempC;
                    float RH = r->hum;
                    float a=17.27f,b=237.7f;
                    float alpha = (a*T)/(b+T) + std::log(RH/100.0f); 
                    float dew = (b*alpha)/(a - alpha);

                    out["ok"]   = true;
                    out["data"] = json{ {"tempC", T}, {"hum", RH}, {"dewPointC", dew} }; 
                }else{
                    out["ok"]    = false;
                    out["error"] = "read_failed_or_timeout";
                }
                mqtt.publish(respTopic, out.dump(), 0, false);
            }
            else if(sensor == "ir"){
                int gapUs = cfg.irGapUs;
                if(j.contains("ir") && j["ir"].contains("gapUs")) gapUs = j["ir"]["gapUs"].get<int>();

                IrReceiver ir(cfg.irPinBcm, gapUs);
                ir.init(50);
                auto f = ir.capture_once(timeoutMs);

                json out;
                out["msgId"]    = msgId;
                out["deviceId"] = cfg.deviceId;
                out["sensor"]   = "ir";
                out["ts"]       = now_ms();

                if(f){
                    out["ok"] = true;
                    out["data"] = json{
                        {"carrierHz", nullptr},
                        {"rawData",   f->raw_us},
                        {"gapUs",     f->gapUs}
                    };
                }else{
                    out["ok"]    = false;
                    out["error"] = "timeout_or_noise";
                }
                mqtt.publish(respTopic, out.dump(), 0, false);
            }
            else{
                json out = {
                    {"msgId", j.value("msgId","")},
                    {"deviceId", cfg.deviceId},
                    {"sensor", sensor},
                    {"ok", false},
                    {"error", "unknown_sensor"}
                };
                mqtt.publish(respTopic, out.dump(), 0, false);
            }
        }catch(std::exception& e){
            std::cerr << "handler exception: " << e.what() << "\n";
        } });

    std::cout << "Listening on topic: " << reqTopic << "\n";
    mqtt.loop_forever();

    mqtt.cleanup();
    gpioTerminate();
    return 0;
}
