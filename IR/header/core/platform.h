#ifndef PLATFORM_H
#define PLATFORM_H

// 플랫폼 감지
#if defined(ESP32) || defined(CONFIG_IDF_TARGET_ESP32)
    #define PLATFORM_ESP32
#elif defined(_WIN32) || defined(_WIN64)
    #define PLATFORM_WINDOWS
#elif defined(__linux__)
    #define PLATFORM_LINUX
#else
    #define PLATFORM_UNKNOWN
#endif

// 플랫폼별 기능 활성화
#ifdef PLATFORM_ESP32
    #define HAS_WIFI true
    #define HAS_GPIO true
    #define HAS_IR true
    #define HAS_MQTT true
#elif defined(PLATFORM_LINUX)
    #define HAS_WIFI false
    #define HAS_GPIO true
    #define HAS_IR true
    #define HAS_MQTT true
#elif defined(PLATFORM_WINDOWS)
    #define HAS_WIFI false
    #define HAS_GPIO false
    #define HAS_IR false
    #define HAS_MQTT true
#else
    #define HAS_WIFI false
    #define HAS_GPIO false
    #define HAS_IR false
    #define HAS_MQTT false
#endif

// ESP32 전용 매크로
#ifdef PLATFORM_ESP32
    #include "esp_log.h"
    #include "driver/gpio.h"
    #include "freertos/FreeRTOS.h"
    #include "freertos/task.h"
    #include "freertos/queue.h"
    
    #define LOG_TAG "IRRemote"
    #define LOG_INFO(format, ...) ESP_LOGI(LOG_TAG, format, ##__VA_ARGS__)
    #define LOG_ERROR(format, ...) ESP_LOGE(LOG_TAG, format, ##__VA_ARGS__)
    #define LOG_DEBUG(format, ...) ESP_LOGD(LOG_TAG, format, ##__VA_ARGS__)
    
    // GPIO 매크로
    #define GPIO_HIGH GPIO_PIN_SET
    #define GPIO_LOW GPIO_PIN_RESET
    #define GPIO_INPUT GPIO_MODE_INPUT
    #define GPIO_OUTPUT GPIO_MODE_OUTPUT
    
    // 지연 함수
    #define delay_ms(ms) vTaskDelay(pdMS_TO_TICKS(ms))
    #define delay_us(us) ets_delay_us(us)
    
#elif defined(PLATFORM_LINUX)
    #include <iostream>
    #include <wiringPi.h>
    
    #define LOG_INFO(format, ...) std::cout << "[INFO] " << format << std::endl
    #define LOG_ERROR(format, ...) std::cerr << "[ERROR] " << format << std::endl
    #define LOG_DEBUG(format, ...) std::cout << "[DEBUG] " << format << std::endl
    
    #define GPIO_HIGH HIGH
    #define GPIO_LOW LOW
    #define GPIO_INPUT INPUT
    #define GPIO_OUTPUT OUTPUT
    
    #define delay_ms(ms) delay(ms)
    #define delay_us(us) delayMicroseconds(us)
    
#elif defined(PLATFORM_WINDOWS)
    #include <iostream>
    #include <chrono>
    #include <thread>
    
    #define LOG_INFO(format, ...) std::cout << "[INFO] " << format << std::endl
    #define LOG_ERROR(format, ...) std::cerr << "[ERROR] " << format << std::endl
    #define LOG_DEBUG(format, ...) std::cout << "[DEBUG] " << format << std::endl
    
    // Windows 시뮬레이션용 GPIO 매크로
    #define GPIO_HIGH 1
    #define GPIO_LOW 0
    #define GPIO_INPUT 0
    #define GPIO_OUTPUT 1
    
    #define delay_ms(ms) std::this_thread::sleep_for(std::chrono::milliseconds(ms))
    #define delay_us(us) std::this_thread::sleep_for(std::chrono::microseconds(us))
    
    // Windows 시뮬레이션용 GPIO 함수
    inline void gpio_set_direction(int pin, int mode) {
        std::cout << "[GPIO Sim] Pin " << pin << " set to mode " << mode << std::endl;
    }
    
    inline void gpio_set_level(int pin, int level) {
        std::cout << "[GPIO Sim] Pin " << pin << " set to " << (level ? "HIGH" : "LOW") << std::endl;
    }
    
    inline int gpio_get_level(int pin) {
        static int counter = 0;
        counter++;
        return (counter % 100 < 5) ? GPIO_HIGH : GPIO_LOW; // 5% 확률로 HIGH
    }
    
    #define gpio_set_direction(pin, mode) gpio_set_direction(pin, mode)
    #define gpio_set_level(pin, level) gpio_set_level(pin, level)
    #define gpio_get_level(pin) gpio_get_level(pin)
#endif

#endif // PLATFORM_H
