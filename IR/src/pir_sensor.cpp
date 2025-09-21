#include "hardware/pir_sensor.h"
#include <iostream>
#include <chrono>
#include <algorithm>
#include <cmath>
#include <vector>

#ifdef __linux__
#include <wiringPi.h>
#include <softPwm.h>
#elif defined(ESP32) || defined(ESP_PLATFORM)
#include "driver/gpio.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#endif

#ifdef ESP32
static const char* PIR_TAG = "PIR_SENSOR";

#define INPUT 0
#define OUTPUT 1
#define INPUT_PULLUP 2
#define HIGH 1
#define LOW 0

inline void pinMode(int pin, int mode) {
    gpio_config_t io_conf = {};
    io_conf.intr_type = GPIO_INTR_DISABLE;
    io_conf.mode = (mode == INPUT) ? GPIO_MODE_INPUT : GPIO_MODE_OUTPUT;
    io_conf.pin_bit_mask = (1ULL << pin);
    io_conf.pull_down_en = GPIO_PULLDOWN_DISABLE;
    io_conf.pull_up_en = (mode == INPUT_PULLUP) ? GPIO_PULLUP_ENABLE : GPIO_PULLUP_DISABLE;

    esp_err_t ret = gpio_config(&io_conf);
    if (ret != ESP_OK) {
        ESP_LOGE(PIR_TAG, "GPIO 설정 실패: pin=%d, mode=%d, error=%s", pin, mode, esp_err_to_name(ret));
    } else {
        ESP_LOGI(PIR_TAG, "GPIO 설정 완료: pin=%d, mode=%d", pin, mode);
    }
}

inline int digitalRead(int pin) {
    return gpio_get_level((gpio_num_t)pin);
}

inline void delay(int ms) {
    vTaskDelay(pdMS_TO_TICKS(ms));
}
#endif

#ifdef ESP32
static void pir_monitoring_task(void* parameter) {
    PIRSensor* sensor = static_cast<PIRSensor*>(parameter);
    sensor->monitoringThread();
}
#endif

PIRSensor::PIRSensor()
    : gpio_pin_(-1), sensitivity_(0.5), enabled_(false), running_(false),
      detected_count_(0), signal_threshold_(100), debounce_time_ms_(50) {
#ifdef ESP32
    monitoring_task_handle_ = nullptr;
#else
    monitoring_thread_ = nullptr;
#endif
}

PIRSensor::~PIRSensor() {
    cleanup();
}

PIRSensor::PIRSensor(PIRSensor&& other) noexcept
    : gpio_pin_(other.gpio_pin_), sensitivity_(other.sensitivity_),
      enabled_(other.enabled_.load()), running_(other.running_.load()),
      ir_signal_callback_(std::move(other.ir_signal_callback_)),
      status_callback_(std::move(other.status_callback_)),
      last_ir_signal_(std::move(other.last_ir_signal_)),
      detected_count_(other.detected_count_.load()),
      signal_buffer_(std::move(other.signal_buffer_)),
      signal_threshold_(other.signal_threshold_),
      debounce_time_ms_(other.debounce_time_ms_) {
#ifdef ESP32
    monitoring_task_handle_ = other.monitoring_task_handle_;
    other.monitoring_task_handle_ = nullptr;
#else
    monitoring_thread_ = std::move(other.monitoring_thread_);
#endif

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
#ifdef ESP32
        monitoring_task_handle_ = other.monitoring_task_handle_;
        other.monitoring_task_handle_ = nullptr;
#else
        monitoring_thread_ = std::move(other.monitoring_thread_);
#endif
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

    pinMode(gpio_pin_, INPUT);

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

#ifdef ESP32
    if (monitoring_task_handle_ != nullptr) {
        vTaskDelete(monitoring_task_handle_);
        monitoring_task_handle_ = nullptr;
    }
#else
    if (monitoring_thread_ && monitoring_thread_->joinable()) {
        monitoring_thread_->join();
    }
#endif

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

#ifdef ESP32
    BaseType_t ret = xTaskCreate(
        pir_monitoring_task,
        "PIR_Monitor",
        4096,
        this,
        1,
        &monitoring_task_handle_
    );

    if (ret != pdPASS) {
        ESP_LOGE(PIR_TAG, "PIR 모니터링 태스크 생성 실패");
        running_ = false;
        enabled_ = false;
        return;
    }
#else
    monitoring_thread_ = std::unique_ptr<std::thread>(new std::thread(&PIRSensor::monitoringThread, this));
#endif

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

#ifdef ESP32
    if (monitoring_task_handle_ != nullptr) {
        vTaskDelete(monitoring_task_handle_);
        monitoring_task_handle_ = nullptr;
    }
#else
    if (monitoring_thread_ && monitoring_thread_->joinable()) {
        monitoring_thread_->join();
    }
#endif

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
#ifdef ESP32
    ESP_LOGI(PIR_TAG, "PIR Sensor monitoring thread started");
#else
    std::cout << "PIR Sensor monitoring thread started" << std::endl;
#endif

    int last_state = LOW;
    auto last_change = std::chrono::steady_clock::now();

    while (running_) {
        if (!enabled_) {
#ifdef ESP32
            vTaskDelay(pdMS_TO_TICKS(100));
#else
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
#endif
            continue;
        }

        int current_state = digitalRead(gpio_pin_);

        if (current_state != last_state) {
            auto now = std::chrono::steady_clock::now();
            auto time_diff = std::chrono::duration_cast<std::chrono::milliseconds>(now - last_change).count();

            if (time_diff > debounce_time_ms_) {
                if (current_state == HIGH) {
                    std::cout << "Motion detected on GPIO " << gpio_pin_ << std::endl;

                    std::vector<int> signal_data;
                    auto start_time = std::chrono::steady_clock::now();

                    while (digitalRead(gpio_pin_) == HIGH &&
                           std::chrono::duration_cast<std::chrono::milliseconds>(
                               std::chrono::steady_clock::now() - start_time).count() < 1000) {

                        signal_data.push_back(1);
                        delay(10);
                    }

                    if (!signal_data.empty()) {
                        std::string ir_signal = analyzeIRPattern(signal_data);
                        last_ir_signal_ = ir_signal;
                        detected_count_++;

                        std::cout << "IR Signal detected: " << ir_signal
                                  << " (count: " << detected_count_ << ")" << std::endl;

                        if (ir_signal_callback_) {
                            ir_signal_callback_(ir_signal, detected_count_);
                        }
                    }
                }

                last_state = current_state;
                last_change = now;
            }
        }

#ifdef ESP32
        vTaskDelay(pdMS_TO_TICKS(10));
#else
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
#endif
    }

#ifdef ESP32
    ESP_LOGI(PIR_TAG, "PIR Sensor monitoring thread stopped");
#else
    std::cout << "PIR Sensor monitoring thread stopped" << std::endl;
#endif
}

std::string PIRSensor::analyzeIRPattern(const std::vector<int>& signal_data) {
    if (signal_data.empty()) {
        return "NO_SIGNAL";
    }

    auto filtered_data = filterNoise(signal_data);

    int signal_length = filtered_data.size();

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
