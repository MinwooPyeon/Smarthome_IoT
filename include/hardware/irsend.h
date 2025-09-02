#pragma once

#include <string>
#include <vector>
#include <memory>
#include <functional>

namespace irremote {

/**
 * @brief IR 전송 결과 열거형
 */
enum class IRSendResult {
    SUCCESS,           ///< 전송 성공
    DEVICE_NOT_FOUND,  ///< IR 장치를 찾을 수 없음
    INVALID_CODE,      ///< 잘못된 IR 코드
    EXECUTION_FAILED,  ///< 명령 실행 실패
    TIMEOUT,           ///< 타임아웃
    PERMISSION_DENIED  ///< 권한 없음
};

/**
 * @brief IR 전송 상태 구조체
 */
struct IRSendStatus {
    IRSendResult result;
    std::string message;
    int retry_count;
    double execution_time_ms;
    
    IRSendStatus() : result(IRSendResult::SUCCESS), retry_count(0), execution_time_ms(0.0) {}
};

/**
 * @brief IR 전송 콜백 함수 타입
 */
using IRSendCallback = std::function<void(const IRSendStatus&)>;

/**
 * @brief IR 전송 클래스
 * 
 * LIRC를 통해 IR 신호를 전송하는 기능을 제공합니다.
 */
class IRSend {
public:
    IRSend();
    ~IRSend();

    // 복사 생성자 및 할당 연산자 (삭제)
    IRSend(const IRSend&) = delete;
    IRSend& operator=(const IRSend&) = delete;

    // 이동 생성자 및 할당 연산자
    IRSend(IRSend&&) noexcept;
    IRSend& operator=(IRSend&&) noexcept;

    /**
     * @brief IR 장치 초기화
     * @param device IR 장치 경로 (기본값: "/dev/lirc0")
     * @return 초기화 성공 여부
     */
    bool initialize(const std::string& device = "/dev/lirc0");

    /**
     * @brief IR 장치 해제
     */
    void cleanup();

    /**
     * @brief IR 명령 전송
     * @param remote_name 리모컨 이름
     * @param command 명령
     * @return 전송 결과
     */
    IRSendStatus sendCommand(const std::string& remote_name, const std::string& command);

    /**
     * @brief IR 명령 전송 (비동기)
     * @param remote_name 리모컨 이름
     * @param command 명령
     * @param callback 완료 콜백
     */
    void sendCommandAsync(const std::string& remote_name, const std::string& command, 
                         IRSendCallback callback = nullptr);

    /**
     * @brief 여러 명령 연속 전송
     * @param remote_name 리모컨 이름
     * @param commands 명령 목록
     * @param delay_ms 명령 간 지연 시간 (밀리초)
     * @return 전송 결과들
     */
    std::vector<IRSendStatus> sendCommands(const std::string& remote_name, 
                                          const std::vector<std::string>& commands,
                                          int delay_ms = 100);

    /**
     * @brief IR 장치 상태 확인
     * @return 장치 사용 가능 여부
     */
    bool isDeviceAvailable() const;

    /**
     * @brief IR 장치 정보 가져오기
     * @return 장치 정보 문자열
     */
    std::string getDeviceInfo() const;

    /**
     * @brief 사용 가능한 리모컨 목록 가져오기
     * @return 리모컨 이름 목록
     */
    std::vector<std::string> getAvailableRemotes() const;

    /**
     * @brief 리모컨의 사용 가능한 명령 목록 가져오기
     * @param remote_name 리모컨 이름
     * @return 명령 목록
     */
    std::vector<std::string> getAvailableCommands(const std::string& remote_name) const;

    /**
     * @brief 전송 타임아웃 설정
     * @param timeout_ms 타임아웃 (밀리초)
     */
    void setTimeout(int timeout_ms);

    /**
     * @brief 재시도 횟수 설정
     * @param retry_count 재시도 횟수
     */
    void setRetryCount(int retry_count);

    /**
     * @brief 전송 지연 시간 설정
     * @param delay_ms 지연 시간 (밀리초)
     */
    void setDelay(int delay_ms);

    /**
     * @brief 디버그 모드 설정
     * @param enabled 디버그 모드 활성화 여부
     */
    void setDebugMode(bool enabled);

    /**
     * @brief 마지막 오류 메시지 가져오기
     * @return 오류 메시지
     */
    std::string getLastError() const { return last_error_; }

    /**
     * @brief 전송 통계 가져오기
     * @return 통계 정보 JSON
     */
    std::string getStatistics() const;

    /**
     * @brief 통계 초기화
     */
    void resetStatistics();

    /**
     * @brief IR 신호 강도 설정 (하드웨어 지원시)
     * @param intensity 신호 강도 (0-100)
     * @return 설정 성공 여부
     */
    bool setSignalIntensity(int intensity);

    /**
     * @brief IR 신호 주파수 설정 (하드웨어 지원시)
     * @param frequency 주파수 (kHz)
     * @return 설정 성공 여부
     */
    bool setSignalFrequency(int frequency);

    /**
     * @brief IR 신호 테스트
     * @param remote_name 리모컨 이름
     * @param command 테스트할 명령
     * @return 테스트 결과
     */
    IRSendStatus testSignal(const std::string& remote_name, const std::string& command);

private:
    // 내부 메서드들
    bool checkDevicePermissions() const;
    bool validateCommand(const std::string& remote_name, const std::string& command) const;
    std::string executeIRSendCommand(const std::string& remote_name, const std::string& command) const;
    void updateStatistics(const IRSendStatus& status);
    void setLastError(const std::string& error) const;

    // 멤버 변수들
    std::string device_path_;
    bool initialized_;
    bool debug_mode_;
    int timeout_ms_;
    int retry_count_;
    int delay_ms_;
    int signal_intensity_;
    int signal_frequency_;
    mutable std::string last_error_;

    // 통계 정보
    struct Statistics {
        uint64_t total_commands;
        uint64_t successful_commands;
        uint64_t failed_commands;
        double average_execution_time;
        double total_execution_time;
        
        Statistics() : total_commands(0), successful_commands(0), failed_commands(0),
                      average_execution_time(0.0), total_execution_time(0.0) {}
    } stats_;
};

} // namespace irremote
