#include "hardware/pir_sensor.h"
#include <iostream>
#include <chrono>
#include <algorithm>
#include <cmath>

#ifdef __linux__
#include <wiringPi.h>
#include <softPwm.h>
#else
// Windows 환경에서는 GPIO 시뮬레이션
#define HIGH 1
#define LOW 0
#define INPUT 0
#define OUTPUT 1
#define INPUT_PULLUP 2

inline void pinMode(int pin, int mode) {
    // Windows에서는 GPIO 시뮬레이션
    std::cout << "GPIO Sim: pin " << pin << " set to mode " << mode << std::endl;
}

inline int digitalRead(int pin) {
    // Windows에서는 랜덤 신호 시뮬레이션
    static int counter = 0;
    counter++;
    return (counter % 100 < 5) ? HIGH : LOW; // 5% 확률로 HIGH
}

inline void delay(int ms) {
    std::this_thread::sleep_for(std::chrono::milliseconds(ms));
}
#endif

namespace irremote {

PIRSensor::PIRSensor() 
    : gpio_pin_(-1), sensitivity_(0.5), enabled_(false), running_(false),
      detected_count_(0), signal_threshold_(100), debounce_time_ms_(50) {
}

PIRSensor::~PIRSensor() {
    cleanup();
}

PIRSensor::PIRSensor(PIRSensor&& other) noexcept
    : gpio_pin_(other.gpio_pin_), sensitivity_(other.sensitivity_),
      enabled_(other.enabled_.load()), running_(other.running_.load()),
      ir_signal_callback_(std::move(other.ir_signal_callback_)),
      status_callback_(std::move(other.status_callback_)),
      monitoring_thread_(std::move(other.monitoring_thread_)),
      last_ir_signal_(std::move(other.last_ir_signal_)),
      detected_count_(other.detected_count_.load()),
      signal_buffer_(std::move(other.signal_buffer_)),
      signal_threshold_(other.signal_threshold_),
      debounce_time_ms_(other.debounce_time_ms_) {
    
    other.gpio_pin_ = -1;
    other.enabled_ = false;
    other.running_ = false;
}

PIRSensor& PIRSensor::operator=(PIRSensor&& other) noexcept {
    if (this != &other) {
        cleanup();
        
        gpio_pin_ = other.gpio_pin_;
        sensitivity_ = other.sensitivity_;
        enabled_ = other.enabled_.load();
        running_ = other.running_.load();
        ir_signal_callback_ = std::move(other.ir_signal_callback_);
        status_callback_ = std::move(other.status_callback_);
        monitoring_thread_ = std::move(other.monitoring_thread_);
        last_ir_signal_ = std::move(other.last_ir_signal_);
        detected_count_ = other.detected_count_.load();
        signal_buffer_ = std::move(other.signal_buffer_);
        signal_threshold_ = other.signal_threshold_;
        debounce_time_ms_ = other.debounce_time_ms_;
        
        other.gpio_pin_ = -1;
        other.enabled_ = false;
        other.running_ = false;
    }
    return *this;
}

bool PIRSensor::initialize(int gpio_pin, float sensitivity) {
    if (gpio_pin < 0) {
        std::cerr << "Invalid GPIO pin: " << gpio_pin << std::endl;
        return false;
    }
    
    gpio_pin_ = gpio_pin;
    setSensitivity(sensitivity);
    
    // GPIO 설정
    pinMode(gpio_pin_, INPUT);
    
    // 신호 임계값 계산
    signal_threshold_ = static_cast<int>(100 * sensitivity);
    
    std::cout << "PIR Sensor initialized on GPIO " << gpio_pin_ 
              << " with sensitivity " << sensitivity_ << std::endl;
    
    if (status_callback_) {
        status_callback_(true, "PIR sensor initialized successfully");
    }
    
    return true;
}

void PIRSensor::cleanup() {
    disable();
    
    if (monitoring_thread_ && monitoring_thread_->joinable()) {
        monitoring_thread_->join();
    }
    
    gpio_pin_ = -1;
    enabled_ = false;
    running_ = false;
    
    if (status_callback_) {
        status_callback_(false, "PIR sensor cleaned up");
    }
}

void PIRSensor::enable() {
    if (enabled_ || gpio_pin_ < 0) {
        return;
    }
    
    enabled_ = true;
    running_ = true;
    
    // 모니터링 스레드 시작
    monitoring_thread_ = std::make_unique<std::thread>(&PIRSensor::monitoringThread, this);
    
    std::cout << "PIR Sensor enabled" << std::endl;
    
    if (status_callback_) {
        status_callback_(true, "PIR sensor enabled");
    }
}

void PIRSensor::disable() {
    if (!enabled_) {
        return;
    }
    
    enabled_ = false;
    running_ = false;
    
    if (monitoring_thread_ && monitoring_thread_->joinable()) {
        monitoring_thread_->join();
    }
    
    std::cout << "PIR Sensor disabled" << std::endl;
    
    if (status_callback_) {
        status_callback_(false, "PIR sensor disabled");
    }
}

void PIRSensor::setSensitivity(float sensitivity) {
    if (sensitivity < 0.0f || sensitivity > 1.0f) {
        std::cerr << "Invalid sensitivity value: " << sensitivity << ". Must be 0.0-1.0" << std::endl;
        return;
    }
    
    sensitivity_ = sensitivity;
    signal_threshold_ = static_cast<int>(100 * sensitivity);
    
    std::cout << "PIR Sensor sensitivity set to " << sensitivity_ 
              << " (threshold: " << signal_threshold_ << ")" << std::endl;
}

void PIRSensor::monitoringThread() {
    std::cout << "PIR Sensor monitoring thread started" << std::endl;
    
    int last_state = LOW;
    auto last_change = std::chrono::steady_clock::now();
    
    while (running_) {
        if (!enabled_) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            continue;
        }
        
        int current_state = digitalRead(gpio_pin_);
        
        // 상태 변화 감지
        if (current_state != last_state) {
            auto now = std::chrono::steady_clock::now();
            auto time_diff = std::chrono::duration_cast<std::chrono::milliseconds>(now - last_change).count();
            
            // 디바운싱
            if (time_diff > debounce_time_ms_) {
                if (current_state == HIGH) {
                    // 모션 감지 (IR 신호 수신으로 해석)
                    std::cout << "Motion detected on GPIO " << gpio_pin_ << std::endl;
                    
                    // 신호 데이터 수집
                    std::vector<int> signal_data;
                    auto start_time = std::chrono::steady_clock::now();
                    
                    // 신호 지속 시간 동안 데이터 수집
                    while (digitalRead(gpio_pin_) == HIGH && 
                           std::chrono::duration_cast<std::chrono::milliseconds>(
                               std::chrono::steady_clock::now() - start_time).count() < 1000) {
                        
                        signal_data.push_back(1);
                        delay(10); // 10ms 간격으로 샘플링
                    }
                    
                    // 신호 패턴 분석
                    if (!signal_data.empty()) {
                        std::string ir_signal = analyzeIRPattern(signal_data);
                        last_ir_signal_ = ir_signal;
                        detected_count_++;
                        
                        std::cout << "IR Signal detected: " << ir_signal 
                                  << " (count: " << detected_count_ << ")" << std::endl;
                        
                        // IR 신호 콜백 호출
                        if (ir_signal_callback_) {
                            ir_signal_callback_(ir_signal, detected_count_);
                        }
                    }
                }
                
                last_state = current_state;
                last_change = now;
            }
        }
        
        // CPU 사용량 조절
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
    
    std::cout << "PIR Sensor monitoring thread stopped" << std::endl;
}

std::string PIRSensor::analyzeIRPattern(const std::vector<int>& signal_data) {
    if (signal_data.empty()) {
        return "NO_SIGNAL";
    }
    
    // 노이즈 필터링
    auto filtered_data = filterNoise(signal_data);
    
    // 신호 길이 기반 패턴 분석
    int signal_length = filtered_data.size();
    
    // 간단한 패턴 분류 (실제로는 더 복잡한 알고리즘 필요)
    if (signal_length < 10) {
        return "SHORT_PULSE";
    } else if (signal_length < 50) {
        return "MEDIUM_PULSE";
    } else if (signal_length < 100) {
        return "LONG_PULSE";
    } else {
        return "EXTENDED_PULSE";
    }
}

std::vector<int> PIRSensor::filterNoise(const std::vector<int>& signal_data) {
    if (signal_data.size() < 3) {
        return signal_data;
    }
    
    std::vector<int> filtered;
    filtered.reserve(signal_data.size());
    
    // 이동 평균 필터 (3점)
    for (size_t i = 1; i < signal_data.size() - 1; ++i) {
        int avg = (signal_data[i-1] + signal_data[i] + signal_data[i+1]) / 3;
        if (avg > signal_threshold_) {
            filtered.push_back(1);
        } else {
            filtered.push_back(0);
        }
    }
    
    return filtered;
}

} // namespace irremote
