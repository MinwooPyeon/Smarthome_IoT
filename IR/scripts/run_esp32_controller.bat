@echo off
chcp 65001 > nul
echo ESP32 IR Remote Controller
echo ========================

REM Python이 설치되어 있는지 확인
python --version > nul 2>&1
if errorlevel 1 (
    echo Python이 설치되어 있지 않습니다.
    echo Python 3.7 이상을 설치해주세요.
    pause
    exit /b 1
)

REM 필요한 패키지 설치
echo 필요한 패키지를 설치합니다...
pip install pyserial

REM ESP32 컨트롤러 실행
echo ESP32 컨트롤러를 시작합니다...
python scripts\esp32_controller.py

pause
