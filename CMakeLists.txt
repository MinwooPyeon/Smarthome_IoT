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

# 프로젝트 옵션
option(BUILD_TESTS "Build tests" ON)
option(BUILD_EXAMPLES "Build examples" ON)
option(ENABLE_COVERAGE "Enable coverage reporting" OFF)
option(ENABLE_SANITIZERS "Enable sanitizers" OFF)

# Find required packages
find_package(PkgConfig REQUIRED)
find_package(Threads REQUIRED)

# Find system libraries
pkg_check_modules(CROW REQUIRED crow)
pkg_check_modules(JSONCPP REQUIRED jsoncpp)
pkg_check_modules(MOSQUITTO REQUIRED libmosquitto)

# Include directories
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/src)

# JSON 라이브러리 설정
find_package(PkgConfig REQUIRED)
pkg_check_modules(NLOHMANN_JSON REQUIRED nlohmann_json)

# Source files for library
set(LIB_SOURCES
    src/remote.cpp
    src/irsend.cpp
    src/config.cpp
    src/mqtt_client.cpp
    src/web_server.cpp
    src/pir_sensor.cpp
)

# Create library
add_library(irremote_lib STATIC ${LIB_SOURCES})

# Set library properties
set_target_properties(irremote_lib PROPERTIES
    VERSION ${PROJECT_VERSION}
    SOVERSION ${PROJECT_VERSION_MAJOR}
    PUBLIC_HEADER "${CMAKE_CURRENT_SOURCE_DIR}/include/core/remote.h;${CMAKE_CURRENT_SOURCE_DIR}/include/core/config.h;${CMAKE_CURRENT_SOURCE_DIR}/include/network/mqtt_client.h;${CMAKE_CURRENT_SOURCE_DIR}/include/network/web_server.h;${CMAKE_CURRENT_SOURCE_DIR}/include/hardware/irsend.h"
)

# Link libraries for library
target_link_libraries(irremote_lib
    ${NLOHMANN_JSON_LIBRARIES}
    ${CROW_LIBRARIES}
    ${JSONCPP_LIBRARIES}
    ${MOSQUITTO_LIBRARIES}
    Threads::Threads
    ssl
    crypto
)

# Compiler flags for library
target_compile_options(irremote_lib PRIVATE
    ${CROW_CFLAGS_OTHER}
    ${NLOHMANN_JSON_CFLAGS_OTHER}
    ${JSONCPP_CFLAGS_OTHER}
    ${MOSQUITTO_CFLAGS_OTHER}
)

# Main executable
set(MAIN_SOURCES src/main.cpp)
add_executable(irremote_server ${MAIN_SOURCES})

# Link main executable with library
target_link_libraries(irremote_server irremote_lib)

# Examples
if(BUILD_EXAMPLES)
    add_subdirectory(examples)
endif()

# Tests
if(BUILD_TESTS)
    enable_testing()
    add_subdirectory(tests)
endif()

# Coverage
if(ENABLE_COVERAGE)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} --coverage")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} --coverage")
endif()

# Sanitizers
if(ENABLE_SANITIZERS)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fsanitize=address,undefined")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -fsanitize=address,undefined")
endif()

# Install targets
install(TARGETS irremote_lib irremote_server
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

# Install documentation
install(DIRECTORY docs/
    DESTINATION share/doc/irremote
    FILES_MATCHING PATTERN "*.md"
)

# Install examples
if(BUILD_EXAMPLES)
    install(DIRECTORY examples/
        DESTINATION share/irremote/examples
        FILES_MATCHING PATTERN "*.cpp"
    )
endif()

# Export targets
install(EXPORT irremoteTargets
    FILE irremoteTargets.cmake
    NAMESPACE irremote::
    DESTINATION lib/cmake/irremote
)

# Create config file
include(CMakePackageConfigHelpers)
write_basic_package_version_file(
    irremoteConfigVersion.cmake
    VERSION ${PROJECT_VERSION}
    COMPATIBILITY AnyNewerVersion
)

install(FILES
    ${CMAKE_CURRENT_BINARY_DIR}/irremoteConfigVersion.cmake
    DESTINATION lib/cmake/irremote
)

# Create and install config file
configure_package_config_file(
    ${CMAKE_CURRENT_SOURCE_DIR}/cmake/irremoteConfig.cmake.in
    ${CMAKE_CURRENT_BINARY_DIR}/irremoteConfig.cmake
    INSTALL_DESTINATION lib/cmake/irremote
)

install(FILES
    ${CMAKE_CURRENT_BINARY_DIR}/irremoteConfig.cmake
    DESTINATION lib/cmake/irremote
)

# Custom targets
add_custom_target(run_tests
    COMMAND ${CMAKE_CTEST_COMMAND} --verbose
    DEPENDS irremote_tests
    WORKING_DIRECTORY ${CMAKE_BINARY_DIR}
)

add_custom_target(format
    COMMAND find ${CMAKE_CURRENT_SOURCE_DIR}/src ${CMAKE_CURRENT_SOURCE_DIR}/include -name "*.cpp" -o -name "*.h" | xargs clang-format -i
    COMMENT "Formatting source code"
)

add_custom_target(clean_build
    COMMAND ${CMAKE_BUILD_TOOL} clean
    COMMENT "Cleaning build directory"
)

# Print configuration summary
message(STATUS "")
message(STATUS "Configuration Summary:")
message(STATUS "  Project: ${PROJECT_NAME} ${PROJECT_VERSION}")
message(STATUS "  Build type: ${CMAKE_BUILD_TYPE}")
message(STATUS "  C++ standard: ${CMAKE_CXX_STANDARD}")
message(STATUS "  Build tests: ${BUILD_TESTS}")
message(STATUS "  Build examples: ${BUILD_EXAMPLES}")
message(STATUS "  Enable coverage: ${ENABLE_COVERAGE}")
message(STATUS "  Enable sanitizers: ${ENABLE_SANITIZERS}")
message(STATUS "")
