# ESP32 IR Remote 테스트 스크립트

Write-Host "=== ESP32 IR Remote 테스트 ===" -ForegroundColor Green

# ESP-IDF 환경 확인
if (-not $env:IDF_PATH) {
    Write-Host "오류: ESP-IDF 환경이 설정되지 않았습니다." -ForegroundColor Red
    Write-Host "다음 명령어로 ESP-IDF를 설정하세요:" -ForegroundColor Yellow
    Write-Host "  . `$env:USERPROFILE\esp\esp-idf\export.ps1" -ForegroundColor Yellow
    exit 1
}

Write-Host "ESP-IDF 경로: $env:IDF_PATH" -ForegroundColor Cyan

# 프로젝트 디렉토리로 이동
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectPath = Split-Path -Parent $scriptPath
Set-Location $projectPath

# ESP32 빌드 디렉토리 생성
if (-not (Test-Path "build_esp32")) {
    New-Item -ItemType Directory -Path "build_esp32" | Out-Null
}

Set-Location "build_esp32"

# ESP32 CMakeLists.txt 복사
Copy-Item "../CMakeLists_ESP32_main.txt" "./CMakeLists.txt" -Force

Write-Host "ESP32 설정 확인 중..." -ForegroundColor Yellow

# 설정 파일 확인
if (-not (Test-Path "../config/esp32_config.json")) {
    Write-Host "오류: ESP32 설정 파일이 없습니다." -ForegroundColor Red
    Write-Host "config/esp32_config.json 파일을 생성하세요." -ForegroundColor Yellow
    exit 1
}

Write-Host "설정 파일 확인 완료" -ForegroundColor Green

# WiFi 설정 확인
$configContent = Get-Content "../config/esp32_config.json" -Raw
if ($configContent -match '"ssid": "your_wifi_ssid"') {
    Write-Host "경고: WiFi 설정이 기본값입니다." -ForegroundColor Yellow
    Write-Host "config/esp32_config.json에서 WiFi SSID와 비밀번호를 설정하세요." -ForegroundColor Yellow
}

# MQTT 브로커 설정 확인
if ($configContent -match '"broker": "192.168.1.100"') {
    Write-Host "경고: MQTT 브로커 설정이 기본값입니다." -ForegroundColor Yellow
    Write-Host "config/esp32_config.json에서 MQTT 브로커 IP를 설정하세요." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== 하드웨어 연결 확인 ===" -ForegroundColor Cyan
Write-Host "ESP32 GPIO 연결:" -ForegroundColor White
Write-Host "  GPIO 4  -> IR LED (220Ω 저항)" -ForegroundColor White
Write-Host "  GPIO 5  -> IR 수신기 OUT" -ForegroundColor White
Write-Host "  GPIO 2  -> 상태 LED (내장 LED)" -ForegroundColor White
Write-Host "  GND     -> IR LED/수신기 GND" -ForegroundColor White
Write-Host "  3.3V    -> IR 수신기 VCC" -ForegroundColor White

Write-Host ""
Write-Host "=== 빌드 테스트 ===" -ForegroundColor Cyan

# 빌드 테스트
Write-Host "ESP32 빌드 테스트 중..." -ForegroundColor Yellow
idf.py build

if ($LASTEXITCODE -eq 0) {
    Write-Host "=== 빌드 성공! ===" -ForegroundColor Green
    Write-Host ""
    Write-Host "다음 단계:" -ForegroundColor Cyan
    Write-Host "1. ESP32를 USB로 연결" -ForegroundColor White
    Write-Host "2. COM 포트 확인 (예: COM3)" -ForegroundColor White
    Write-Host "3. 플래시 명령어 실행:" -ForegroundColor White
    Write-Host "   idf.py -p COM3 flash" -ForegroundColor Yellow
    Write-Host "4. 모니터 명령어 실행:" -ForegroundColor White
    Write-Host "   idf.py -p COM3 monitor" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "또는 한 번에 실행:" -ForegroundColor White
    Write-Host "   idf.py -p COM3 flash monitor" -ForegroundColor Yellow
} else {
    Write-Host "빌드 실패!" -ForegroundColor Red
    Write-Host "오류를 확인하고 수정하세요." -ForegroundColor Yellow
    exit 1
}
