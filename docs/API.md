# IR Remote Control API 문서

## 개요

Raspberry Pi IR Remote Control 시스템의 API 문서입니다. 이 시스템은 웹 API와 MQTT를 통해 IR 리모컨을 제어할 수 있게 해줍니다.

## 웹 API

### 기본 정보

- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **인증**: API 토큰 (선택사항)

### 인증

API 토큰이 활성화된 경우, 모든 요청에 다음 헤더를 포함해야 합니다:

```
Authorization: Bearer <your_api_token>
```

### 엔드포인트

#### 1. 리모컨 목록 조회

**GET** `/api/remotes`

사용 가능한 모든 리모컨의 목록을 반환합니다.

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "remotes": [
      {
        "name": "cambridge_cxa",
        "code_count": 30,
        "description": "Cambridge Audio CXA60 Amplifier"
      }
    ]
  }
}
```

#### 2. 리모컨 정보 조회

**GET** `/api/remotes/{remote_name}`

특정 리모컨의 상세 정보를 반환합니다.

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "name": "cambridge_cxa",
    "codes": [
      {
        "name": "KEY_POWER",
        "code": "KEY_POWER",
        "description": "전원 버튼"
      },
      {
        "name": "KEY_VOLUMEUP",
        "code": "KEY_VOLUMEUP",
        "description": "볼륨 증가"
      }
    ]
  }
}
```

#### 3. IR 명령 전송

**POST** `/api/remotes/{remote_name}/send`

IR 명령을 전송합니다.

**요청 본문:**
```json
{
  "command": "KEY_POWER",
  "retry_count": 3,
  "delay_ms": 100
}
```

**응답 예시:**
```json
{
  "success": true,
  "message": "Command sent successfully",
  "data": {
    "remote": "cambridge_cxa",
    "command": "KEY_POWER",
    "execution_time_ms": 45.2
  }
}
```

#### 4. 코드 검색

**GET** `/api/remotes/{remote_name}/search?pattern={pattern}`

리모컨에서 특정 패턴과 일치하는 코드를 검색합니다.

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "pattern": "VOLUME",
    "results": [
      {
        "name": "KEY_VOLUMEUP",
        "code": "KEY_VOLUMEUP",
        "description": "볼륨 증가"
      },
      {
        "name": "KEY_VOLUMEDOWN",
        "code": "KEY_VOLUMEDOWN",
        "description": "볼륨 감소"
      }
    ]
  }
}
```

#### 5. 시스템 상태 조회

**GET** `/api/status`

시스템의 현재 상태를 반환합니다.

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "version": "1.0.0",
    "uptime": 3600,
    "remote_count": 2,
    "mqtt_connected": true,
    "ir_device_available": true,
    "total_commands_sent": 150,
    "successful_commands": 148,
    "failed_commands": 2
  }
}
```

#### 6. 설정 조회

**GET** `/api/config`

현재 시스템 설정을 반환합니다.

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "web_server": {
      "port": 8080,
      "host": "0.0.0.0",
      "enabled": true
    },
    "mqtt": {
      "broker": "localhost",
      "port": 1883,
      "enabled": true,
      "client_id": "irremote_client"
    },
    "ir": {
      "device": "/dev/lirc0",
      "timeout_ms": 5000,
      "retry_count": 3
    }
  }
}
```

#### 7. 설정 업데이트

**PUT** `/api/config`

시스템 설정을 업데이트합니다.

**요청 본문:**
```json
{
  "web_server": {
    "port": 8080
  },
  "mqtt": {
    "enabled": true,
    "broker": "mqtt.example.com"
  }
}
```

#### 8. 헬스 체크

**GET** `/api/health`

서비스의 건강 상태를 확인합니다.

**응답 예시:**
```json
{
  "status": "healthy",
  "timestamp": "2024-01-01T12:00:00Z",
  "version": "1.0.0"
}
```

### 오류 응답

모든 API는 오류 발생 시 다음과 같은 형식으로 응답합니다:

```json
{
  "success": false,
  "error": "Error message",
  "error_code": "ERROR_CODE"
}
```

**일반적인 오류 코드:**
- `INVALID_REMOTE`: 존재하지 않는 리모컨
- `INVALID_COMMAND`: 존재하지 않는 명령
- `DEVICE_ERROR`: IR 장치 오류
- `PERMISSION_DENIED`: 권한 없음
- `VALIDATION_ERROR`: 입력 데이터 검증 실패

## MQTT API

### 브로커 설정

- **기본 브로커**: `localhost:1883`
- **클라이언트 ID**: `irremote_client`
- **토픽 접두사**: `irremote`

### 토픽

#### 1. 명령 수신

**토픽**: `irremote/commands`

IR 명령을 수신하는 토픽입니다.

**메시지 형식:**
```json
{
  "remote_id": "cambridge_cxa",
  "command": "KEY_POWER",
  "retry_count": 3,
  "delay_ms": 100
}
```

#### 2. 상태 발행

**토픽**: `irremote/status`

시스템 상태를 발행하는 토픽입니다.

**메시지 형식:**
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": "online",
  "remote_count": 2,
  "total_commands": 150,
  "successful_commands": 148
}
```

