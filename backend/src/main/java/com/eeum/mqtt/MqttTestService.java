package com.eeum.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MqttTestService {
	  private final IMqttClient mqttClient;
	  private final MqttConnectOptions opts;

    private void ensureConnected() throws Exception {
        if (!mqttClient.isConnected()) mqttClient.connect(opts);
      }

    public void subscribe() throws Exception {
        ensureConnected();

        mqttClient.subscribe("eeum/test", (topic, msg) ->
            System.out.println("Spring Boot received: " + new String(msg.getPayload())));
    }

    public void publish() throws Exception {
        ensureConnected();

        mqttClient.publish("eeum/test",
            new MqttMessage("hello from Spring Boot".getBytes()));

    }
    
    public void publishEnvSample() throws Exception {
        ensureConnected();
        String topic = "hub/test-device/env";
        String payload = """
            {
              "ts": %d,
              "deviceId": "test-device",
              "msgId": "env-sample-001",
              "schema": "env/1.1",
              "temperature": 25.3,
              "humidity": 55.0,
              "gasDensity": 400.5,
              "units": { "temperature": "C", "humidity": "%RH", "gasDensity": "ppm" },
              "calib": { "tOffset": 0.2, "hOffset": -1.0 },
              "sampleRateHz": 1.0,
              "status": "ok",
              "meta": { "sensorModel": "DHT11" }
            }
            """.formatted(System.currentTimeMillis());

        mqttClient.publish(topic, new MqttMessage(payload.getBytes()));
        System.out.println("[MQTT] published ENV sample -> " + topic);
    }

    /** 샘플 IR 데이터 발행 */
    public void publishIrSample() throws Exception {
        ensureConnected();
        String topic = "hub/test-device/irsignal";
        String payload = """
            {
              "ts": %d,
              "deviceId": "test-device",
              "msgId": "ir-sample-001",
              "schema": "irsignal/1.0",
              "encoding": "NEC",
              "carrierHz": 38000,
              "dutyCycle": 0.33,
              "address": "0x20DF",
              "command": "0x10EF",
              "timing": { "header":[9000,4500], "one":[560,1690], "zero":[560,560], "gap":40000 },
              "rawData": [9000,4500,560,560,560,1690],
              "data": "20DF10EF",
              "repeat": 2,
              "quality": 0.97
            }
            """.formatted(System.currentTimeMillis());

        mqttClient.publish(topic, new MqttMessage(payload.getBytes()));
        System.out.println("[MQTT] published IR sample -> " + topic);
    }
    
    
}
