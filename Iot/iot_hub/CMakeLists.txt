cmake_minimum_required(VERSION 3.16)
project(rpi_mqtt_sub LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ---- Common warnings (선택)
if (CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang")
  add_compile_options(-Wall -Wextra -Wpedantic)
endif()

# =========================
# 옵션: DHT11을 Lab04(pigpio 직결) 경로로 사용
#  - ON  : Dht11Sensor.cpp 내부에서 Lab04 스타일 폴링 사용 (권장: 마감 촉박 시)
#  - OFF : 기존 iGpio 경로 유지
# =========================
option(DHT11_USE_LAB04 "Use Lab04-style pigpio polling for DHT11" ON)   # ✅ ADD

# =========================
# Mosquitto (MQTT)
# =========================
find_path(MOSQ_INCLUDE_DIR mosquitto.h)
find_library(MOSQ_LIB mosquitto)
if (NOT MOSQ_INCLUDE_DIR OR NOT MOSQ_LIB)
  message(FATAL_ERROR "libmosquitto not found. Install 'libmosquitto-dev'")
endif()

# =========================
# pigpio (GPIO / timing)
# =========================
find_path(PIGPIO_INCLUDE_DIR pigpio.h)
find_library(PIGPIO_LIB pigpio)
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
# =========================
add_library(sensors
  src/sensors/sensorManager.cpp
  src/sensors/Dht11Sensor.cpp
  src/sensors/IrReceiverSensor.cpp
  src/interface/gpioPigpio.cpp
  src/sensors/CDht11.cpp            # ✅ ADD: Lab04 클래스 포함
)

target_include_directories(sensors
  PUBLIC
    include
    header                          # ✅ ensure header/sensors/CDht11.hpp 가 보이도록
    ${PIGPIO_INCLUDE_DIR}
)

target_link_libraries(sensors
  PUBLIC
    ${PIGPIO_LIB}
    Threads::Threads
    ${RT_LIB}
)

# Lab04 경로를 사용할지 여부를 컴파일 정의로 전달
if (DHT11_USE_LAB04)                # ✅ ADD
  target_compile_definitions(sensors PUBLIC DHT11_USE_LAB04=1)
endif()

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
# 센서 단독 테스트 실행파일
# =========================
add_executable(test_sensors
  src/sensors/test_sensors.cpp
)
target_link_libraries(test_sensors PRIVATE sensors)

# =========================
# (선택) Lab04 예제 메인으로 바로 확인해보기
# - Lab04 repo에서 main 예제를 src/sensors/dht11_lab04_main.cpp로 복사했다면 켜세요.
# =========================
 if (EXISTS "${CMAKE_SOURCE_DIR}/src/sensors/lab04.cpp")      # ✅ ADD (옵션)
   add_executable(dht11_lab04_demo
     src/sensors/lab04.cpp
     src/sensors/CDht11.cpp
   )
   target_include_directories(dht11_lab04_demo PRIVATE header ${PIGPIO_INCLUDE_DIR})
   target_link_libraries(dht11_lab04_demo PRIVATE ${PIGPIO_LIB} Threads::Threads ${RT_LIB})
 endif()
