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
        
//        mqttClient.publish("eeum/dev/rp-001/cmd",
//        	    new MqttMessage("{\"action\":\"power\",\"on\":true}".getBytes()));

    }
    
    
}
