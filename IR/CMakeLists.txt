cmake_minimum_required(VERSION 3.16)
project(esp32-ir-remote VERSION 1.0.0 LANGUAGES CXX)

# ESP-IDF 프로젝트 설정 (ESP32 빌드 시)
if(CMAKE_SYSTEM_NAME STREQUAL "Generic")
    include($ENV{IDF_PATH}/tools/cmake/project.cmake)
    return()
endif()

# C++ 표준 설정
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 빌드 타입 설정
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

# 컴파일러 플래그
set(CMAKE_CXX_FLAGS_DEBUG "-g -O0 -Wall -Wextra -DDEBUG")
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -DNDEBUG")

# Find required packages
find_package(Threads REQUIRED)

# MQTT 라이브러리 찾기 (ESP32에서는 ESP-IDF 내장 MQTT 사용)
# ESP32 프로젝트에서는 ESP-IDF의 MQTT 컴포넌트를 사용하므로 외부 라이브러리 불필요

# Include directories
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/header)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/src)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/external)

# JSON 라이브러리 설정 (nlohmann/json.hpp 사용)
set(NLOHMANN_JSON_INCLUDE_DIRS ${CMAKE_CURRENT_SOURCE_DIR}/external/nlohmann)

# Library sources
set(LIB_SOURCES
    src/config.cpp
    src/mqtt_client.cpp
    src/mqtt_message.cpp
    src/matter_client.cpp
    src/appliance_controller.cpp
    src/remote.cpp
)

# ESP32 specific sources (only for ESP32 builds)
if(CMAKE_SYSTEM_NAME STREQUAL "Generic")
    list(APPEND LIB_SOURCES
        src/esp32_ir_receiver.cpp
        src/esp32_ir_store.cpp
        src/pir_sensor.cpp
    )
endif()

# Create library
add_library(irremote_lib STATIC ${LIB_SOURCES})

# Set library properties
set_target_properties(irremote_lib PROPERTIES
    VERSION ${PROJECT_VERSION}
    SOVERSION ${PROJECT_VERSION_MAJOR}
    PUBLIC_HEADER "${CMAKE_CURRENT_SOURCE_DIR}/header/core/config.h;${CMAKE_CURRENT_SOURCE_DIR}/header/network/mqtt_client.h;${CMAKE_CURRENT_SOURCE_DIR}/header/network/matter_client.h;${CMAKE_CURRENT_SOURCE_DIR}/header/hardware/esp32_ir_receiver.h;${CMAKE_CURRENT_SOURCE_DIR}/header/hardware/esp32_ir_store.h;${CMAKE_CURRENT_SOURCE_DIR}/header/hardware/appliance_controller.h"
)

# Link libraries for library
target_link_libraries(irremote_lib
    Threads::Threads
)

# ESP32에서는 ESP-IDF 내장 MQTT 컴포넌트 사용
# 외부 MQTT 라이브러리 링크 불필요

# Main executable
set(MAIN_SOURCES src/main.cpp)
add_executable(irremote_sender ${MAIN_SOURCES})

# Link main executable with library
target_link_libraries(irremote_sender irremote_lib)

# Install targets
install(TARGETS irremote_lib irremote_sender
    EXPORT irremoteTargets
    LIBRARY DESTINATION lib
    ARCHIVE DESTINATION lib
    RUNTIME DESTINATION bin
    PUBLIC_HEADER DESTINATION include/irremote
)

# Install headers
install(DIRECTORY header/
    DESTINATION include/irremote
    FILES_MATCHING PATTERN "*.h"
)

# Install configuration files
# install(FILES 
#     config/linux/systemd/irremote.service 
#     DESTINATION /etc/systemd/system
# )

# Install scripts
# install(DIRECTORY scripts/
#     DESTINATION bin/irremote
#     USE_SOURCE_PERMISSIONS
# )

# Export targets
install(EXPORT irremoteTargets
    FILE irremoteTargets.cmake
    DESTINATION lib/cmake/irremote
)

# Add tests (ESP32에서는 제외)
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/tests" AND NOT CMAKE_SYSTEM_NAME STREQUAL "Generic")
    add_subdirectory(tests)
endif()

# Print configuration summary
message(STATUS "")
message(STATUS "Configuration Summary:")
message(STATUS "  Project: ${PROJECT_NAME} ${PROJECT_VERSION}")
message(STATUS "  Build type: ${CMAKE_BUILD_TYPE}")
message(STATUS "  C++ standard: ${CMAKE_CXX_STANDARD}")
message(STATUS "")

# ESP32 specific build configuration
if(CMAKE_SYSTEM_NAME STREQUAL "Generic")
    # ESP-IDF build
    message(STATUS "Building for ESP32 with ESP-IDF")
    # ESP-IDF specific configurations can be added here
else()
    # Standard CMake build
    message(STATUS "Building with standard CMake")
endif()
