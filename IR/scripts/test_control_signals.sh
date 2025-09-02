#!/bin/bash

# 제어신호-IR신호 매핑 테스트 스크립트
# MQTT 브로커를 통해 제어신호를 전송하여 테스트

MQTT_BROKER="localhost"
MQTT_PORT="1883"
DEVICE_ID="hub-rpi-01"

echo "=== 제어신호-IR신호 매핑 테스트 ==="
echo "MQTT 브로커: $MQTT_BROKER:$MQTT_PORT"
echo "디바이스 ID: $DEVICE_ID"
echo ""

# 1. 단일 제어신호 테스트 (에어컨 전원)
echo "1. 단일 제어신호 테스트 (에어컨 전원)"
mosquitto_pub -h $MQTT_BROKER -p $MQTT_PORT -t "hub/$DEVICE_ID/order" -m '{
  "msgId": "test-001",
  "corrId": "test-001",
  "type": "control",
  "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'",
  "ttl": 30000,
  "payload": {
    "control": {
      "signal": "에어컨_전원"
    }
  }
}'

sleep 2

# 2. 순차 제어신호 테스트 (에어컨 설정)
echo "2. 순차 제어신호 테스트 (에어컨 설정)"
mosquitto_pub -h $MQTT_BROKER -p $MQTT_PORT -t "hub/$DEVICE_ID/order" -m '{
  "msgId": "test-002",
  "corrId": "test-002",
  "type": "control",
  "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'",
  "ttl": 30000,
  "payload": {
    "control": {
      "signals": ["에어컨_전원", "에어컨_냉방모드", "에어컨_온도_26도"],
      "delay_ms": 200
    }
  }
}'

sleep 2

# 3. TV 제어 테스트
echo "3. TV 제어 테스트 (전원 + HDMI1 소스)"
mosquitto_pub -h $MQTT_BROKER -p $MQTT_PORT -t "hub/$DEVICE_ID/order" -m '{
  "msgId": "test-003",
  "corrId": "test-003",
  "type": "control",
  "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'",
  "ttl": 30000,
  "payload": {
    "control": {
      "signals": ["TV_전원", "TV_소스_HDMI1"],
      "delay_ms": 1000
    }
  }
}'

sleep 2

# 4. 복잡한 시나리오 테스트 (홈시네마 모드)
echo "4. 복잡한 시나리오 테스트 (홈시네마 모드)"
mosquitto_pub -h $MQTT_BROKER -p $MQTT_PORT -t "hub/$DEVICE_ID/order" -m '{
  "msgId": "test-004",
  "corrId": "test-004",
  "type": "control",
  "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'",
  "ttl": 30000,
  "payload": {
    "control": {
      "signals": [
        "프로젝터_전원",
        "프로젝터_소스_HDMI",
        "TV_전원",
        "TV_소스_HDMI1"
      ],
      "delay_ms": 500
    }
  }
}'

sleep 2

# 5. 공기청정기 제어 테스트
echo "5. 공기청정기 제어 테스트 (전원 + 자동 모드)"
mosquitto_pub -h $MQTT_BROKER -p $MQTT_PORT -t "hub/$DEVICE_ID/order" -m '{
  "msgId": "test-005",
  "corrId": "test-005",
  "type": "control",
  "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)'",
  "ttl": 30000,
  "payload": {
    "control": {
      "signals": ["공기청정기_전원", "공기청정기_자동모드"],
      "delay_ms": 300
    }
  }
}'

echo ""
echo "=== 테스트 완료 ==="
echo "MQTT 메시지가 전송되었습니다."
echo "제어신호가 IR 신호로 변환되어 전송되는지 확인하세요."
echo ""
echo "사용된 제어신호들:"
echo "- 에어컨_전원, 에어컨_냉방모드, 에어컨_온도_26도"
echo "- TV_전원, TV_소스_HDMI1"
echo "- 프로젝터_전원, 프로젝터_소스_HDMI"
echo "- 공기청정기_전원, 공기청정기_자동모드"
