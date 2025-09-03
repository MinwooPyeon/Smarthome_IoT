cmake_minimum_required(VERSION 3.16)
project(rpi-ir-remote-cpp VERSION 1.0.0 LANGUAGES CXX)

# C++ 표준 설정
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 빌드 타입 설정
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

# 플랫폼 감지
if(WIN32)
    message(STATUS "Building for Windows platform")
    
    # Windows 전용 설정
    set(PLATFORM_SPECIFIC_SOURCES "")
    
    # MATLAB Engine 찾기
    find_package(Matlab QUIET)
    if(Matlab_FOUND)
        message(STATUS "MATLAB found: ${Matlab_VERSION}")
    else()
        message(STATUS "MATLAB not found, building without MATLAB support")
        add_definitions(-DMATLAB_DISABLED)
    endif()
    
    # Windows용 JSON 라이브러리
    if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/external/nlohmann/json.hpp")
        include_directories(${CMAKE_CURRENT_SOURCE_DIR}/external/nlohmann)
        message(STATUS "Using local nlohmann-json header")
    endif()
    
    # MQTT 기능 비활성화
    add_definitions(-DMQTT_DISABLED)
    set(MQTT_DISABLED TRUE)
    
else()
    message(STATUS "Building for Linux/Unix platform")
    
    # Linux 전용 설정
    set(PLATFORM_SPECIFIC_SOURCES
        src/ui/linux_gui.cpp
    )
    
    # PkgConfig 사용
    find_package(PkgConfig REQUIRED)
    find_package(Threads REQUIRED)
    
    # Find system libraries
    pkg_check_modules(MOSQUITTO REQUIRED libmosquitto)
    pkg_check_modules(NLOHMANN_JSON REQUIRED nlohmann_json)
endif()

# Include directories
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/src)

# Library sources
set(LIB_SOURCES
    src/config.cpp
    src/matter_client.cpp
    src/ir_receiver.cpp
    src/appliance_controller.cpp
    ${PLATFORM_SPECIFIC_SOURCES}
)

# Create library
add_library(irremote_lib STATIC ${LIB_SOURCES})

# Set library properties
set_target_properties(irremote_lib PROPERTIES
    VERSION ${PROJECT_VERSION}
    SOVERSION ${PROJECT_VERSION_MAJOR}
)

# Link libraries for library
if(WIN32)
    target_link_libraries(irremote_lib
        ws2_32
        iphlpapi
    )
    
    # Windows 전용 컴파일 옵션
    if(CMAKE_CXX_COMPILER_ID MATCHES "MSVC")
        target_compile_options(irremote_lib PRIVATE
            /MP  # 멀티 프로세서 컴파일
            /EHsc # 예외 처리 모델
        )
    else()
        # MinGW용 컴파일 옵션
        target_compile_options(irremote_lib PRIVATE
            -Wall
            -Wextra
            -std=c++17
        )
    endif()
    
    # Windows 전용 정의
    target_compile_definitions(irremote_lib PRIVATE
        _WIN32_WINNT=0x0601  # Windows 7 이상
        WIN32_LEAN_AND_MEAN
    )
else()
    target_link_libraries(irremote_lib
        ${NLOHMANN_JSON_LIBRARIES}
        ${MOSQUITTO_LIBRARIES}
        Threads::Threads
        ssl
        crypto
    )
    
    # Linux 전용 컴파일 옵션
    target_compile_options(irremote_lib PRIVATE
        ${NLOHMANN_JSON_CFLAGS_OTHER}
        ${MOSQUITTO_CFLAGS_OTHER}
    )
endif()

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
)

# Install headers
install(DIRECTORY include/
    DESTINATION include/irremote
    FILES_MATCHING PATTERN "*.h"
)

# Install configuration files
if(NOT WIN32)
    install(FILES 
        config/systemd/irremote.service 
        DESTINATION /etc/systemd/system
    )
endif()

# Export targets
install(EXPORT irremoteTargets
    FILE irremoteTargets.cmake
    NAMESPACE ::
    DESTINATION lib/cmake/irremote
)

# Print configuration summary
message(STATUS "")
message(STATUS "=== Configuration Summary ===")
message(STATUS "  Project: ${PROJECT_NAME} ${PROJECT_VERSION}")
message(STATUS "  Build type: ${CMAKE_BUILD_TYPE}")
message(STATUS "  Platform: ${CMAKE_SYSTEM_NAME}")
if(WIN32)
    message(STATUS "  MATLAB support: ${Matlab_FOUND}")
    if(MSVC)
        message(STATUS "  Compiler: MSVC")
    else()
        message(STATUS "  Compiler: MinGW")
    endif()
else()
    message(STATUS "  MQTT support: ${MOSQUITTO_FOUND}")
    message(STATUS "  JSON support: ${NLOHMANN_JSON_FOUND}")
endif()
message(STATUS "=============================")
message(STATUS "")
