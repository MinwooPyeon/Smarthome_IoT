#pragma once
#include <string>

struct AppConfig {
    // --- device ---
    std::string deviceId = "rasp-1";

    // --- MQTT broker ---
    std::string mqttHost = "43.201.62.254";
    int         mqttPort = 8883;
    std::string mqttUser = "eeum";
    std::string mqttPass = "ssafy2086eeum";
    int         mqttKeepAlive = 60;

    // --- TLS (서버 인증 필수) ---
    // NOTE: '~'는 mosquitto가 직접 확장 못 함 → 코드에서 확장 처리해줌
    std::string mqttCAFile = "/home/eeum/broker_selfsigned_ca.crt";
    std::string mqttClientCertFile = ""; // mTLS 필요 시
    std::string mqttClientKeyFile  = ""; // mTLS 필요 시
    bool        mqttTLSInsecure = true;  // SNI/호스트명 검증 off (테스트용)

    // --- defaults for tests ---
    int         defaultQos = 1;
    bool        defaultRetain = false;

    // --- publish topic ---
    std::string topicEnv        = "hub/" + deviceId + "/env"; //realtime environment
    std::string topicIrSignal   = "hub/" + deviceId + "/irSignal"; //ir raw data
    std::string topicError      = "hub/" + deviceId + "/error"; //error
    // --- subscribe topic ---
    std::string topicOrderIrReq = "hub/" + deviceId + "/order/ir_req"; //ir signal require
    std::string topicOrderCtrl = "hub/" + deviceId + "/order/control"; //control
    std::string topicOrderEnv = "hub/" + deviceId + "/order/env"; //environment request

    // --- ENV Streaming ---
    bool envStreamOn = false;
    int envIntervalMs = 2000;

    // --- pin nubmer ---
    int         dhtPinBcm = 4;
    int         irPinBcm  = 17;
    int         irGapUs   = 8000;

    // --- EMWA Alpha ---
    double      ewmaAlphaT = 0.2;
    double      ewmaAlphaH = 0.2;
    double      comfortClo = 0.5;
    double      comfortMet = 1.l;
    double      comfortTr = 0;
    double      comfortVel = 0.1;

};
