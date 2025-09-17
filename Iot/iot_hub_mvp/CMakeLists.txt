cmake_minimum_required(VERSION 3.16)
project(hub_agent LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(PkgConfig REQUIRED)
pkg_check_modules(MOSQUITTO REQUIRED libmosquitto)

# nlohmann/json (패키지 있으면)
find_package(nlohmann_json QUIET)
if(NOT nlohmann_json_FOUND)
  add_library(nlohmann_json::nlohmann_json INTERFACE IMPORTED)
  target_include_directories(nlohmann_json::nlohmann_json INTERFACE /usr/include)
endif()

add_executable(hub_agent
  src/main.cpp
  src/mqtt_client.cpp
  src/dht11_reader.cpp
  src/ir_receiver.cpp
  src/util.cpp
)
target_include_directories(hub_agent PRIVATE include ${MOSQUITTO_INCLUDE_DIRS})
target_link_libraries(hub_agent
  ${MOSQUITTO_LIBRARIES}
  nlohmann_json::nlohmann_json
  pigpio rt pthread
)

add_executable(dht11_test src/dht11_test.cpp src/dht11_reader.cpp)
target_include_directories(dht11_test PRIVATE include)
target_link_libraries(dht11_test pigpio rt pthread)

add_executable(ir_test src/ir_test.cpp src/ir_receiver.cpp)
target_include_directories(ir_test PRIVATE include)
target_link_libraries(ir_test pigpio rt pthread)

add_executable(mqtt_pub_test  src/mqtt_pub_test.cpp  src/mqtt_client.cpp)
target_include_directories(mqtt_pub_test PRIVATE include)
target_link_libraries(mqtt_pub_test mosquitto)

add_executable(mqtt_sub_test  src/mqtt_sub_test.cpp  src/mqtt_client.cpp)
target_include_directories(mqtt_sub_test PRIVATE include)
target_link_libraries(mqtt_sub_test mosquitto)
