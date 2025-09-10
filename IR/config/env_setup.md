# 환경변수 설정 가이드

## Windows (PowerShell)
```powershell
$env:WIFI_SSID="your_wifi_ssid"
$env:WIFI_PASSWORD="your_wifi_password"
$env:MQTT_BROKER="192.168.1.100"
$env:MQTT_PORT="1883"
$env:MQTT_CLIENT_ID="esp32_ir_controller"
```

## Windows (Command Prompt)
```cmd
set WIFI_SSID=your_wifi_ssid
set WIFI_PASSWORD=your_wifi_password
set MQTT_BROKER=192.168.1.100
set MQTT_PORT=1883
set MQTT_CLIENT_ID=esp32_ir_controller
```

## Linux/macOS
```bash
export WIFI_SSID="your_wifi_ssid"
export WIFI_PASSWORD="your_wifi_password"
export MQTT_BROKER="192.168.1.100"
export MQTT_PORT="1883"
export MQTT_CLIENT_ID="esp32_ir_controller"
```

## 영구 설정 (Linux/macOS)
~/.bashrc 또는 ~/.zshrc에 추가:
```bash
export WIFI_SSID="your_wifi_ssid"
export WIFI_PASSWORD="your_wifi_password"
export MQTT_BROKER="192.168.1.100"
export MQTT_PORT="1883"
export MQTT_CLIENT_ID="esp32_ir_controller"
```

## 빌드 명령어
```bash
# 개발 빌드
pio run -e esp32dev_debug

# 릴리즈 빌드
pio run -e esp32dev_release

# 업로드
pio run -e esp32dev -t upload

# 시리얼 모니터
pio device monitor
```
