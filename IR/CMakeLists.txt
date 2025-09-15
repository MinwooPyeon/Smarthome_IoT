cmake_minimum_required(VERSION 3.16)
project(IRRemoteController)

# C++ 표준 설정
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 플랫폼 감지
if(WIN32)
    add_definitions(-DPLATFORM_WINDOWS)
    set(PLATFORM_NAME "Windows")
elseif(UNIX AND NOT APPLE)
    add_definitions(-DPLATFORM_LINUX)
    set(PLATFORM_NAME "Linux")
elseif(APPLE)
    add_definitions(-DPLATFORM_MACOS)
    set(PLATFORM_NAME "macOS")
endif()

# 디버그/릴리즈 설정
if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_definitions(-DDEBUG_MODE=1)
    add_definitions(-DVERBOSE_LOGGING=1)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -O0")
else()
    add_definitions(-DRELEASE_MODE=1)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2")
endif()

# 헤더 디렉토리 추가
include_directories(header)
include_directories(external)

# 소스 파일들
set(SOURCES
    src/config.cpp
    src/irsend.cpp
    src/ir_receiver.cpp
    src/mqtt_client.cpp
    src/mqtt_message.cpp
    src/appliance_controller.cpp
    src/generic_device.cpp
    src/remote.cpp
    src/serial_controller.cpp
    src/ir_learner.cpp
    src/pir_sensor.cpp
    src/matter_client.cpp
    src/esp32_ir_receiver.cpp
    src/esp32_ir_store.cpp
    src/esp32_wifi_mqtt.cpp
    src/security.cpp
)

# 헤더 파일들
set(HEADERS
    header/core/config.h
    header/core/esp32_ir_system.h
    header/core/ir_code_store.h
    header/core/ir_system.h
    header/core/platform.h
    header/core/remote.h
    header/core/security.h
    header/hardware/appliance_controller.h
    header/hardware/esp32_ir_receiver.h
    header/hardware/esp32_ir_store.h
    header/hardware/esp32_ir_transmitter.h
    header/hardware/generic_device.h
    header/hardware/ir_database.h
    header/hardware/ir_learner.h
    header/hardware/ir_protocol_detector.h
    header/hardware/ir_receiver.h
    header/hardware/irsend.h
    header/hardware/pir_sensor.h
    header/network/matter_client.h
    header/network/mqtt_client.h
    header/network/mqtt_message.h
    header/network/serial_controller.h
)

# 메인 실행 파일 생성
add_executable(ir_remote_controller
    tests/test_simulation.cpp
    ${SOURCES}
    ${HEADERS}
)

# Windows에서 필요한 라이브러리
if(WIN32)
    # Windows에서는 추가 라이브러리가 필요하지 않음 (시뮬레이션 모드)
    # 보안 기능을 위해 advapi32와 crypt32 라이브러리 링크
    target_link_libraries(ir_remote_controller advapi32 crypt32)
else()
    # Linux에서 OpenSSL 라이브러리 링크
    find_package(OpenSSL REQUIRED)
    target_link_libraries(ir_remote_controller OpenSSL::SSL OpenSSL::Crypto)
endif()

# 테스트 실행 파일 생성 (Google Test 사용)
find_package(GTest QUIET)
if(GTest_FOUND)
    enable_testing()

    # 단위 테스트
    add_executable(unit_tests
        tests/unit/test_ir_receiver.cpp
        tests/unit/test_appliance_controller.cpp
        tests/unit/test_mqtt_client.cpp
        ${SOURCES}
        ${HEADERS}
    )

    target_link_libraries(unit_tests GTest::GTest GTest::Main)

    # 통합 테스트
    add_executable(integration_tests
        tests/integration/test_system_integration.cpp
        ${SOURCES}
        ${HEADERS}
    )

    target_link_libraries(integration_tests GTest::GTest GTest::Main)

    # 테스트 추가
    add_test(NAME UnitTests COMMAND unit_tests)
    add_test(NAME IntegrationTests COMMAND integration_tests)
else()
    message(WARNING "Google Test not found. Tests will not be built.")
endif()

# 설치 설정
install(TARGETS ir_remote_controller
    RUNTIME DESTINATION bin
)

# 출력 디렉토리 설정
set_target_properties(ir_remote_controller PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
)

if(TARGET unit_tests)
    set_target_properties(unit_tests PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
    )
endif()

if(TARGET integration_tests)
    set_target_properties(integration_tests PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
    )
endif()
