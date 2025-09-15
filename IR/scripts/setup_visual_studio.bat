@echo off
echo Setting up Visual Studio project for IR Remote Controller...

REM CMake 빌드 디렉토리 생성
if not exist "build" mkdir build
cd build

REM Visual Studio 솔루션 생성
echo Generating Visual Studio solution...
cmake -G "Visual Studio 17 2022" -A x64 ..

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ Visual Studio solution generated successfully!
    echo.
    echo 📁 Solution file location: build\IRRemoteController.sln
    echo.
    echo 🚀 You can now:
    echo    1. Open build\IRRemoteController.sln in Visual Studio
    echo    2. Set 'ir_remote_controller' as startup project
    echo    3. Press F5 to debug
    echo.
    echo 🔧 Available projects:
    echo    - ir_remote_controller (main application)
    echo    - unit_tests (unit tests)
    echo    - integration_tests (integration tests)
    echo.
) else (
    echo.
    echo ❌ Failed to generate Visual Studio solution
    echo Please check if CMake and Visual Studio are properly installed
    echo.
)

cd ..
pause
