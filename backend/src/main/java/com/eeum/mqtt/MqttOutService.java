package com.eeum.mqtt;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서버 -> 허브 제어/요청 발행기
 * - hub/{deviceId}/order/ir_req
 * - hub/{deviceId}/order/control
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttOutService {
	
    private final IMqttClient client;
    private final MqttConnectOptions opts;
    private final ObjectMapper om = new ObjectMapper();

    private void ensure() throws Exception {
        if (!client.isConnected()) client.connect(opts);
    }

    // server → hub (프론트 요청 기반 IR 신호 등록 요청)
    public void publishIrReq(String deviceId, int txId, String brand, String device, String function) throws Exception {
        ensure();
        String topic = "hub/%s/order/ir_req".formatted(deviceId);
        Map<String, Object> payload = Map.of(
                "tx_id", txId,
                "brand", brand,
                "device", device,
                "function", function
        );
        byte[] bytes = om.writeValueAsBytes(payload);
        MqttMessage msg = new MqttMessage(bytes);
        msg.setQos(1);
        msg.setRetained(false);
        client.publish(topic, msg);
    }

    // server → hub/device (로깅 & AI 제어 & IR 디바이스 제어)
    public void publishControl(
            String hubDeviceId,        // 허브 대상
            UUID txUuid,               // DB 저장용 UUID
            String irDeviceId,         // IR 송신기 ID
            String deviceType,         // ex: "air_conditioner"
            List<Integer> rawData,     // IR raw data
            String function,           // 제어 함수명
            List<String> meta,         // 메타 데이터
            String model               // 모델명
    ) throws Exception {
        ensure();

        // UUID → int 변환 (IR 송신기 전송용)
        int txIdInt = txUuid.hashCode();

        String topic = "hub/%s/order/control".formatted(irDeviceId);
        Map<String, Object> payload = Map.of(
                "tx_id", txIdInt,          
                "send_device_id", irDeviceId,
                "device_type", deviceType,
                "raw_data", rawData,
                "function", function,
                "meta_data", meta
        );

        byte[] bytes = om.writeValueAsBytes(payload);
        MqttMessage msg = new MqttMessage(bytes);
        msg.setQos(1);
        msg.setRetained(false);
        client.publish(topic, msg);

        log.info("[MQTT PUBLISH] topic={} txIdInt={} irDeviceId={} function={}",
                topic, txIdInt, irDeviceId, function);
    }
    
    public void publishSendDevice(String hubDeviceId, int txId, String irDeviceId, String deviceType, boolean add) throws Exception {
        ensure();
        String topic = "hub/%s/sendDevice".formatted(hubDeviceId);

        Map<String, Object> payload = Map.of(
            "tx_id", txId,
            "deviceId", irDeviceId,          // 등록 대상 IR 송신기 ID
            "device_type", deviceType,       // 가전 타입 (air_conditioner 등)
            "consumption", 500.0f,           // 기본 소비전력
            "add_rm", add                    // true: 등록, false: 제거
        );

        byte[] bytes = om.writeValueAsBytes(payload);
        MqttMessage msg = new MqttMessage(bytes);
        msg.setQos(1);
        msg.setRetained(false);
        client.publish(topic, msg);
    }
}