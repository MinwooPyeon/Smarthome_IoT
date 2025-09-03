# Visual Studio 솔루션 생성을 위한 PowerShell 스크립트

Write-Host "Visual Studio 솔루션 생성을 시작합니다..." -ForegroundColor Green

# 기존 build 디렉토리 정리
if (Test-Path "build") {
    Remove-Item -Recurse -Force "build"
}
New-Item -ItemType Directory -Name "build" | Out-Null
Set-Location "build"

# Visual Studio 2022 솔루션 생성
Write-Host "Visual Studio 2022 솔루션을 생성합니다..." -ForegroundColor Yellow
cmake .. -G "Visual Studio 17 2022" -A x64

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Visual Studio 솔루션 생성 완료!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "다음 단계:" -ForegroundColor Cyan
    Write-Host "1. build/rpi-ir-remote-cpp.sln 파일을 Visual Studio에서 열기" -ForegroundColor White
    Write-Host "2. irremote_sender 프로젝트를 시작 프로젝트로 설정" -ForegroundColor White
    Write-Host "3. F5 키로 디버깅 시작 또는 Ctrl+F5로 실행" -ForegroundColor White
    Write-Host ""
    Write-Host "솔루션 파일 위치: $(Get-Location)\rpi-ir-remote-cpp.sln" -ForegroundColor Yellow
    Write-Host ""
    
    # 솔루션 파일 열기 옵션 제공
    $openSolution = Read-Host "Visual Studio에서 솔루션을 열까요? (y/n)"
    if ($openSolution -eq "y" -or $openSolution -eq "Y") {
        Start-Process "rpi-ir-remote-cpp.sln"
    }
} else {
    Write-Host ""
    Write-Host "오류가 발생했습니다. Visual Studio 2022가 설치되어 있는지 확인하세요." -ForegroundColor Red
    Write-Host ""
}

Write-Host "아무 키나 누르면 종료됩니다..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