#### 3. 명령 결과

**토픽**: `irremote/results`

명령 실행 결과를 발행하는 토픽입니다.

**메시지 형식:**
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "remote_id": "cambridge_cxa",
  "command": "KEY_POWER",
  "success": true,
  "execution_time_ms": 45.2,
  "message": "Command sent successfully"
}
```

### QoS 레벨

- **명령 수신**: QoS 1 (최소 한 번 전달)
- **상태 발행**: QoS 0 (최대 한 번 전달)
- **결과 발행**: QoS 1 (최소 한 번 전달)

## 예제

### cURL을 사용한 명령 전송

```bash
# 리모컨 목록 조회
curl -X GET http://localhost:8080/api/remotes

# 명령 전송
curl -X POST http://localhost:8080/api/remotes/cambridge_cxa/send \
  -H "Content-Type: application/json" \
  -d '{"command": "KEY_POWER"}'

# 토큰이 필요한 경우
curl -X POST http://localhost:8080/api/remotes/cambridge_cxa/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_token_here" \
  -d '{"command": "KEY_POWER"}'
```

### Python을 사용한 명령 전송

```python
import requests
import json

# 리모컨 목록 조회
response = requests.get('http://localhost:8080/api/remotes')
remotes = response.json()

# 명령 전송
command_data = {
    "command": "KEY_POWER",
    "retry_count": 3
}

response = requests.post(
    'http://localhost:8080/api/remotes/cambridge_cxa/send',
    json=command_data
)

result = response.json()
print(f"Command result: {result}")
```

### Node.js를 사용한 MQTT 명령 전송

```javascript
const mqtt = require('mqtt');

const client = mqtt.connect('mqtt://localhost:1883');

client.on('connect', () => {
    console.log('Connected to MQTT broker');
    
    // 명령 전송
    const command = {
        remote_id: 'cambridge_cxa',
        command: 'KEY_POWER',
        retry_count: 3
    };
    
    client.publish('irremote/commands', JSON.stringify(command));
});

client.on('message', (topic, message) => {
    if (topic === 'irremote/results') {
        const result = JSON.parse(message.toString());
        console.log('Command result:', result);
    }
});

client.subscribe('irremote/results');
```

## 제한사항

1. **동시 명령**: 한 번에 하나의 IR 명령만 처리됩니다.
2. **명령 지연**: 연속된 명령 사이에는 최소 100ms 지연이 있습니다.
3. **재시도**: 기본적으로 최대 3회 재시도합니다.
4. **타임아웃**: IR 명령 실행은 5초 후 타임아웃됩니다.

## 버전 관리

API 버전은 URL 경로에 포함됩니다:

- 현재 버전: `/api/v1/`
- 향후 버전: `/api/v2/`

## 지원

문제가 발생하거나 질문이 있으시면 다음을 참조하세요:

- [설치 가이드](DEPLOYMENT.md)
- [문제 해결](TROUBLESHOOTING.md)
- [GitHub Issues](https://github.com/your-repo/issues)
