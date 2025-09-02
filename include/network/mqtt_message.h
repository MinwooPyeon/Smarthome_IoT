#pragma once

#include <string>
#include <vector>
#include <map>
#include <nlohmann/json.hpp>

namespace irremote {

/**
 * @brief 공통 메시지 헤더
 */
struct MessageHeader {
    int64_t ts;                    ///< 타임스탬프 (밀리초)
    std::string deviceId;          ///< 디바이스 ID
    std::string msgId;             ///< 메시지 ID (UUID)
    std::string schema;            ///< 스키마 버전 (예: "irsignal/1.0")
    
    MessageHeader() : ts(0) {}
    MessageHeader(const std::string& device, const std::string& schema);
    
    void setTimestamp();
    std::string generateMsgId();
    
    nlohmann::json toJson() const;
    void fromJson(const nlohmann::json& json);
};

/**
 * @brief IR 신호 메시지
 */
struct IRSignalMessage : public MessageHeader {
    std::string encoding;          ///< 인코딩 (NEC, RC5, Samsung, Sony, Raw 등)
    int carrierHz;                 ///< 반송파 주파수 (Hz)
    float dutyCycle;               ///< 듀티 사이클 (0~1)
    std::string address;           ///< 프로토콜 주소/디바이스 코드
    std::string command;           ///< 커맨드 코드
    std::map<std::string, std::vector<int>> timing; ///< 타이밍 정보
    std::vector<int> rawData;      ///< 마이크로초 펄스 시퀀스
    std::string data;              ///< 프로토콜 인코딩된 비트열
    int repeat;                    ///< 재전송 횟수
    float quality;                 ///< 매칭 신뢰도 (학습 시)
    std::string remark;            ///< 비고
    
    IRSignalMessage();
    
    nlohmann::json toJson() const;
    void fromJson(const nlohmann::json& json);
};

/**
 * @brief 환경 센서 메시지
 */
struct EnvMessage : public MessageHeader {
    float temperature;             ///< 온도 (°C)
    float humidity;                ///< 습도 (%RH)
    float gasDensity;              ///< 가스 밀도 (ppm 등)
    std::map<std::string, std::string> units; ///< 단위 정보
    std::map<std::string, float> calib;       ///< 교정 정보
    float sampleRateHz;            ///< 샘플링 주기
    std::string status;            ///< 상태 (ok, error, warning)
    std::map<std::string, std::string> meta;  ///< 메타 정보
    
    EnvMessage();
    
    nlohmann::json toJson() const;
    void fromJson(const nlohmann::json& json);
};

/**
 * @brief 명령 메시지
 */
struct OrderMessage : public MessageHeader {
    std::string corrId;            ///< 상관관계 ID
    std::string type;              ///< 명령 타입 (ir, matter, system)
    int priority;                  ///< 우선순위 (0~9)
    int64_t expiresAt;            ///< 만료시각 (ms)
    std::map<std::string, int> retry; ///< 재시도 설정
    std::string replyTo;           ///< 응답 토픽
    nlohmann::json payload;        ///< 페이로드
    
    OrderMessage();
    
    nlohmann::json toJson() const;
    void fromJson(const nlohmann::json& json);
    
    bool isExpired() const;
    bool shouldRetry(int currentRetries) const;
};

/**
 * @brief 응답 메시지
 */
struct AckMessage : public MessageHeader {
    std::string corrId;            ///< 상관관계 ID
    std::string status;            ///< 상태 (accepted, processing, done, error)
    std::map<std::string, std::string> result; ///< 결과
    int durationMs;                ///< 수행시간 (ms)
    int retries;                   ///< 실제 재시도 횟수
    
    AckMessage();
    
    nlohmann::json toJson() const;
    void fromJson(const nlohmann::json& json);
};

/**
 * @brief 에러 메시지
 */
struct ErrorMessage : public MessageHeader {
    std::string level;             ///< 레벨 (INFO, WARN, ERROR, FATAL)
    std::string code;              ///< 에러 코드
    std::string detail;            ///< 상세 내용
    std::map<std::string, std::string> ctx; ///< 컨텍스트
    
    ErrorMessage();
    
    nlohmann::json toJson() const;
    void fromJson(const nlohmann::json& json);
};

/**
 * @brief 디바이스 상태 메시지
 */
struct StateMessage : public MessageHeader {
    std::string status;            ///< 상태 (online, offline, error)
    std::map<std::string, std::string> info; ///< 추가 정보
    
    StateMessage();
    
    nlohmann::json toJson() const;
    void fromJson(const nlohmann::json& json);
};

} // namespace irremote
