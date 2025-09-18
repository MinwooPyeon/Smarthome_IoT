#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ESP32 IR Remote Controller - Error Message Test Script
에러 메시지 테스트를 위한 Python 스크립트
"""

import paho.mqtt.client as mqtt
import json
import time
import ssl

# MQTT 브로커 설정
MQTT_BROKER = "43.201.62.254"
MQTT_PORT = 8883
MQTT_USERNAME = "eeum"
MQTT_PASSWORD = "ssafy2086eeum"
MQTT_TOPIC = "hub/test-device/order/control"
MQTT_ERROR_TOPIC = "hub/test-device/error"

# SSL 인증서 검증 비활성화
ssl_context = ssl.create_default_context()
ssl_context.check_hostname = False
ssl_context.verify_mode = ssl.CERT_NONE

def on_connect(client, userdata, flags, rc):
    """MQTT 연결 콜백"""
    if rc == 0:
        print("MQTT 브로커 연결 성공!")
        # 에러 토픽 구독
        client.subscribe(MQTT_ERROR_TOPIC)
        print(f"에러 토픽 구독: {MQTT_ERROR_TOPIC}")
    else:
        print(f"MQTT 연결 실패: {rc}")

def on_message(client, userdata, msg):
    """MQTT 메시지 수신 콜백"""
    topic = msg.topic
    payload = msg.payload.decode('utf-8')

    if topic == MQTT_ERROR_TOPIC:
        print(f"에러 메시지 수신:")
        print(f"   토픽: {topic}")
        print(f"   내용: {payload}")
        try:
            error_data = json.loads(payload)
            print(f"   tx_id: {error_data.get('tx_id', 'N/A')}")
            print(f"   error: {error_data.get('error', 'N/A')}")
        except json.JSONDecodeError:
            print(f"   (JSON 파싱 실패)")
    else:
        print(f"메시지 수신: {topic} -> {payload}")

def on_disconnect(client, userdata, rc):
    """MQTT 연결 해제 콜백"""
    print(f"MQTT 연결 해제: {rc}")

def send_test_message(client, message, description):
    """테스트 메시지 전송"""
    print(f"\n테스트: {description}")
    print(f"전송 메시지: {json.dumps(message, indent=2, ensure_ascii=False)}")

    try:
        result = client.publish(MQTT_TOPIC, json.dumps(message))
        if result.rc == mqtt.MQTT_ERR_SUCCESS:
            print("메시지 전송 성공")
        else:
            print(f"메시지 전송 실패: {result.rc}")
    except Exception as e:
        print(f"전송 중 오류: {e}")

def main():
    """메인 함수"""
    print("ESP32 IR Remote Controller - 에러 메시지 테스트 시작")
    print("=" * 60)

    # MQTT 클라이언트 생성
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION1)
    client.username_pw_set(MQTT_USERNAME, MQTT_PASSWORD)
    client.tls_set_context(ssl_context)

    # 콜백 함수 설정
    client.on_connect = on_connect
    client.on_message = on_message
    client.on_disconnect = on_disconnect

    try:
        # MQTT 브로커 연결
        print(f"MQTT 브로커 연결 중... ({MQTT_BROKER}:{MQTT_PORT})")
        client.connect(MQTT_BROKER, MQTT_PORT, 60)
        client.loop_start()

        # 연결 대기
        time.sleep(2)

        # 테스트 케이스들
        test_cases = [
            {
                "message": {
                    "device_type": "samsung_tv",
                    "raw_data": [9000, 4500, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1680]
                },
                "description": "tx_id 누락 (필수 필드 누락 에러)"
            },
            {
                "message": {
                    "tx_id": 12345,
                    "raw_data": [9000, 4500, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1680]
                },
                "description": "device_type 누락 (필수 필드 누락 에러)"
            },
            {
                "message": {
                    "tx_id": 12345,
                    "device_type": "samsung_tv"
                },
                "description": "raw_data 누락 (필수 필드 누락 에러)"
            },
            {
                "message": {
                    "tx_id": 12345,
                    "device_type": "samsung_tv",
                    "raw_data": "9000,4500,560,560"
                },
                "description": "raw_data 타입 오류 (배열이 아닌 문자열)"
            },
            {
                "message": {
                    "tx_id": 12345,
                    "device_type": "samsung_tv",
                    "raw_data": []
                },
                "description": "raw_data 빈 배열 (IR 전송 실패 에러)"
            }
        ]

        # 각 테스트 케이스 실행
        for i, test_case in enumerate(test_cases, 1):
            print(f"\n{'='*60}")
            print(f"테스트 {i}/{len(test_cases)}")
            send_test_message(client, test_case["message"], test_case["description"])

            # 응답 대기
            print("응답 대기 중... (3초)")
            time.sleep(3)

        # 잘못된 JSON 형식 테스트
        print(f"\n{'='*60}")
        print("테스트 6/6")
        print("테스트: 잘못된 JSON 형식 (JSON 파싱 실패 에러)")
        print("전송 메시지: { \"tx_id\": 12345, \"device_type\": \"samsung_tv\", \"raw_data\": [9000, 4500, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1680")

        try:
            # 잘못된 JSON 전송 (닫는 괄호 누락)
            invalid_json = '{ "tx_id": 12345, "device_type": "samsung_tv", "raw_data": [9000, 4500, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1680'
            result = client.publish(MQTT_TOPIC, invalid_json)
            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                print("메시지 전송 성공")
            else:
                print(f"메시지 전송 실패: {result.rc}")
        except Exception as e:
            print(f"전송 중 오류: {e}")

        print("응답 대기 중... (3초)")
        time.sleep(3)

        print(f"\n{'='*60}")
        print("모든 테스트 완료!")
        print("에러 메시지 수신 대기 중... (10초)")
        time.sleep(10)

    except Exception as e:
        print(f"오류 발생: {e}")

    finally:
        # 연결 종료
        client.loop_stop()
        client.disconnect()
        print("MQTT 연결 종료")

if __name__ == "__main__":
    main()
