#pragma once

#include <string>
#include <functional>
#include <thread>
#include <atomic>
#include <chrono>

namespace irremote {

/**
 * @brief PIR 센서 상태 열거형
 */
enum class PIRSensorState {
    NO_MOTION,    ///< 움직임 없음
    MOTION_DETECTED, ///< 움직임 감지됨
    ERROR         ///< 오류 상태
};

/**
 * @brief PIR 센서 콜백 함수 타입
 */
using PIRMotionCallback = std::function<void(bool motion_detected)>;

/**
 * @brief PIR 센서 클래스
 * 
 * 움직임 감지를 담당하며, 감지 시 콜백을 호출합니다.
 */
class PIRSensor {
public:
    /**
     * @brief 생성자
     * @param gpio_pin PIR 센서가 연결된 GPIO 핀 번호
     */
    explicit PIRSensor(int gpio_pin = 17);
    ~PIRSensor();

    // 복사 생성자 및 할당 연산자 (삭제)
    PIRSensor(const PIRSensor&) = delete;
    PIRSensor& operator=(const PIRSensor&) = delete;

    // 이동 생성자 및 할당 연산자
    PIRSensor(PIRSensor&&) noexcept;
    PIRSensor& operator=(PIRSensor&&) noexcept;

    /**
     * @brief PIR 센서 초기화
     * @return 초기화 성공 여부
     */
    bool initialize();

    /**
     * @brief PIR 센서 해제
     */
    void cleanup();

    /**
     * @brief 움직임 감지 시작
     * @param callback 움직임 감지 시 호출될 콜백 함수
     * @return 시작 성공 여부
     */
    bool startDetection(PIRMotionCallback callback = nullptr);

    /**
     * @brief 움직임 감지 중지
     */
    void stopDetection();

    /**
     * @brief 현재 움직임 감지 상태 확인
     * @return 움직임 감지 여부
     */
    bool isMotionDetected() const;

    /**
     * @brief 현재 센서 상태 가져오기
     * @return 센서 상태
     */
    PIRSensorState getState() const;

    /**
     * @brief 움직임 감지 임계값 설정
     * @param threshold_ms 움직임 감지 후 대기 시간 (밀리초)
     */
    void setDetectionThreshold(int threshold_ms);

    /**
     * @brief 센서 감도 설정
     * @param sensitivity 감도 (1-10, 높을수록 민감)
     */
    void setSensitivity(int sensitivity);

    /**
     * @brief 센서 정보 가져오기
     * @return 센서 정보 JSON
     */
    std::string getSensorInfo() const;

    /**
     * @brief 센서 통계 가져오기
     * @return 통계 정보 JSON
     */
    std::string getStatistics() const;

    /**
     * @brief 통계 초기화
     */
    void resetStatistics();

    /**
     * @brief 마지막 오류 메시지 가져오기
     * @return 오류 메시지
     */
    std::string getLastError() const { return last_error_; }

    /**
     * @brief 센서 테스트
     * @return 테스트 성공 여부
     */
    bool testSensor();

private:
    // 내부 메서드들
    void detectionLoop();
    bool setupGPIO();
    void cleanupGPIO();
    bool readGPIO() const;
    void setLastError(const std::string& error) const;
    void updateStatistics(bool motion_detected);

    // 멤버 변수들
    int gpio_pin_;
    bool initialized_;
    std::atomic<bool> running_;
    std::atomic<bool> motion_detected_;
    PIRSensorState state_;
    PIRMotionCallback motion_callback_;
    
    // 설정
    int detection_threshold_ms_;
    int sensitivity_;
    
    // 스레드
    std::thread detection_thread_;
    
    // 통계
    struct Statistics {
        uint64_t total_detections;
        uint64_t false_positives;
        double average_detection_time;
        std::chrono::steady_clock::time_point last_detection;
        
        Statistics() : total_detections(0), false_positives(0), 
                      average_detection_time(0.0) {}
    } stats_;
    
    mutable std::string last_error_;
};

} // namespace irremote
