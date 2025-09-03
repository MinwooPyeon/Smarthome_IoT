#!/bin/bash

# Test execution script for IR Receiver System
# This script runs unit tests and integration tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BUILD_DIR="build"
TEST_RESULTS_DIR="test_results"
COVERAGE_DIR="coverage"

echo -e "${BLUE}=== IR Receiver System Test Runner ===${NC}"

# Check if build directory exists
if [ ! -d "$BUILD_DIR" ]; then
    echo -e "${YELLOW}Build directory not found. Creating build directory...${NC}"
    mkdir -p "$BUILD_DIR"
    cd "$BUILD_DIR"
    cmake ..
    cd ..
fi

# Create test results directory
mkdir -p "$TEST_RESULTS_DIR"
mkdir -p "$COVERAGE_DIR"

echo -e "${BLUE}Building tests...${NC}"
cd "$BUILD_DIR"
make -j$(nproc)

echo -e "${BLUE}Running unit tests...${NC}"
if make run_unit_tests; then
    echo -e "${GREEN}✓ Unit tests passed!${NC}"
else
    echo -e "${RED}✗ Unit tests failed!${NC}"
    exit 1
fi

echo -e "${BLUE}Running integration tests...${NC}"
if make run_integration_tests; then
    echo -e "${GREEN}✓ Integration tests passed!${NC}"
else
    echo -e "${RED}✗ Integration tests failed!${NC}"
    exit 1
fi

echo -e "${BLUE}Running all tests with CTest...${NC}"
if ctest --output-on-failure --verbose; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
else
    echo -e "${RED}✗ Some tests failed!${NC}"
    exit 1
fi

# Generate coverage report if enabled
if [ "$ENABLE_COVERAGE" = "true" ]; then
    echo -e "${BLUE}Generating coverage report...${NC}"
    
    if command -v lcov &> /dev/null; then
        lcov --capture --directory . --output-file coverage.info
        lcov --remove coverage.info '/usr/*' --output-file coverage.info
        lcov --remove coverage.info '*/tests/*' --output-file coverage.info
        genhtml coverage.info --output-directory "$COVERAGE_DIR"
        echo -e "${GREEN}✓ Coverage report generated in $COVERAGE_DIR${NC}"
    else
        echo -e "${YELLOW}lcov not found. Skipping coverage report.${NC}"
    fi
fi

# Run memory check if enabled
if [ "$ENABLE_MEMCHECK" = "true" ]; then
    echo -e "${BLUE}Running memory check...${NC}"
    
    if command -v valgrind &> /dev/null; then
        ctest -T memcheck
        echo -e "${GREEN}✓ Memory check completed!${NC}"
    else
        echo -e "${YELLOW}valgrind not found. Skipping memory check.${NC}"
    fi
fi

echo -e "${GREEN}=== All tests completed successfully! ===${NC}"

# Show test summary
echo -e "${BLUE}Test Results Summary:${NC}"
echo -e "  - Unit Tests: ${GREEN}PASSED${NC}"
echo -e "  - Integration Tests: ${GREEN}PASSED${NC}"
echo -e "  - Test Results: $TEST_RESULTS_DIR"
if [ "$ENABLE_COVERAGE" = "true" ]; then
    echo -e "  - Coverage Report: $COVERAGE_DIR"
fi

cd ..
