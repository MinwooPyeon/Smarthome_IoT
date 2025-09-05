# ESP32 IR Remote 빌드 스크립트 (PowerShell)

Write-Host "=== ESP32 IR Remote 빌드 시작 ===" -ForegroundColor Green

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

# ESP32 빌드 실행
Write-Host "ESP32 빌드 중..." -ForegroundColor Yellow
idf.py build

if ($LASTEXITCODE -eq 0) {
    Write-Host "=== ESP32 IR Remote 빌드 완료 ===" -ForegroundColor Green
    Write-Host ""
    Write-Host "빌드된 파일:" -ForegroundColor Cyan
    Write-Host "  - build_esp32/esp32-ir-remote.bin" -ForegroundColor White
    Write-Host "  - build_esp32/esp32-ir-remote.elf" -ForegroundColor White
    Write-Host ""
    Write-Host "플래시 명령어:" -ForegroundColor Cyan
    Write-Host "  idf.py -p COM3 flash" -ForegroundColor White
    Write-Host ""
    Write-Host "모니터 명령어:" -ForegroundColor Cyan
    Write-Host "  idf.py -p COM3 monitor" -ForegroundColor White
    Write-Host ""
    Write-Host "플래시 + 모니터:" -ForegroundColor Cyan
    Write-Host "  idf.py -p COM3 flash monitor" -ForegroundColor White
} else {
    Write-Host "빌드 실패!" -ForegroundColor Red
    exit 1
}
