@echo off
REM Test execution script for IR Receiver System (Windows)
REM This script runs unit tests and integration tests

setlocal enabledelayedexpansion

REM Configuration
set BUILD_DIR=build
set TEST_RESULTS_DIR=test_results
set COVERAGE_DIR=coverage

echo === IR Receiver System Test Runner (Windows) ===

REM Check if build directory exists
if not exist "%BUILD_DIR%" (
    echo Build directory not found. Creating build directory...
    mkdir "%BUILD_DIR%"
    cd "%BUILD_DIR%"
    cmake ..
    cd ..
)

REM Create test results directory
if not exist "%TEST_RESULTS_DIR%" mkdir "%TEST_RESULTS_DIR%"
if not exist "%COVERAGE_DIR%" mkdir "%COVERAGE_DIR%"

echo Building tests...
cd "%BUILD_DIR%"

REM Build tests
cmake --build . --config Release --target unit_tests
if errorlevel 1 (
    echo Unit tests build failed!
    exit /b 1
)

cmake --build . --config Release --target integration_tests
if errorlevel 1 (
    echo Integration tests build failed!
    exit /b 1
)

echo Running unit tests...
ctest -C Release -R UnitTests --output-on-failure
if errorlevel 1 (
    echo Unit tests failed!
    exit /b 1
)

echo Running integration tests...
ctest -C Release -R IntegrationTests --output-on-failure
if errorlevel 1 (
    echo Integration tests failed!
    exit /b 1
)

echo Running all tests with CTest...
ctest -C Release --output-on-failure --verbose
if errorlevel 1 (
    echo Some tests failed!
    exit /b 1
)

REM Generate coverage report if enabled
if "%ENABLE_COVERAGE%"=="true" (
    echo Generating coverage report...
    REM Windows coverage tools can be added here
    echo Coverage report generation not implemented for Windows yet.
)

REM Run memory check if enabled
if "%ENABLE_MEMCHECK%"=="true" (
    echo Running memory check...
    REM Windows memory check tools can be added here
    echo Memory check not implemented for Windows yet.
)

echo === All tests completed successfully! ===

REM Show test summary
echo Test Results Summary:
echo   - Unit Tests: PASSED
echo   - Integration Tests: PASSED
echo   - Test Results: %TEST_RESULTS_DIR%
if "%ENABLE_COVERAGE%"=="true" (
    echo   - Coverage Report: %COVERAGE_DIR%
)

cd ..
pause
