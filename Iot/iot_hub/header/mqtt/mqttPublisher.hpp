// File: header/mqtt/mqttPublisher.hpp
#pragma once

#include <string>
#include <atomic>
#include <nlohmann/json.hpp>
#include <mosquittopp.h>   // C++ wrapper

using json = nlohmann::json;

/**
 * Mosquitto(libmosquittopp) 기반 간단 퍼블리셔
 * - LWT(offline/online)
 * - loop_start() 내부스레드 사용
 * - 자동 재연결 딜레이 설정
 */
class MqttPublisher : public mosqpp::mosquittopp {
public:
    MqttPublisher(const std::string& host,
                  int                port,
                  const std::string& clientId,
                  const std::string& topicBase,
                  int qos = 1,
                  bool retained = false,
                  int keepalive = 60);
    ~MqttPublisher();

    // 브로커 연결 시작(비동기). 내부적으로 loop_start() 실행
    void connect();

    // JSON payload 발행
    void publish(const json& j);

    // 정상 종료
    void disconnect();

protected:
    // mosquittopp 콜백
    void on_connect(int rc) override;
    void on_disconnect(int rc) override;
    void on_publish(int mid) override;

private:
    std::string host_;
    int         port_;
    std::string topicBase_;
    int         qos_;
    bool        retained_;
    int         keepalive_;

    std::atomic<bool> connected_{false};
};
