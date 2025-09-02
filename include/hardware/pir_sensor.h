#pragma once

#include <string>
#include <functional>
#include <memory>
#include <thread>
#include <atomic>

namespace irremote {

/**
 * @brief PIR 센서를 IR 수신 센서로 사용하는 클래스
 * 
 * PIR 센서의 모션 감지 신호를 IR 신호 수신으로 해석합니다.
 */
class PIRSensor {
public:
    /**
     * @brief IR 신호 수신 콜백 함수 타입
     */
    using IRSignalCallback = std::function<void(const std::string&, int)>;

    /**
     * @brief 센서 상태 콜백 함수 타입
     */
    using StatusCallback = std::function<void(bool, const std::string&)>;

    PIRSensor();
    ~PIRSensor();

    // 복사 생성자 및 할당 연산자 (삭제)
    PIRSensor(const PIRSensor&) = delete;
    PIRSensor& operator=(const PIRSensor&) = delete;

    // 이동 생성자 및 할당 연산자
    PIRSensor(PIRSensor&&) noexcept;
    PIRSensor& operator=(PIRSensor&&) noexcept;

    /**
     * @brief PIR 센서 초기화
     * @param gpio_pin GPIO 핀 번호
     * @param sensitivity 감도 설정 (0.0 ~ 1.0)
     * @return 초기화 성공 여부
     */
    bool initialize(int gpio_pin, float sensitivity = 0.5);

    /**
     * @brief 센서 해제
     */
    void cleanup();

    /**
     * @brief IR 신호 수신 콜백 설정
     * @param callback 콜백 함수
     */
    void setIRSignalCallback(IRSignalCallback callback) { ir_signal_callback_ = callback; }

    /**
     * @brief 센서 상태 콜백 설정
     * @param callback 콜백 함수
     */
    void setStatusCallback(StatusCallback callback) { status_callback_ = callback; }

    /**
     * @brief 센서 활성화
     */
    void enable();

    /**
     * @brief 센서 비활성화
     */
    void disable();

    /**
     * @brief 센서 상태 확인
     * @return 활성화 상태
     */
    bool isEnabled() const { return enabled_; }

    /**
     * @brief 마지막 감지된 IR 신호 정보 가져오기
     * @return IR 신호 데이터
     */
    std::string getLastIRSignal() const { return last_ir_signal_; }

    /**
     * @brief 감지된 IR 신호 개수 가져오기
     * @return 감지된 신호 개수
     */
    int getDetectedCount() const { return detected_count_; }

    /**
     * @brief 감도 설정
     * @param sensitivity 감도 (0.0 ~ 1.0)
     */
    void setSensitivity(float sensitivity);

    /**
     * @brief 현재 감도 가져오기
     * @return 현재 감도
     */
    float getSensitivity() const { return sensitivity_; }

private:
    /**
     * @brief 센서 모니터링 스레드
     */
    void monitoringThread();

    /**
     * @brief IR 신호 패턴 분석
     * @param signal_data 원시 신호 데이터
     * @return 분석된 IR 신호
     */
    std::string analyzeIRPattern(const std::vector<int>& signal_data);

    /**
     * @brief 신호 노이즈 필터링
     * @param signal_data 원시 신호 데이터
     * @return 필터링된 신호 데이터
     */
    std::vector<int> filterNoise(const std::vector<int>& signal_data);

    // 멤버 변수들
    int gpio_pin_;
    float sensitivity_;
    std::atomic<bool> enabled_;
    std::atomic<bool> running_;
    
    // 콜백 함수들
    IRSignalCallback ir_signal_callback_;
    StatusCallback status_callback_;
    
    // 모니터링 스레드
    std::unique_ptr<std::thread> monitoring_thread_;
    
    // 센서 데이터
    std::string last_ir_signal_;
    std::atomic<int> detected_count_;
    
    // 신호 처리 관련
    std::vector<int> signal_buffer_;
    int signal_threshold_;
    int debounce_time_ms_;
};

} // namespace irremote
