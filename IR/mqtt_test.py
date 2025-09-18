import paho.mqtt.client as mqtt
import json
import time

def on_connect(client, userdata, flags, rc):
    print(f"Connected with result code {rc}")

    if rc == 0:
        # 메시지 전송
        message = {
            "tx_id": 12345,
            "device_type": "samsung_tv",
            "raw_data": [9000, 4500, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1680],
            "function": "power_on",
            "meta_data": ["living_room", "main_tv"]
        }

        result = client.publish("hub/test-device/order/control", json.dumps(message))
        print(f"Message sent! Result: {result}")

        # 응답 구독
        client.subscribe("hub/test-device/order/response")
        client.subscribe("hub/test-device/error")
        print("Subscribed to response topics")
    else:
        print(f"Failed to connect: {rc}")

def on_message(client, userdata, msg):
    print(f"Received message on topic {msg.topic}: {msg.payload.decode()}")

def on_disconnect(client, userdata, rc):
    print(f"Disconnected with result code {rc}")

# MQTT 클라이언트 생성
client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION1)
client.username_pw_set("eeum", "ssafy2086eeum")
client.on_connect = on_connect
client.on_message = on_message
client.on_disconnect = on_disconnect

# SSL/TLS 설정 - 인증서 검증 완전 비활성화
import ssl
context = ssl.create_default_context()
context.check_hostname = False
context.verify_mode = ssl.CERT_NONE
client.tls_set_context(context)

try:
    print("Connecting to MQTT broker...")
    client.connect("43.201.62.254", 8883, 60)
    client.loop_start()

    # 메시지 전송 후 잠시 대기
    time.sleep(5)

    # 응답 대기
    print("Waiting for responses...")
    time.sleep(10)

except Exception as e:
    print(f"Error: {e}")
finally:
    client.loop_stop()
    client.disconnect()
    print("Disconnected")
