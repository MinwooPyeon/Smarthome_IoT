#pragma once
#include <string>

struct AppConfig {
    std::string deviceId = "rpi5-a";
    std::string mqttHost = "43.201.62.254";
    int         mqttPort = 8883;
    std::string mqttUser = "";     // 필요 시
    std::string mqttPass = "";     // 필요 시

    int dhtPinBcm = 4;             // 기본 DHT11 BCM 핀
    int irPinBcm  = 17;            // VS1838B OUT 연결 BCM 핀
    int irGapUs   = 8000;          // 프레임 분리 임계
};
