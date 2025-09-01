# 1) `hub/{deviceId}/irsignal`

학습/재생 모두 커버하도록 필드 정리.

| Key | Type | 설명 |
| --- | --- | --- |
| ts | int64 | 공통 |
| deviceId | string | 공통 |
| msgId | string | 공통 |
| schema | string | 공통 (예: `"irsignal/1.0"`) |
| encoding | string | `NEC`/`RC5`/`Samsung`/`Sony`/`Raw` 등 |
| carrierHz | int | 반송파(예: 38000) |
| dutyCycle | float | 0~1 (예: 0.33) |
| address | int/string | 프로토콜 주소/디바이스 코드 |
| command | int/string | 커맨드 코드 |
| timing | object | `{ header:[us,us], one:[us,us], zero:[us,us], gap:us }` |
| rawData | int[] | 마이크로초 펄스 시퀀스(+on/-off 또는 절대 us 배열) |
| data | string | 프로토콜 인코딩된 비트열(hex/base64) |
| repeat | int | 재전송 횟수(버튼 길게 누르기) |
| quality | float | 매칭 신뢰도(학습 시) |
| remark | string | 비고 |

> 추가 이유: 실기기 간 파형 오차/브랜드별 차이를 흡수하려면 carrierHz, dutyCycle, timing, repeat, quality가 필요합니다.
> 

### 예시 페이로드

```json
{
  "ts": 1756713600123,
  "deviceId": "hub-rpi-01",
  "msgId": "b3b4e9c3-2d3a-4b87-9a0a-7a57c4bda911",
  "schema": "irsignal/1.0",
  "encoding": "NEC",
  "carrierHz": 38000,
  "dutyCycle": 0.33,
  "address": "0x20DF",
  "command": "0x10EF",
  "timing": { "header":[9000,4500], "one":[560,1690], "zero":[560,560], "gap":40000 },
  "rawData": [9000,4500,560,560,560,1690, ...],
  "data": "20DF10EF",
  "repeat": 2,
  "quality": 0.97
}

```

---

# 2) `hub/{deviceId}/env`

센서 단위·교정·이상치 처리에 필요한 필드 추가.

| Key | Type | 설명 |
| --- | --- | --- |
| ts, deviceId, msgId, schema |  | 공통 |
| temperature | float | 섭씨(°C) |
| humidity | float | %RH |
| gasDensity | float | 센서 원단위(ppm 등) |
| units | object | `{ temperature:"C", humidity:"%RH", gasDensity:"ppm" }` |
| calib | object | 교정 정보 `{ tOffset:0.2, hOffset:-1.1 }` |
| sampleRateHz | float | 샘플링 주기 |
| status | string | `ok |
| meta | object | `{ sensorModel:"DHT11", tvoc:..., co2eq:... }` (선택) |

> 운영 팁: 이 채널은 retain=true로 마지막 상태를 남겨두면 대시보드 초기 로딩이 빨라집니다.
> 

### 예시

```json
{
  "ts": 1756713600456,
  "deviceId": "hub-rpi-01",
  "msgId": "8b0ef0a2-7c66-4d5b-a2e7-0a1c2b9c1e55",
  "schema": "env/1.1",
  "temperature": 26.1,
  "humidity": 54.2,
  "gasDensity": 412.0,
  "units": { "temperature": "C", "humidity": "%RH", "gasDensity": "ppm" },
  "calib": { "tOffset": 0.2 },
  "sampleRateHz": 1.0,
  "status": "ok",
  "meta": { "sensorModel": "DHT11" }
}

```

---

# 3) `hub/{deviceId}/order`

지금 스펙에 **수명/우선순위/만료/응답경로**가 빠져 있습니다. 프로덕션에선 필수입니다.

| Key | Type | 설명 |
| --- | --- | --- |
| ts, deviceId, msgId, schema, corrId |  | 공통 |
| type | string | `"ir" |
| priority | int | 0(낮음)~9(높음) |
| expiresAt | int64 | 만료시각(ms). 만료 시 드롭 |
| retry | object | `{ max:3, backoffMs:200 }` |
| replyTo | string | 응답 토픽(예: `hub/{deviceId}/order/ack`) |
| payload | object | 아래 **payload 스펙** 참조 |

### payload 스펙 예시

