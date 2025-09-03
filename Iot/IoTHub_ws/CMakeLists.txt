cmake_minimum_required(VERSION 3.16)
project(rpi_mqtt_sub LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# libmosquitto (라즈베리파이: sudo apt install -y libmosquitto-dev)
find_path(MOSQ_INCLUDE_DIR mosquitto.h)
find_library(MOSQ_LIB mosquitto)
if (NOT MOSQ_INCLUDE_DIR OR NOT MOSQ_LIB)
  message(FATAL_ERROR "libmosquitto not found. Install libmosqutto-dev")
endif()

add_library(mqtt_sub
  src/mqtt/mqttSubscriber.cpp
)
target_include_directories(mqtt_sub PUBLIC header ${MOSQ_INCLUDE_DIR})
target_link_libraries(mqtt_sub PRIVATE ${MOSQ_LIB})

add_executable(test_sub src/mqtt/test_sub.cpp)
target_link_libraries(test_sub PRIVATE mqtt_sub)

add_library(mqtt_pub
  src/mqtt/mqttPublisher.cpp
)
target_include_directories(mqtt_pub PUBLIC header ${MOSQ_INCLUDE_DIR})
target_link_libraries(mqtt_pub PRIVATE ${MOSQ_LIB})

add_executable(test_pub src/mqtt/test_pub.cpp)
target_link_libraries(test_pub PRIVATE mqtt_pub)