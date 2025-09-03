#pragma once

#include <string>
#include <vector>
#include <memory>
#include <chrono>
#include <thread>
#include <atomic>
#include <mutex>

// 전방 선언
class IRCodeStore;

/**
 * @brief IR 전송 결과 열거형
 */
enum class IRSendResult {
    SUCCESS,                    // 성공
    DEVICE_NOT_FOUND,          // IR 전송 장치를 찾을 수 없음
    INVALID_CODE,              // 잘못된 IR 코드
    TRANSMISSION_FAILED,       // 전송 실패
    PERMISSION_DENIED,         // 권한 없음
    TIMEOUT,                   // 시간 초과
    UNKNOWN_ERROR              // 알 수 없는 오류
};

/**
 * @brief IR 전송 상태 구조체
 */
struct IRSendStatus {
    IRSendResult result;        // 전송 결과
    std::string message;        // 상태 메시지
    int attempts;               // 시도 횟수
    double duration_ms;         // 소요 시간 (밀리초)
    
    IRSendStatus() : result(IRSendResult::UNKNOWN_ERROR), attempts(0), duration_ms(0.0) {}
    IRSendStatus(IRSendResult r, const std::string& msg, int att = 0, double dur = 0.0)
        : result(r), message(msg), attempts(att), duration_ms(dur) {}
};

/**
 * @brief IR 신호 전송 클래스
 * 
 * 제어신호-IR신호 매핑을 통해 IR 신호를 전송합니다.
 * 허브에서 받은 제어신호를 IR 신호로 변환하여 즉시 전송할 수 있습니다.
 */
class IRSend {
public:
    IRSend();
    ~IRSend();
    
    // 복사 생성자 및 할당 연산자 (삭제)
    IRSend(const IRSend&) = delete;
    IRSend& operator=(const IRSend&) = delete;
    
    // 이동 생성자 및 할당 연산자
    IRSend(IRSend&& other) noexcept;
    IRSend& operator=(IRSend&& other) noexcept;

    /**
     * @brief IR 전송 장치 초기화
     * @return 초기화 성공 여부
     */
    bool initialize();

    /**
     * @brief IR 전송 장치 정리
     */
    void cleanup();

    /**
     * @brief 제어신호로 IR 신호 전송
     * @param control_signal 제어신호 
     * @return 전송 결과
     */
    IRSendStatus sendControlSignal(const std::string& control_signal);

    /**
     * @brief 여러 제어신호 순차 전송
     * @param control_signals 제어신호 목록
     * @param delay_ms 명령 간 지연 시간 (밀리초)
     * @return 전송 결과들
     */
    std::vector<IRSendStatus> sendControlSignals(const std::vector<std::string>& control_signals,
                                                int delay_ms = 100);

    /**
     * @brief IR 코드 저장소 설정
     * @param code_store IR 코드 저장소 포인터
     */
    void setCodeStore(IRCodeStore* code_store);

    /**
     * @brief 디버그 모드 설정
     * @param enabled 디버그 모드 활성화 여부
     */
    void setDebugMode(bool enabled);

    /**
     * @brief 마지막 오류 메시지 가져오기
     * @return 마지막 오류 메시지
     */
    std::string getLastError() const;

    /**
     * @brief 전송 통계 정보 가져오기
     * @return 전송 통계 정보
     */
    struct Statistics {
        size_t total_sent;          // 총 전송 횟수
        size_t successful_sends;    // 성공한 전송 횟수
        size_t failed_sends;        // 실패한 전송 횟수
        double average_duration_ms; // 평균 소요 시간
        std::chrono::steady_clock::time_point last_send_time; // 마지막 전송 시간
    };
    
    Statistics getStatistics() const;

private:
    std::atomic<bool> initialized_;
    std::atomic<bool> debug_mode_;
    IRCodeStore* code_store_;
    mutable std::mutex mutex_;
    
    // 통계 정보
    mutable std::mutex stats_mutex_;
    Statistics stats_;
    std::string last_error_;
    
    // 헬퍼 메서드들
    bool checkDevicePermissions();
    bool validateControlSignal(const std::string& control_signal);
    IRSendStatus executeIRSendCommand(const std::string& ir_signal);
    void updateStatistics(const IRSendStatus& status);
    void setLastError(const std::string& error);
    
    /**
     * @brief IR 신호 직접 전송
     * @param ir_signal IR 신호 (예: "0xE0E040BF")
     * @return 전송 결과
     */
    IRSendStatus sendIRCode(const std::string& ir_signal);
};
