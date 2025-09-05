# ESP32 IR Remote Controller

ESP32를 사용한 IR 리모컨 제어 시스템입니다.

## 🚀 주요 기능

- **IR 신호 수신/송신**: ESP32 GPIO를 통한 IR 신호 처리
- **WiFi 연결**: ESP32 내장 WiFi 모듈 사용
- **MQTT 통신**: 실시간 제어 명령 수신 및 상태 전송
- **가전기기 제어**: TV, 에어컨, 공기청정기, 프로젝터 제어
- **Matter 지원**: Matter 프로토콜을 통한 스마트홈 연동

## 📋 하드웨어 요구사항

### ESP32 보드
- ESP32 DevKit V1 또는 호환 보드
- WiFi 내장
- GPIO 핀 4개 이상

### IR 센서/LED
- IR 수신기 (TSOP4838 등)
- IR LED (940nm)
- 저항 (220Ω, 10kΩ)

### 연결 방법
```
ESP32    IR 수신기    IR LED
GPIO 5   -> OUT
GPIO 4   -> + (220Ω 저항)
GND      -> GND       -> GND
3.3V     -> VCC       -> + (220Ω 저항)
```

## 🛠️ 소프트웨어 요구사항

### ESP-IDF
- ESP-IDF v4.4 이상
- Python 3.8 이상
- CMake 3.16 이상

### 라이브러리
- IRremoteESP8266 (ESP32용 IR 라이브러리)
- PubSubClient (MQTT 클라이언트)
- ArduinoJson (JSON 처리)

## 📦 설치 및 설정

### 1. ESP-IDF 설치
```bash
# ESP-IDF 설치 (Linux/macOS)
git clone --recursive https://github.com/espressif/esp-idf.git
cd esp-idf
./install.sh
. ./export.sh

# ESP-IDF 설치 (Windows)
# ESP-IDF 설치 프로그램 다운로드 및 실행
# https://dl.espressif.com/dl/esp-idf/
```

### 2. 프로젝트 클론
```bash
git clone <repository-url>
cd IR
```

### 3. 설정 파일 수정
`config/esp32_config.json` 파일을 편집하여 WiFi 및 MQTT 설정을 변경하세요:

```json
{
    "wifi": {
        "ssid": "your_wifi_ssid",
        "password": "your_wifi_password"
    },
    "mqtt": {
        "broker": "192.168.1.100",
        "port": 1883
    }
}
```

### 4. 빌드 및 플래시
```bash
# Linux/macOS
./scripts/build_esp32.sh

# Windows
.\scripts\build_esp32.ps1

# 수동 빌드
idf.py build
idf.py -p /dev/ttyUSB0 flash monitor
```

## 🔧 설정

### GPIO 핀 설정
```json
{
    "gpio": {
        "ir_tx_pin": 4,    // IR 송신 핀
        "ir_rx_pin": 5,    // IR 수신 핀
        "led_status_pin": 2, // 상태 LED 핀
        "button_pin": 0    // 버튼 핀
    }
}
```

### IR 코드 설정
```json
{
    "appliances": {
        "samsung_tv": {
            "gpio_pin": 4,
            "ir_codes": {
                "power": "0xE0E040BF",
                "volume_up": "0xE0E0E01F",
                "volume_down": "0xE0E0D02F"
            }
        }
    }
}
```

## 📡 MQTT 토픽

### 수신 토픽
- `irremote/control`: 제어 명령 수신
  ```json
  {
      "device_id": "samsung_tv",
      "command": "1",
      "timestamp": 1234567890
  }
  ```

### 송신 토픽
- `irremote/received`: IR 코드 수신 알림
- `irremote/status`: 디바이스 상태
- `esp32/status`: ESP32 연결 상태

## 🔄 동작 방식

1. **초기화**: WiFi 연결 및 MQTT 브로커 연결
2. **IR 수신**: IR 센서로부터 신호 수신
3. **코드 변환**: IR 코드를 가전기기 명령으로 변환
4. **제어 실행**: 해당 가전기기 제어
5. **상태 전송**: MQTT로 제어 결과 전송

## 🐛 문제 해결

### WiFi 연결 실패
- SSID/비밀번호 확인
- 신호 강도 확인
- 방화벽 설정 확인

### MQTT 연결 실패
- 브로커 IP 주소 확인
- 포트 번호 확인 (기본: 1883)
- 네트워크 연결 확인

### IR 신호 수신 안됨
- GPIO 핀 연결 확인
- IR 센서 동작 확인
- 리모컨 배터리 확인

## 📊 성능 최적화

### 메모리 사용량
- FreeRTOS 태스크 스택 크기 조정
- JSON 버퍼 크기 최적화
- IR 코드 캐시 사용

### 전력 소비
- WiFi 절전 모드 사용
- 불필요한 로그 출력 제거
- 딥 슬립 모드 구현

## 🔒 보안

### WiFi 보안
- WPA2/WPA3 암호화 사용
- 강력한 비밀번호 설정

### MQTT 보안
- TLS/SSL 암호화 사용
- 인증서 기반 인증
- 토픽 접근 권한 제어

## 📈 확장 가능성

### 추가 가전기기
- 새로운 IR 코드 추가
- 가전기기 타입 확장
- 제어 명령 추가

### 스마트홈 연동
- Matter 프로토콜 지원
- Home Assistant 연동
- Google Home/Alexa 연동

## 📝 라이선스

MIT License

## 🤝 기여

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 지원

문제가 발생하면 GitHub Issues를 통해 문의해주세요.
