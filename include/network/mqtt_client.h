#pragma once

#include <string>
#include <functional>
#include <memory>
#include <mosquitto.h>
#include <nlohmann/json.hpp>
#include "network/mqtt_message.h"

namespace irremote {

class MqttClient {
public:
    /**
     * @brief 명령 수신 콜백 함수 타입
     */
    using OrderCallback = std::function<void(const OrderMessage&)>;

    /**
     * @brief 연결 상태 콜백 함수 타입
     */
    using ConnectionCallback = std::function<void(bool)>;

    /**
     * @brief 메시지 발행 완료 콜백 함수 타입
     */
    using PublishCallback = std::function<void(bool, int)>;

    MqttClient();
    ~MqttClient();

    // 복사 생성자 및 할당 연산자 (삭제)
    MqttClient(const MqttClient&) = delete;
    MqttClient& operator=(const MqttClient&) = delete;

    // 이동 생성자 및 할당 연산자
    MqttClient(MqttClient&&) noexcept;
    MqttClient& operator=(MqttClient&&) noexcept;

    /**
     * @brief MQTT 브로커에 연결
     * @param broker 브로커 주소
     * @param port 브로커 포트
     * @param username 사용자명 (선택사항)
     * @param password 비밀번호 (선택사항)
     * @param client_id 클라이언트 ID
     * @return 연결 성공 여부
     */
    bool connect(const std::string& broker, int port = 1883,
                const std::string& username = "", const std::string& password = "",
                const std::string& client_id = "");

    /**
     * @brief MQTT 브로커에서 연결 해제
     */
    void disconnect();

    /**
     * @brief 연결 상태 확인
     * @return 연결 상태
     */
    bool isConnected() const;

    /**
     * @brief 명령 수신 콜백 설정
     * @param callback 콜백 함수
     */
    void setOrderCallback(OrderCallback callback) { order_callback_ = callback; }

    /**
     * @brief 연결 상태 콜백 설정
     * @param callback 콜백 함수
     */
    void setConnectionCallback(ConnectionCallback callback) { connection_callback_ = callback; }

    /**
     * @brief 명령 토픽 구독
     * @param deviceId 디바이스 ID
     * @param qos QoS 레벨 (0, 1, 2)
     * @return 구독 성공 여부
     */
    bool subscribeToOrders(const std::string& deviceId, int qos = 1);

    /**
     * @brief 명령 토픽 구독 해제
     * @param deviceId 디바이스 ID
     * @return 구독 해제 성공 여부
     */
    bool unsubscribeFromOrders(const std::string& deviceId);

    /**
     * @brief IR 신호 메시지 발행
     * @param deviceId 디바이스 ID
     * @param message IR 신호 메시지
     * @param qos QoS 레벨 (0, 1, 2)
     * @param callback 발행 완료 콜백
     * @return 발행 성공 여부
     */
    bool publishIRSignal(const std::string& deviceId, const IRSignalMessage& message, 
                        int qos = 1, PublishCallback callback = nullptr);

    /**
     * @brief 환경 센서 메시지 발행
     * @param deviceId 디바이스 ID
     * @param message 환경 센서 메시지
     * @param qos QoS 레벨 (0, 1, 2)
     * @param callback 발행 완료 콜백
     * @return 발행 성공 여부
     */
    bool publishEnvironment(const std::string& deviceId, const EnvMessage& message,
                          int qos = 1, PublishCallback callback = nullptr);

    /**
     * @brief 명령 응답 메시지 발행
     * @param deviceId 디바이스 ID
     * @param message 응답 메시지
     * @param qos QoS 레벨 (0, 1, 2)
     * @param callback 발행 완료 콜백
     * @return 발행 성공 여부
     */
    bool publishAck(const std::string& deviceId, const AckMessage& message,
                    int qos = 1, PublishCallback callback = nullptr);

    /**
     * @brief 에러 메시지 발행
     * @param deviceId 디바이스 ID
     * @param message 에러 메시지
     * @param qos QoS 레벨 (0, 1, 2)
     * @param callback 발행 완료 콜백
     * @return 발행 성공 여부
     */
    bool publishError(const std::string& deviceId, const ErrorMessage& message,
                     int qos = 1, PublishCallback callback = nullptr);

    /**
     * @brief 디바이스 상태 메시지 발행
     * @param deviceId 디바이스 ID
     * @param message 상태 메시지
     * @param qos QoS 레벨 (0, 1, 2)
     * @param callback 발행 완료 콜백
     * @return 발행 성공 여부
     */
    bool publishState(const std::string& deviceId, const StateMessage& message,
                     int qos = 1, PublishCallback callback = nullptr);

    /**
     * @brief MQTT 루프 실행
     * @param timeout_ms 타임아웃 (밀리초)
     * @return 성공 여부
     */
    bool loop(int timeout_ms = 100);

    /**
     * @brief 디바이스 ID 설정
     * @param deviceId 디바이스 ID
     */
    void setDeviceId(const std::string& deviceId) { device_id_ = deviceId; }

    /**
     * @brief 디바이스 ID 가져오기
     * @return 디바이스 ID
     */
    const std::string& getDeviceId() const { return device_id_; }

private:
    // Mosquitto 콜백 함수들
    static void onConnect(struct mosquitto* mosq, void* userdata, int result);
    static void onDisconnect(struct mosquitto* mosq, void* userdata, int result);
    static void onMessage(struct mosquitto* mosq, void* userdata, 
                         const struct mosquitto_message* message);
    static void onPublish(struct mosquitto* mosq, void* userdata, int mid);

    // 내부 메서드들
    bool publishMessage(const std::string& topic, const nlohmann::json& payload,
                       int qos, PublishCallback callback = nullptr);
    std::string buildTopic(const std::string& type, const std::string& deviceId) const;
    void handleOrderMessage(const std::string& payload);

    // 멤버 변수들
    struct mosquitto* mosq_;
    std::string device_id_;
    std::string broker_;
    int port_;
    bool connected_;
    
    // 콜백 함수들
    OrderCallback order_callback_;
    ConnectionCallback connection_callback_;
    
    // 발행 완료 콜백 맵
    std::map<int, PublishCallback> publish_callbacks_;
    std::mutex callback_mutex_;
};

} // namespace irremote
