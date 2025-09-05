# ESP32 IR Remote - 빠른 시작 가이드

## 🚀 5분 만에 시작하기

### 1. 하드웨어 연결

```
ESP32 DevKit    IR LED        IR 수신기
GPIO 4    ->    + (220Ω)     
GPIO 5    ->              -> OUT
GND       ->    GND       -> GND
3.3V      ->              -> VCC
GPIO 2    ->    내장 LED
```

### 2. 설정 파일 수정

`config/esp32_config.json` 파일을 편집:

```json
{
    "wifi": {
        "ssid": "당신의_WiFi_이름",
        "password": "당신의_WiFi_비밀번호"
    },
    "mqtt": {
        "broker": "192.168.1.100",  // MQTT 브로커 IP
        "port": 1883
    }
}
```

### 3. 빌드 및 플래시

```bash
# Windows PowerShell
.\scripts\test_esp32.ps1

# 또는 수동으로
idf.py build
idf.py -p COM3 flash monitor
```

### 4. 동작 확인

ESP32가 부팅되면:
1. WiFi 연결
2. MQTT 브로커 연결
3. IR 수신/송신 준비 완료

## 📡 MQTT 명령어

### 가전기기 제어
```json
// 토픽: irremote/control
{
    "device_id": "samsung_tv",
    "command": "POWER"
}
```

### 지원하는 명령어
- `POWER`: 전원 켜기/끄기
- `VOLUME_UP`: 볼륨 올리기
- `VOLUME_DOWN`: 볼륨 내리기
- `CHANNEL_UP`: 채널 올리기
- `CHANNEL_DOWN`: 채널 내리기
- `TEMP_UP`: 온도 올리기 (에어컨)
- `TEMP_DOWN`: 온도 내리기 (에어컨)

## 🔧 문제 해결

### WiFi 연결 안됨
- SSID/비밀번호 확인
- 2.4GHz 대역 사용 (5GHz 지원 안함)

### MQTT 연결 안됨
- 브로커 IP 주소 확인
- 방화벽 설정 확인
- 포트 1883 열려있는지 확인

### IR 신호 안됨
- GPIO 핀 연결 확인
- IR LED 극성 확인
- 저항값 확인 (220Ω)

## 📊 모니터링

### 시리얼 모니터
```bash
idf.py -p COM3 monitor
```

### MQTT 토픽
- `irremote/received`: IR 코드 수신 알림
- `irremote/response`: 제어 결과
- `esp32/status`: ESP32 상태

## 🎯 테스트 방법

1. **IR 수신 테스트**: 리모컨으로 IR 신호 전송
2. **IR 송신 테스트**: MQTT 명령어로 가전기기 제어
3. **연결 테스트**: WiFi/MQTT 연결 상태 확인

## 📝 로그 확인

ESP32 시리얼 모니터에서 다음 로그 확인:
```
I (1234) IR_REMOTE_MAIN: WiFi 연결 성공!
I (2345) IR_REMOTE_MAIN: MQTT 연결 성공!
I (3456) IR_REMOTE_MAIN: ESP32 IR 송신기 초기화 완료
```

## 🔄 업데이트

새로운 IR 코드 추가:
1. `config/esp32_config.json`에서 IR 코드 추가
2. `src/irsend.cpp`의 `control_to_ir` 맵에 매핑 추가
3. 재빌드 및 플래시

## 📞 지원

문제가 발생하면:
1. 시리얼 모니터 로그 확인
2. 하드웨어 연결 재확인
3. 설정 파일 재확인
4. GitHub Issues에 문의
