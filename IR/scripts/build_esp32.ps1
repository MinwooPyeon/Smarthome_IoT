# ESP32 IR Remote Controller Build Script

param(
    [string]$Target = "build",
    [switch]$Clean = $false,
    [switch]$Flash = $false,
    [switch]$Monitor = $false,
    [string]$Port = "COM3"
)

Write-Host "ESP32 IR Remote Controller Build Script" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# ESP-IDF 환경 확인
$idfPath = $env:IDF_PATH
if (-not $idfPath) {
    Write-Host "Error: IDF_PATH environment variable is not set" -ForegroundColor Red
    Write-Host "Please run the ESP-IDF setup script first:" -ForegroundColor Yellow
    Write-Host "  . `$IDF_PATH/export.sh" -ForegroundColor Yellow
    exit 1
}

Write-Host "ESP-IDF Path: $idfPath" -ForegroundColor Cyan

# 프로젝트 디렉토리로 이동
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDir = Split-Path -Parent $projectDir
Set-Location $projectDir

Write-Host "Project Directory: $projectDir" -ForegroundColor Cyan

# Clean 빌드
if ($Clean) {
    Write-Host "Cleaning build directory..." -ForegroundColor Yellow
    if (Test-Path "build") {
        Remove-Item -Recurse -Force "build"
    }
}

# 빌드 명령어 구성
$buildCmd = "idf.py"

if ($Target -eq "build") {
    $buildCmd += " build"
} elseif ($Target -eq "menuconfig") {
    $buildCmd += " menuconfig"
} elseif ($Target -eq "size") {
    $buildCmd += " size"
} elseif ($Target -eq "size-components") {
    $buildCmd += " size-components"
} elseif ($Target -eq "size-files") {
    $buildCmd += " size-files"
}

if ($Flash) {
    $buildCmd += " flash"
    if ($Port) {
        $buildCmd += " -p $Port"
    }
}

if ($Monitor) {
    $buildCmd += " monitor"
    if ($Port) {
        $buildCmd += " -p $Port"
    }
}

Write-Host "Executing: $buildCmd" -ForegroundColor Cyan
Write-Host ""

# 명령어 실행
try {
    Invoke-Expression $buildCmd
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "Build completed successfully!" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "Build failed with exit code: $LASTEXITCODE" -ForegroundColor Red
        exit $LASTEXITCODE
    }
} catch {
    Write-Host "Error executing build command: $_" -ForegroundColor Red
    exit 1
}