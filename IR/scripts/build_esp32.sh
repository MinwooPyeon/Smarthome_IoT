#!/bin/bash

# ESP32 IR Remote 빌드 스크립트

set -e

echo "=== ESP32 IR Remote 빌드 시작 ==="

# ESP-IDF 환경 확인
if [ -z "$IDF_PATH" ]; then
    echo "오류: ESP-IDF 환경이 설정되지 않았습니다."
    echo "다음 명령어로 ESP-IDF를 설정하세요:"
    echo "  . $HOME/esp/esp-idf/export.sh"
    exit 1
fi

echo "ESP-IDF 경로: $IDF_PATH"

# 프로젝트 디렉토리로 이동
cd "$(dirname "$0")/.."

# ESP32 빌드 디렉토리 생성
if [ ! -d "build_esp32" ]; then
    mkdir build_esp32
fi

cd build_esp32

# ESP32 CMakeLists.txt 복사
cp ../CMakeLists_ESP32_main.txt ./CMakeLists.txt

# ESP32 빌드 실행
echo "ESP32 빌드 중..."
idf.py build

echo "=== ESP32 IR Remote 빌드 완료 ==="
echo ""
echo "빌드된 파일:"
echo "  - build_esp32/esp32-ir-remote.bin"
echo "  - build_esp32/esp32-ir-remote.elf"
echo ""
echo "플래시 명령어:"
echo "  idf.py -p /dev/ttyUSB0 flash"
echo ""
echo "모니터 명령어:"
echo "  idf.py -p /dev/ttyUSB0 monitor"
echo ""
echo "플래시 + 모니터:"
echo "  idf.py -p /dev/ttyUSB0 flash monitor"
