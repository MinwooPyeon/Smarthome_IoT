cmake_minimum_required(VERSION 3.16)
project(rpi_mqtt_sub LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ---- Common warnings (선택)
if (CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang")
  add_compile_options(-Wall -Wextra -Wpedantic)
endif()

# =========================
# Mosquitto (MQTT)
# sudo apt install -y libmosquitto-dev
# =========================
find_path(MOSQ_INCLUDE_DIR mosquitto.h)
find_library(MOSQ_LIB mosquitto)
if (NOT MOSQ_INCLUDE_DIR OR NOT MOSQ_LIB)
  message(FATAL_ERROR "libmosquitto not found. Install 'libmosquitto-dev'")
endif()

# =========================
# pigpio (GPIO / timing)
# sudo apt install -y pigpio
# =========================
find_path(PIGPIO_INCLUDE_DIR pigpio.h)
find_library(PIGPIO_LIB pigpio)
# pigpio는 pthread/rt 필요
find_package(Threads REQUIRED)
find_library(RT_LIB rt)

if (NOT PIGPIO_INCLUDE_DIR OR NOT PIGPIO_LIB)
  message(FATAL_ERROR "pigpio not found. Install 'pigpio'")
endif()
if (NOT RT_LIB)
  message(FATAL_ERROR "librt not found")
endif()

# =========================
# Sensors library (DHT11, IR Receiver, GpioPigpio, Manager)
# 파일 경로는 프로젝트 구조에 맞게 조정하세요.
# =========================
add_library(sensors
  src/sensors/sensorManager.cpp
  src/sensors/Dht11Sensor.cpp
  src/sensors/IrReceiverSensor.cpp
  src/interface/gpioPigpio.cpp      # 이미 있으시면 유지
)

target_include_directories(sensors
  PUBLIC
    include            # 권장: include/sensors/...
    header             # 기존에 쓰시던 경로가 있다면 함께 유지
    ${PIGPIO_INCLUDE_DIR}
)

target_link_libraries(sensors
  PUBLIC
    ${PIGPIO_LIB}
    Threads::Threads
    ${RT_LIB}
)

# 필요 시 다른 타겟에서 센서 타입/인터페이스를 사용하도록
# (예: target_link_libraries(다른타겟 PRIVATE sensors))

# =========================
# MQTT Subscriber library
# =========================
add_library(mqtt_sub
  src/mqtt/mqttSubscriber.cpp
)
target_include_directories(mqtt_sub PUBLIC
  include
  header
  ${MOSQ_INCLUDE_DIR}
)
target_link_libraries(mqtt_sub PRIVATE ${MOSQ_LIB})

add_executable(test_sub src/mqtt/test_sub.cpp)
target_link_libraries(test_sub PRIVATE mqtt_sub)

# =========================
# MQTT Publisher library
# =========================
add_library(mqtt_pub
  src/mqtt/mqttPublisher.cpp
)
target_include_directories(mqtt_pub PUBLIC
  include
  header
  ${MOSQ_INCLUDE_DIR}
)
target_link_libraries(mqtt_pub PRIVATE ${MOSQ_LIB})

add_executable(test_pub src/mqtt/test_pub.cpp)
target_link_libraries(test_pub PRIVATE mqtt_pub)

# =========================
# [예시] 센서 단독 테스트 실행파일
# DHT11/IR 수신 동작만 확인하고 싶을 때 사용
# =========================
add_executable(test_sensors
  src/sensors/test_sensors.cpp   # 예: 센서 매니저 초기화/시작/읽기 코드
)
target_link_libraries(test_sensors PRIVATE sensors)

# =========================
# [선택] MQTT + Sensors 통합 예시 실행파일
# =========================
#add_executable(hub_node src/app/hub_node.cpp)
#target_link_libraries(hub_node PRIVATE sensors mqtt_pub mqtt_sub)
#target_include_directories(hub_node PRIVATE include header)
