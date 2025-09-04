# Windows용 IR Remote 서비스 관리 스크립트
# 관리자 권한으로 실행해야 합니다

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("install", "uninstall", "start", "stop", "status")]
    [string]$Action
)

$ServiceName = "IRRemoteService"
$ServiceDisplayName = "IR Remote Control Service"
$ServiceDescription = "IR 수신기 기반 가전기기 제어 시스템"
$ExePath = Join-Path $PSScriptRoot "..\..\build\irremote_sender.exe"

function Install-Service {
    Write-Host "IR Remote 서비스를 설치합니다..." -ForegroundColor Green
    
    try {
        $service = New-Service -Name $ServiceName `
                              -DisplayName $ServiceDisplayName `
                              -Description $ServiceDescription `
                              -BinaryPathName $ExePath `
                              -StartupType Automatic
        
        Write-Host "서비스가 성공적으로 설치되었습니다." -ForegroundColor Green
        Write-Host "서비스 이름: $ServiceName" -ForegroundColor Yellow
        Write-Host "실행 파일: $ExePath" -ForegroundColor Yellow
    }
    catch {
        Write-Error "서비스 설치 실패: $_"
        exit 1
    }
}

function Uninstall-Service {
    Write-Host "IR Remote 서비스를 제거합니다..." -ForegroundColor Yellow
    
    try {
        if (Get-Service -Name $ServiceName -ErrorAction SilentlyContinue) {
            Stop-Service -Name $ServiceName -Force -ErrorAction SilentlyContinue
            Remove-Service -Name $ServiceName
            Write-Host "서비스가 성공적으로 제거되었습니다." -ForegroundColor Green
        } else {
            Write-Host "서비스가 설치되어 있지 않습니다." -ForegroundColor Yellow
        }
    }
    catch {
        Write-Error "서비스 제거 실패: $_"
        exit 1
    }
}

function Start-Service {
    Write-Host "IR Remote 서비스를 시작합니다..." -ForegroundColor Green
    
    try {
        Start-Service -Name $ServiceName
        Write-Host "서비스가 시작되었습니다." -ForegroundColor Green
    }
    catch {
        Write-Error "서비스 시작 실패: $_"
        exit 1
    }
}

function Stop-Service {
    Write-Host "IR Remote 서비스를 중지합니다..." -ForegroundColor Yellow
    
    try {
        Stop-Service -Name $ServiceName -Force
        Write-Host "서비스가 중지되었습니다." -ForegroundColor Green
    }
    catch {
        Write-Error "서비스 중지 실패: $_"
        exit 1
    }
}

function Get-ServiceStatus {
    try {
        $service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
        if ($service) {
            Write-Host "서비스 상태:" -ForegroundColor Cyan
            Write-Host "  이름: $($service.Name)" -ForegroundColor White
            Write-Host "  표시명: $($service.DisplayName)" -ForegroundColor White
            Write-Host "  상태: $($service.Status)" -ForegroundColor White
            Write-Host "  시작 유형: $($service.StartType)" -ForegroundColor White
        } else {
            Write-Host "서비스가 설치되어 있지 않습니다." -ForegroundColor Red
        }
    }
    catch {
        Write-Error "서비스 상태 확인 실패: $_"
        exit 1
    }
}

# 메인 실행 로직
switch ($Action) {
    "install" { Install-Service }
    "uninstall" { Uninstall-Service }
    "start" { Start-Service }
    "stop" { Stop-Service }
    "status" { Get-ServiceStatus }
    default {
        Write-Host "사용법: .\irremote_service.ps1 [install|uninstall|start|stop|status]" -ForegroundColor Cyan
        Write-Host "예시:" -ForegroundColor Cyan
        Write-Host "  .\irremote_service.ps1 install" -ForegroundColor White
        Write-Host "  .\irremote_service.ps1 status" -ForegroundColor White
    }
}
