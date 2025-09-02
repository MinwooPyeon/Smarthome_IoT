// File: header/mqtt/mqttSubscriber.hpp
#pragma once

#include <string>
#include <vector>
#include <atomic>
#include <nlohmann/json.hpp>
#include <mosquittopp.h>

using json = nlohmann::json;

/**
 * Mosquitto(libmosquittopp) 기반 Subscriber
 * - loop_start() 내부 스레드 사용
 * - 자동 재연결 (지수 백오프)
 * - on_connect()에서 재구독
 * - 메시지(JSON) 파싱 예시 포함
 */
class MqttSubscriber : public mosqpp::mosquittopp {
public:
    MqttSubscriber(const std::string& host,
                   int                port,
                   const std::string& clientId,
                   const std::vector<std::string>& topics,
                   int qos = 1,
                   int keepalive = 60);

    ~MqttSubscriber();

    // 비동기 연결 시작(내부 스레드 run)
    void connect();
    // 정상 종료
    void disconnect();

protected:
    // 콜백
    void on_connect(int rc) override;
    void on_disconnect(int rc) override;
    void on_subscribe(int mid, int qos_count, const int* granted_qos) override;
    void on_message(const mosquitto_message* message) override;

private:
    std::string host_;
    int         port_;
    int         keepalive_;
    int         qos_;
    std::vector<std::string> topics_;
    std::atomic<bool> connected_{false};

    void subscribe_all();
};
