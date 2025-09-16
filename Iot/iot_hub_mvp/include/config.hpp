#pragma once
#include <string>

struct AppConfig {
    // --- device ---
    std::string deviceId = "rpi5-a";

    // --- MQTT broker ---
    std::string mqttHost = "43.201.62.254";
    int         mqttPort = 8883;
    std::string mqttUser = "eeum";
    std::string mqttPass = "ssafy2086eeum";
    int         mqttKeepAlive = 60;

    // --- TLS (서버 인증 필수) ---
    // NOTE: '~'는 mosquitto가 직접 확장 못 함 → 코드에서 확장 처리해줌
    std::string mqttCAFile = "/home/eeum/fullchain.pem";
    std::string mqttClientCertFile = ""; // mTLS 필요 시
    std::string mqttClientKeyFile  = ""; // mTLS 필요 시
    bool        mqttTLSInsecure = true;  // SNI/호스트명 검증 off (테스트용)

    // --- defaults for tests ---
    int  defaultQos = 1;
    bool defaultRetain = false;

    // (센서 핀도 기존대로 유지 가능)
    int dhtPinBcm = 4;
    int irPinBcm  = 17;
    int irGapUs   = 8000;
};
