cmake_minimum_required(VERSION 3.16)
project(rpi-ir-remote-cpp VERSION 1.0.0 LANGUAGES CXX)

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

# Include directories
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/src)

# JSON 라이브러리 설정 (nlohmann/json.hpp 사용)
set(NLOHMANN_JSON_INCLUDE_DIRS ${CMAKE_CURRENT_SOURCE_DIR}/external)

# Library sources
set(LIB_SOURCES
    src/config.cpp
    src/mqtt_client.cpp
    src/mqtt_message.cpp
    src/matter_client.cpp
    src/ir_receiver.cpp
    src/appliance_controller.cpp
)

# Create library
add_library(irremote_lib STATIC ${LIB_SOURCES})

# Set library properties
set_target_properties(irremote_lib PROPERTIES
    VERSION ${PROJECT_VERSION}
    SOVERSION ${PROJECT_VERSION_MAJOR}
    PUBLIC_HEADER "${CMAKE_CURRENT_SOURCE_DIR}/include/core/config.h;${CMAKE_CURRENT_SOURCE_DIR}/include/network/mqtt_client.h;${CMAKE_CURRENT_SOURCE_DIR}/include/network/matter_client.h;${CMAKE_CURRENT_SOURCE_DIR}/include/hardware/ir_receiver.h;${CMAKE_CURRENT_SOURCE_DIR}/include/hardware/appliance_controller.h"
)

# Link libraries for library
target_link_libraries(irremote_lib
    Threads::Threads
)

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
install(DIRECTORY include/
    DESTINATION include/irremote
    FILES_MATCHING PATTERN "*.h"
)

# Install configuration files
install(FILES 
    config/systemd/irremote.service 
    DESTINATION /etc/systemd/system
)

# Install scripts
install(DIRECTORY scripts/
    DESTINATION bin/irremote
    USE_SOURCE_PERMISSIONS
)

# Export targets
install(EXPORT irremoteTargets
    FILE irremoteTargets.cmake
    DESTINATION lib/cmake/irremote
)

# Add tests (commented out for Windows build)
# add_subdirectory(tests)

# Print configuration summary
message(STATUS "")
message(STATUS "Configuration Summary:")
message(STATUS "  Project: ${PROJECT_NAME} ${PROJECT_VERSION}")
message(STATUS "  Build type: ${CMAKE_BUILD_TYPE}")
message(STATUS "  C++ standard: ${CMAKE_CXX_STANDARD}")
message(STATUS "")
