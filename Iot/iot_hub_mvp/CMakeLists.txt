cmake_minimum_required(VERSION 3.16)
project(hub_agent LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# --- mosquitto (pkg-config)
find_package(PkgConfig REQUIRED)
pkg_check_modules(MOSQUITTO REQUIRED libmosquitto)

# --- nlohmann/json (있으면 사용, 없으면 /usr/include 가정)
find_package(nlohmann_json QUIET)
if(NOT nlohmann_json_FOUND)
  add_library(nlohmann_json::nlohmann_json INTERFACE IMPORTED)
  target_include_directories(nlohmann_json::nlohmann_json INTERFACE /usr/include)
endif()

# =========================================
# 메인 실행파일: hub_main
# =========================================
add_executable(hub_main
  main.cpp

  # ---- MQTT
  src/mqtt/mqtt_client.cpp
  src/mqtt/mqtt_handler.cpp

  # ---- MANAGERs (DI 버전)
  src/manager/mqtt_manager.cpp
  src/manager/actuator_manager.cpp
  src/manager/data_manager.cpp
  src/manager/csv_manager.cpp

  # ---- 기타 유틸/분석 (있으면 사용)
  src/analyzer.cpp
  src/util.cpp

  # ---- 센서(있으면 사용)
  src/actuator/dht11_reader.cpp
  src/actuator/ir_receiver.cpp
)

target_include_directories(hub_main PRIVATE
  include
  ${MOSQUITTO_INCLUDE_DIRS}
)

target_link_libraries(hub_main
  ${MOSQUITTO_LIBRARIES}
  nlohmann_json::nlohmann_json
  pigpio
  rt
  pthread
)

# =========================================
# 보조 테스트 실행파일들 (선택)
# 파일 없으면 해당 타깃만 지우세요.
# =========================================

# DHT11 단독 테스트
#add_executable(dht11_test
#  src/tests/dht11_test.cpp
#  src/sensors/dht11_reader.cpp
#)
#target_include_directories(dht11_test PRIVATE include)
#target_link_libraries(dht11_test pigpio rt pthread)

# IR 수신 테스트
#add_executable(ir_test
#  src/tests/ir_test.cpp
#  src/sensors/ir_receiver.cpp
#)
#target_include_directories(ir_test PRIVATE include)
#target_link_libraries(ir_test pigpio rt pthread)

# MQTT 퍼블리시 테스트
#add_executable(mqtt_pub_test
#  src/tests/mqtt_pub_test.cpp
#  src/mqtt/mqtt_client.cpp
#)
#target_include_directories(mqtt_pub_test PRIVATE include ${MOSQUITTO_INCLUDE_DIRS})
#target_link_libraries(mqtt_pub_test ${MOSQUITTO_LIBRARIES})

# MQTT 서브스크라이브 테스트
#add_executable(mqtt_sub_test
#  src/tests/mqtt_sub_test.cpp
#  src/mqtt/mqtt_client.cpp
#)
#target_include_directories(mqtt_sub_test PRIVATE include ${MOSQUITTO_INCLUDE_DIRS})
#target_link_libraries(mqtt_sub_test ${MOSQUITTO_LIBRARIES})
