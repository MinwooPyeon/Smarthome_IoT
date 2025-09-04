# Test configuration
enable_testing()

# Find GoogleTest (optional)
find_package(GTest QUIET)
if(NOT GTest_FOUND)
    message(WARNING "GoogleTest not found. Tests will be disabled.")
    return()
endif()
find_package(Threads REQUIRED)

# Test source files
set(UNIT_TEST_SOURCES
    unit/test_ir_receiver.cpp
    unit/test_appliance_controller.cpp
    unit/test_mqtt_client.cpp
)

set(INTEGRATION_TEST_SOURCES
    integration/test_system_integration.cpp
    integration/test_hardware_simulation.cpp
)

# Unit tests
add_executable(unit_tests ${UNIT_TEST_SOURCES})
target_link_libraries(unit_tests
    irremote_lib
    GTest::gtest
    GTest::gtest_main
    GTest::gmock
    Threads::Threads
    ${NLOHMANN_JSON_LIBRARIES}
)

# Integration tests
add_executable(integration_tests ${INTEGRATION_TEST_SOURCES})
target_link_libraries(integration_tests
    irremote_lib
    GTest::gtest
    GTest::gtest_main
    GTest::gmock
    Threads::Threads
    ${NLOHMANN_JSON_LIBRARIES}
)

# Set include directories for tests
target_include_directories(unit_tests PRIVATE
    ${CMAKE_SOURCE_DIR}/include
    ${NLOHMANN_JSON_INCLUDE_DIRS}
)

target_include_directories(integration_tests PRIVATE
    ${CMAKE_SOURCE_DIR}/include
    ${NLOHMANN_JSON_INCLUDE_DIRS}
)

# Add tests
add_test(NAME UnitTests COMMAND unit_tests)
add_test(NAME IntegrationTests COMMAND integration_tests)

# Test properties
set_tests_properties(UnitTests PROPERTIES
    TIMEOUT 300
    LABELS "unit"
)

set_tests_properties(IntegrationTests PROPERTIES
    TIMEOUT 600
    LABELS "integration"
)

# Test output configuration
set_tests_properties(UnitTests IntegrationTests PROPERTIES
    ENVIRONMENT "GTEST_COLOR=1"
    ENVIRONMENT "GTEST_OUTPUT=xml:test_results.xml"
)

# Coverage configuration (if enabled)
option(ENABLE_COVERAGE "Enable code coverage" OFF)
if(ENABLE_COVERAGE)
    if(CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang")
        target_compile_options(unit_tests PRIVATE --coverage)
        target_compile_options(integration_tests PRIVATE --coverage)
        target_link_options(unit_tests PRIVATE --coverage)
        target_link_options(integration_tests PRIVATE --coverage)
    endif()
endif()

# Memory check configuration
option(ENABLE_MEMCHECK "Enable memory checking" OFF)
if(ENABLE_MEMCHECK)
    find_program(VALGRIND "valgrind")
    if(VALGRIND)
        set_tests_properties(UnitTests IntegrationTests PROPERTIES
            MEMORYCHECK_COMMAND ${VALGRIND}
            MEMORYCHECK_COMMAND_OPTIONS "--tool=memcheck --leak-check=full --show-leak-kinds=all"
        )
    endif()
endif()

# Test discovery
include(GoogleTest)
gtest_discover_tests(UnitTests)
gtest_discover_tests(IntegrationTests)

# Custom test targets
add_custom_target(run_unit_tests
    COMMAND ${CMAKE_CTEST_COMMAND} --output-on-failure -C $<CONFIG> -R UnitTests
    DEPENDS unit_tests
    COMMENT "Running unit tests..."
)

add_custom_target(run_integration_tests
    COMMAND ${CMAKE_CTEST_COMMAND} --output-on-failure -C $<CONFIG> -R IntegrationTests
    DEPENDS integration_tests
    COMMENT "Running integration tests..."
)

add_custom_target(run_all_tests
    COMMAND ${CMAKE_CTEST_COMMAND} --output-on-failure -C $<CONFIG>
    DEPENDS unit_tests integration_tests
    COMMENT "Running all tests..."
)

# Test data setup
add_custom_target(setup_test_data
    COMMAND ${CMAKE_COMMAND} -E copy_directory
        ${CMAKE_SOURCE_DIR}/config
        ${CMAKE_BINARY_DIR}/tests/config
    COMMENT "Setting up test data..."
)

# Make tests depend on test data setup
add_dependencies(unit_tests setup_test_data)
add_dependencies(integration_tests setup_test_data)
