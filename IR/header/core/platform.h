#pragma once

// 플랫폼 감지 및 매크로 정의
#if defined(ESP32) || defined(ESP_PLATFORM)
    #define PLATFORM_ESP32
    #define PLATFORM_NAME "ESP32"
#elif defined(__linux__)
    #define PLATFORM_LINUX
    #define PLATFORM_NAME "Linux"
#elif defined(_WIN32) || defined(_WIN64)
    #define PLATFORM_WINDOWS
    #define PLATFORM_NAME "Windows"
#else
    #define PLATFORM_UNKNOWN
    #define PLATFORM_NAME "Unknown"
#endif

// 로깅 매크로 정의 (ESP32에서는 esp_log.h의 매크로를 직접 사용)
#ifndef PLATFORM_ESP32
    #include <iostream>
    #include <cstdio>
    #define LOG_INFO(tag, format, ...) printf("[INFO] " format "\n", ##__VA_ARGS__)
    #define LOG_ERROR(tag, format, ...) printf("[ERROR] " format "\n", ##__VA_ARGS__)
    #define LOG_WARN(tag, format, ...) printf("[WARN] " format "\n", ##__VA_ARGS__)
    #define LOG_DEBUG(tag, format, ...) printf("[DEBUG] " format "\n", ##__VA_ARGS__)
#endif

// 플랫폼별 특성 정의
#ifdef PLATFORM_ESP32
    #define HAS_RMT_SUPPORT 1
    #define HAS_WIFI_SUPPORT 1
    #define HAS_BLUETOOTH_SUPPORT 1
#elif defined(PLATFORM_LINUX)
    #define HAS_LIRC_SUPPORT 1
    #define HAS_WIFI_SUPPORT 1
#elif defined(PLATFORM_WINDOWS)
    #define HAS_SIMULATION_MODE 1
#endif

// 컴파일러 특성
#ifdef __cplusplus
    #define CXX_STANDARD __cplusplus
#else
    #define CXX_STANDARD 0
#endif

// 버전 정보
#define PLATFORM_VERSION_MAJOR 1
#define PLATFORM_VERSION_MINOR 0
#define PLATFORM_VERSION_PATCH 0

// 유틸리티 매크로
#define UNUSED(x) (void)(x)
#define ARRAY_SIZE(arr) (sizeof(arr) / sizeof((arr)[0]))

// 플랫폼별 타입 정의
#ifdef PLATFORM_ESP32
    #include <cstdint>
    typedef uint32_t platform_time_t;
    typedef int32_t platform_duration_t;
#else
    #include <chrono>
    typedef std::chrono::milliseconds platform_time_t;
    typedef std::chrono::milliseconds platform_duration_t;
#endif
