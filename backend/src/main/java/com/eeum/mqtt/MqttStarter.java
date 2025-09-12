package com.eeum.mqtt;

import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MqttStarter {

  private final IMqttClient client;
  private final MqttConnectOptions opts;

  @EventListener(ApplicationReadyEvent.class)
  public void connectWhenReady() throws Exception {
    if (!client.isConnected()) {
      client.connect(opts);
      System.out.println("MQTT Connected to " + client.getServerURI());
    }
  }
}
