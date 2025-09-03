@echo off
echo Visual Studio 솔루션 생성을 시작합니다...

REM 기존 build 디렉토리 정리
if exist build rmdir /s /q build
mkdir build
cd build

REM Visual Studio 2022 솔루션 생성
echo Visual Studio 2022 솔루션을 생성합니다...
cmake .. -G "Visual Studio 17 2022" -A x64

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Visual Studio 솔루션 생성 완료!
    echo ========================================
    echo.
    echo 다음 단계:
    echo 1. build/rpi-ir-remote-cpp.sln 파일을 Visual Studio에서 열기
    echo 2. irremote_sender 프로젝트를 시작 프로젝트로 설정
    echo 3. F5 키로 디버깅 시작 또는 Ctrl+F5로 실행
    echo.
    echo 솔루션 파일 위치: %CD%\rpi-ir-remote-cpp.sln
    echo.
) else (
    echo.
    echo 오류가 발생했습니다. Visual Studio 2022가 설치되어 있는지 확인하세요.
    echo.
)

pause
