
# IoT MQTT Topic Specification

본 문서는 SARS 프로젝트의 IoT 환경에서 사용되는 MQTT 토픽별 명세를 정의합니다.  
각 토픽별 **용도, 발신·수신 주체, QoS/Retain, ACL, JSON Schema, 예시 Payload**를 제공합니다.

---

## 목차
1. [iot/{site}/{hubId}/state](#1-iotsitehubidstate)
2. [iot/{site}/{hubId}/env](#2-iotsitehubidenv)
3. [iot/{site}/{hubId}/irsignal/raw](#3-iotsitehubidirsignalraw)
4. [iot/{site}/{hubId}/order](#4-iotsitehubidorder)
5. [iot/{site}/{hubId}/order/ack](#5-iotsitehubidorderack)
6. [iot/{site}/{hubId}/error](#6-iotsitehubiderror)
7. [iot/{site}/{irTxId}/tx](#7-iotsiteirtxidtx)
8. [iot/{site}/{irTxId}/tx/ack](#8-iotsiteirtxidtxack)
9. [iot/{site}/{pirId}/event](#9-iotsitepiridevent)
10. [iot/{site}/matlab/{nodeId}/analysis/env](#10-iotsitematlabnodeidanalysisenv)
11. [iot/{site}/matlab/{nodeId}/analysis/irsignal](#11-iotsitematlabnodeidanalysisirsignal)

---

# 1) `iot/{site}/{hubId}/state`
**용도**: 허브 상태 보고 및 LWT  
**발신**: Hub │ **수신**: Server, MFC  
**QoS/Retain**: QoS1, retain=true  
**ACL**:  
- Publish: `iot/+/{hubId}/state` (허브)  
- Subscribe: `iot/+/+/state` (서버, MFC)

**예시 Payload**
```json
{"ts":1756713600123,"deviceId":"hub-rpi-01","msgId":"...","schema":"state/1.0","status":"online"}
```

---

# 2) `iot/{site}/{hubId}/env`
**용도**: 환경 센서 데이터(온도/습도/가스 등)  
**발신**: Hub │ **수신**: Server, MATLAB, MFC  
**QoS/Retain**: QoS1, retain=true  
**ACL**:  
- Publish: `iot/+/{hubId}/env` (허브)  
- Subscribe: `iot/+/+/env` (서버, MATLAB, MFC)

**예시 Payload**
```json
{"ts":1756713600456,"deviceId":"hub-rpi-01","msgId":"...","schema":"env/1.1","temperature":26.1,"humidity":54.2,"gasDensity":412}
```

---

# 3) `iot/{site}/{hubId}/irsignal/raw`
**용도**: IR 원신호 및 메타 데이터 송신  
**발신**: Hub │ **수신**: Server, MATLAB  
**QoS/Retain**: QoS1, retain=false  
**ACL**:  
- Publish: `iot/+/{hubId}/irsignal/raw`  
- Subscribe: `iot/+/+/irsignal/raw`

**예시 Payload**
```json
{"ts":1756713600780,"deviceId":"hub-rpi-01","msgId":"...","schema":"irsignal/1.0","encoding":"NEC","carrierHz":38000,"rawData":[9000,4500,560,560,...]}
```

---

# 4) `iot/{site}/{hubId}/order`
**용도**: 허브에 명령 전달(IR, Matter, System)  
**발신**: Server │ **수신**: Hub  
**QoS/Retain**: QoS1, retain=false  
**ACL**:  
- Publish: `iot/+/{hubId}/order` (서버)  
- Subscribe: `iot/+/{hubId}/order` (허브)

**예시 Payload**
```json
{"ts":1756713600789,"deviceId":"server-app","msgId":"...","schema":"order/1.2","type":"matter","replyTo":"iot/lab1/hub-rpi-01/order/ack","payload":{"matter":{"nodeId":"0x1234","endpoint":1,"cluster":"0x0006","command":"0x01","args":{}}}}
```

---

# 5) `iot/{site}/{hubId}/order/ack`
**용도**: 허브 명령 처리 상태 보고  
**발신**: Hub │ **수신**: Server, MFC  
**QoS/Retain**: QoS1, retain=false  
**ACL**:  
- Publish: `iot/+/{hubId}/order/ack`  
- Subscribe: `iot/+/+/order/ack`

**예시 Payload**
```json
{"ts":1756713601200,"deviceId":"hub-rpi-01","msgId":"<orderMsgId>","schema":"ack/1.0","status":"done","result":{"code":0,"detail":"OK"}}
```

---

# 6) `iot/{site}/{hubId}/error`
**용도**: 허브 에러 리포트  
**발신**: Hub │ **수신**: Server, MFC  
**QoS/Retain**: QoS1, retain=false  
**ACL**:  
- Publish: `iot/+/{hubId}/error`  
- Subscribe: `iot/+/+/error`

**예시 Payload**
```json
{"ts":1756713602222,"deviceId":"hub-rpi-01","msgId":"...","schema":"error/1.0","level":"WARN","code":"IR_SEND_TIMEOUT","detail":"TX GPIO no response"}
```

---

# 7) `iot/{site}/{irTxId}/tx`
**용도**: IR 송신 명령(서버 → IR 디바이스)  
**발신**: Server │ **수신**: IR-TX  
**QoS/Retain**: QoS1, retain=false  
**ACL**:  
- Publish: `iot/+/{irTxId}/tx`  
- Subscribe: `iot/+/{irTxId}/tx`

**예시 Payload**
```json
{"ts":1756713601800,"deviceId":"server-app","msgId":"...","schema":"irtx/1.0","encoding":"NEC","carrierHz":38000,"data":"20DF10EF","repeat":2}
```

---

# 8) `iot/{site}/{irTxId}/tx/ack`
**용도**: IR 송신 결과 보고  
**발신**: IR-TX │ **수신**: Server, Hub  
**QoS/Retain**: QoS1, retain=false  
**ACL**:  
- Publish: `iot/+/{irTxId}/tx/ack`  
- Subscribe: `iot/+/+/tx/ack`

**예시 Payload**
```json
{"ts":1756713601850,"deviceId":"ir-esp32-01","msgId":"<orderMsgId>","schema":"ack/1.0","status":"done","result":{"code":0}}
```

---

# 9) `iot/{site}/{pirId}/event`
**용도**: PIR 인체감지 이벤트 보고  
**발신**: PIR │ **수신**: Hub, Server  
**QoS/Retain**: QoS1, retain=false  
**ACL**:  
- Publish: `iot/+/{pirId}/event`  
- Subscribe: `iot/+/+/event`

**예시 Payload**
```json
{"ts":1756713602600,"deviceId":"pir-arduino-01","msgId":"...","schema":"pir/1.0","motion":true,"confidence":0.92}
```

---

# 10) `iot/{site}/matlab/{nodeId}/analysis/env`
**용도**: MATLAB 환경 이상치 분석 결과  
**발신**: MATLAB │ **수신**: Server, MFC  
**QoS/Retain**: QoS1, retain=false  
**ACL**:  
- Publish: `iot/+/{matlabId}/analysis/env`  
- Subscribe: `iot/+/matlab/+/analysis/env`

**예시 Payload**
```json
{"ts":1756713603000,"deviceId":"matlab-01","msgId":"...","schema":"analysis.env/1.0","target":"hub-rpi-01","anomalyScore":0.81,"method":"EWMA","window":120}
```

---

# 11) `iot/{site}/matlab/{nodeId}/analysis/irsignal`
**용도**: MATLAB IR 신호 분석 결과  
**발신**: MATLAB │ **수신**: Server, MFC  
**QoS/Retain**: QoS1, retain=false  
**ACL**:  
- Publish: `iot/+/{matlabId}/analysis/irsignal`  
- Subscribe: `iot/+/matlab/+/analysis/irsignal`

**예시 Payload**
```json
{"ts":1756713603400,"deviceId":"matlab-01","msgId":"...","schema":"analysis.irsignal/1.0","protocol":"NEC","carrierHz":38000,"confidence":0.96}
```