- **IR 송신**
    
    ```json
    "payload": {
      "ir": {
        "encoding":"NEC",
        "carrierHz":38000,
        "data":"20DF10EF",
        "repeat":2,
        "timing":{ "gap":40000 }
      }
    }
    
    ```
    
- **Matter 제어**(전구 켜기)
    
    ```json
    "payload": {
      "matter": {
        "nodeId":"0x1234",
        "endpoint":1,
        "cluster":"0x0006",
        "command":"0x01",
        "args":{}
      }
    }
    
    ```
    
- **시스템 제어**(예: 허브 재부팅)
    
    ```json
    "payload": { "system": { "action":"reboot" } }
    
    ```
    

### 전체 예시

```json
{
  "ts": 1756713600789,
  "deviceId": "controller-server",
  "msgId": "a1bc3e77-0f8f-4c3e-8b9a-2f6e3d2a7d10",
  "schema": "order/1.2",
  "corrId": "workflow-20250901-0001",
  "type": "matter",
  "priority": 5,
  "expiresAt": 1756713660000,
  "retry": { "max": 3, "backoffMs": 300 },
  "replyTo": "hub/hub-rpi-01/order/ack",
  "payload": {
    "matter": {
      "nodeId": "0x1234",
      "endpoint": 1,
      "cluster": "0x0006",
      "command": "0x01",
      "args": {}
    }
  }
}

```

---

# 4) 응답/에러 채널

## `hub/{deviceId}/order/ack`

| Key | Type | 설명 |
| --- | --- | --- |
| ts, deviceId, schema, corrId |  | 공통 |
| msgId | string | **요청 msgId** 를 그대로 회송(트레이싱) |
| status | string | `accepted |
| result | object | `{ code:0, detail:"OK" }` 혹은 디바이스 회신값 |
| durationMs | int | 수행시간 |
| retries | int | 실제 재시도 횟수 |

예시

```json
{
  "ts": 1756713601200,
  "deviceId": "hub-rpi-01",
  "schema": "ack/1.0",
  "corrId": "workflow-20250901-0001",
  "msgId": "a1bc3e77-0f8f-4c3e-8b9a-2f6e3d2a7d10",
  "status": "done",
  "result": { "code": 0, "detail": "Matter On success" },
  "durationMs": 120,
  "retries": 0
}

```

## `hub/{deviceId}/error`

운영/알림을 위해 독립 채널 권장.

```json
{
  "ts": 1756713602222,
  "deviceId": "hub-rpi-01",
  "schema": "error/1.0",
  "level": "WARN",
  "code": "IR_SEND_TIMEOUT",
  "detail": "TX GPIO no response within 80ms",
  "ctx": { "orderMsgId": "a1bc3e77-0f8f-4c3e-8b9a-2f6e3d2a7d10" }
}

```

---

# 5) 브로커 설정(LWT & 보안)

- **LWT**: 클라이언트 연결 시
    - Topic: `hub/{deviceId}/state`
    - Payload: `{"status":"offline","ts":...}`
    - QoS 1, retain true
- **연결/해제 시**: `online`/`offline` 게시
- **TLS + 인증**: mTLS 또는 토큰 기반 + `sig` 필드로 이중 보호
- **권한(ACL)**: `order`는 서버만 **publish**, 허브는 **subscribe**. `env/irsignal/ack/error/state`는 허브만 **publish**.

---

# 6) JSON 스키마(요약)

운영팀/클라이언트 검증용으로 아래처럼 최소 스키마를 두세요.

```json
{
  "$id": "https://example.com/iot/order.schema.json",
  "type": "object",
  "required": ["ts","deviceId","msgId","schema","type","payload"],
  "properties": {
    "ts": { "type": "integer" },
    "deviceId": { "type": "string" },
    "msgId": { "type": "string" },
    "schema": { "type": "string" },
    "corrId": { "type": "string" },
    "type": { "enum": ["ir","matter","system"] },
    "priority": { "type": "integer", "minimum": 0, "maximum": 9 },
    "expiresAt": { "type": "integer" },
    "retry": {
      "type": "object",
      "properties": { "max": { "type":"integer" }, "backoffMs": { "type":"integer" } }
    },
    "replyTo": { "type": "string" },
    "payload": { "type": "object" }
  }
}

```