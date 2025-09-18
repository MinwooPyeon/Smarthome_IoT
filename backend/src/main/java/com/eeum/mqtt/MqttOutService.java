package com.eeum.mqtt;

import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * 서버 -> 허브 제어/요청 발행기
 * - hub/{deviceId}/order/ir_req
 * - hub/{deviceId}/order/control
 */
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
    public void publishControl(String deviceId, int txId, String deviceType,
                               List<Integer> rawData, String function, List<String> meta) throws Exception {
        ensure();
        String topic = "hub/%s/order/control".formatted(deviceId);
        Map<String, Object> payload = Map.of(
                "tx_id", txId,
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
    }
}