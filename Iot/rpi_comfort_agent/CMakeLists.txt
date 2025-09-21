cmake_minimum_required(VERSION 3.16)
project(rpi_daily_param_updater LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
add_compile_options(-O2 -Wall -Wextra)

find_package(PkgConfig REQUIRED)
pkg_check_modules(MOSQUITTO REQUIRED libmosquitto)

# nlohmann/json (header-only)
find_path(JSON_INCLUDE_DIR nlohmann/json.hpp)

include_directories(${JSON_INCLUDE_DIR})
include_directories(${MOSQUITTO_INCLUDE_DIRS} include)

add_executable(daily_param_updater
    src/main.cpp
    src/params.cpp
    src/csv_reader.cpp
    src/timeutil.cpp
    src/metrics.cpp
    src/optimizer.cpp
)
target_link_libraries(daily_param_updater
    ${MOSQUITTO_LIBRARIES}
)
