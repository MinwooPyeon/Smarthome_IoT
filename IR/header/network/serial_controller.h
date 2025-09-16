#pragma once

#include <string>
#include <functional>
#include <chrono>
#include "cJSON.h"

/**
 * @brief ESP32와 컴퓨터 간의 시리얼 통신을 담당하는 클래스
 * USB-C to USB-C 연결을 통해 JSON 기반 명령을 처리합니다.
 */
class SerialController {
public:
    /**
     * @brief 명령 처리 콜백 함수 타입
     * @param command 명령어
     * @param params 명령 파라미터
     * @return 처리 결과 JSON 문자열
     */
    using CommandCallback = std::function<std::string(const std::string& command, const cJSON* params)>;

    /**
     * @brief 생성자
     * @param baud_rate 시리얼 통신 속도 (기본값: 115200)
     */
    SerialController(int baud_rate = 115200);

    /**
     * @brief 소멸자
     */
    ~SerialController();

    /**
     * @brief 시리얼 통신 초기화
     * @return 초기화 성공 여부
     */
    bool initialize();

    /**
     * @brief 시리얼 통신 루프 (메인 루프에서 호출)
     */
    void loop();

    /**
     * @brief 명령 처리 콜백 등록
     * @param callback 명령 처리 함수
     */
    void setCommandCallback(CommandCallback callback);

    /**
     * @brief 응답 전송
     * @param response 응답 JSON 문자열
     */
    void sendResponse(const std::string& response);

    /**
     * @brief 에러 응답 전송
     * @param error_code 에러 코드
     * @param error_message 에러 메시지
     */
    void sendError(const std::string& error_code, const std::string& error_message);

    /**
     * @brief 상태 정보 전송
     * @param status 상태 정보
     */
    void sendStatus(const cJSON* status);

    /**
     * @brief 연결 상태 확인
     * @return 연결 상태
     */
    bool isConnected() const;

    /**
     * @brief 디버그 모드 설정
     * @param enabled 디버그 모드 활성화 여부
     */
    void setDebugMode(bool enabled);

    // 보안 관련 메서드
    void setAuthenticationToken(const std::string& token);
    void setMaxMessageSize(size_t max_size);
    void setRateLimit(int max_messages_per_second);
    bool validateCommand(const std::string& command) const;
    bool validateJson(const std::string& json_str) const;
    std::string sanitizeInput(const std::string& input) const;

private:
    int m_baud_rate;
    bool m_initialized;
    bool m_debug_mode;
    CommandCallback m_command_callback;

    // 보안 관련 멤버 변수
    std::string m_auth_token;
    size_t m_max_message_size;
    int m_max_messages_per_second;
    std::chrono::steady_clock::time_point m_last_message_time;
    int m_message_count;

    // 내부 버퍼
    std::string m_input_buffer;
    static const size_t MAX_BUFFER_SIZE = 1024;

    /**
     * @brief 입력 데이터 처리
     */
    void processInput();

    /**
     * @brief JSON 명령 파싱 및 처리
     * @param json_str JSON 문자열
     */
    void processCommand(const std::string& json_str);

    /**
     * @brief 기본 명령 처리
     * @param command 명령어
     * @param params 명령 파라미터
     * @return 처리 결과
     */
    std::string handleDefaultCommand(const std::string& command, const cJSON* params);

    /**
     * @brief 디버그 출력
     * @param message 디버그 메시지
     */
    void debugPrint(const std::string& message);
    
    /**
     * @brief 속도 제한 확인
     * @return 속도 제한 통과 여부
     */
    bool checkRateLimit();
};
