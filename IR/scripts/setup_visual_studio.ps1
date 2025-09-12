# Visual Studio 프로젝트 설정 스크립트
Write-Host "🔧 Setting up Visual Studio project for IR Remote Controller..." -ForegroundColor Cyan

# CMake 빌드 디렉토리 생성
if (-not (Test-Path "build")) {
    New-Item -ItemType Directory -Path "build" | Out-Null
    Write-Host "📁 Created build directory" -ForegroundColor Green
}

Set-Location "build"

# Visual Studio 솔루션 생성
Write-Host "🔨 Generating Visual Studio solution..." -ForegroundColor Yellow
$cmakeResult = cmake -G "Visual Studio 17 2022" -A x64 ..

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Visual Studio solution generated successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📁 Solution file location: build\IRRemoteController.sln" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "🚀 You can now:" -ForegroundColor Yellow
    Write-Host "   1. Open build\IRRemoteController.sln in Visual Studio" -ForegroundColor White
    Write-Host "   2. Set 'ir_remote_controller' as startup project" -ForegroundColor White
    Write-Host "   3. Press F5 to debug" -ForegroundColor White
    Write-Host ""
    Write-Host "🔧 Available projects:" -ForegroundColor Yellow
    Write-Host "   - ir_remote_controller (main application)" -ForegroundColor White
    Write-Host "   - unit_tests (unit tests)" -ForegroundColor White
    Write-Host "   - integration_tests (integration tests)" -ForegroundColor White
    Write-Host ""

    # 솔루션 파일이 생성되었는지 확인
    if (Test-Path "IRRemoteController.sln") {
        Write-Host "🎯 Solution file found! Opening in Visual Studio..." -ForegroundColor Green
        Start-Process "IRRemoteController.sln"
    }
}
else {
    Write-Host ""
    Write-Host "❌ Failed to generate Visual Studio solution" -ForegroundColor Red
    Write-Host "Please check if CMake and Visual Studio are properly installed" -ForegroundColor Red
    Write-Host ""
}

Set-Location ".."

# 환경변수 설정 안내
Write-Host "🔐 Environment Variables Setup:" -ForegroundColor Yellow
Write-Host "Before debugging, set these environment variables:" -ForegroundColor White
Write-Host ""
Write-Host '$env:WIFI_SSID="your_wifi_ssid"' -ForegroundColor Cyan
Write-Host '$env:WIFI_PASSWORD="your_wifi_password"' -ForegroundColor Cyan
Write-Host '$env:MQTT_BROKER="192.168.1.100"' -ForegroundColor Cyan
Write-Host '$env:MQTT_PORT="1883"' -ForegroundColor Cyan
Write-Host ""

Read-Host "Press Enter to continue"
