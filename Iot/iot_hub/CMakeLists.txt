cmake_minimum_required(VERSION 3.16)
project(IoTHub LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

# nlohmann/json (header-only)
find_package(nlohmann_json CONFIG REQUIRED)

# Mosquitto (C & C++). 보통 pkg-config 이름은 libmosquitto / libmosquittopp
find_package(PkgConfig REQUIRED)
pkg_check_modules(MOSQ   REQUIRED libmosquitto)
pkg_check_modules(MOSQPP REQUIRED libmosquittopp)

# --- publisher executable ---
add_executable(demo_pub
    src/mqtt/pub_main.cpp
    src/mqtt/mqttPublisher.cpp
)

target_include_directories(demo_pub
    PRIVATE
        ${CMAKE_SOURCE_DIR}/header
        ${MOSQ_INCLUDE_DIRS}
        ${MOSQPP_INCLUDE_DIRS}
)
target_link_directories(demo_pub
    PRIVATE
        ${MOSQ_LIBRARY_DIRS}
        ${MOSQPP_LIBRARY_DIRS}
)

target_link_libraries(demo_pub
    PRIVATE
        ${MOSQPP_LIBRARIES}   # mosquittopp
        ${MOSQ_LIBRARIES}     # mosquitto
        nlohmann_json::nlohmann_json
)

# --- subscriber executable ---
add_executable(demo_sub
    src/mqtt/sub_main.cpp
    src/mqtt/mqttSubscriber.cpp
)

target_include_directories(demo_sub
    PRIVATE
        ${CMAKE_SOURCE_DIR}/header
        ${MOSQ_INCLUDE_DIRS}
        ${MOSQPP_INCLUDE_DIRS}
)
target_link_directories(demo_sub
    PRIVATE
        ${MOSQ_LIBRARY_DIRS}
        ${MOSQPP_LIBRARY_DIRS}
)

target_link_libraries(demo_sub
    PRIVATE
        ${MOSQPP_LIBRARIES}
        ${MOSQ_LIBRARIES}
        nlohmann_json::nlohmann_json
)

if (CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang")
    target_compile_options(demo_pub PRIVATE -Wall -Wextra -Wpedantic)
endif()
