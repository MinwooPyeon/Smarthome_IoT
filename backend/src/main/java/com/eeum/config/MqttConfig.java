package com.eeum.config;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttConfig {

  @Value("${mqtt.broker}")   private String brokerUrl;
  @Value("${mqtt.username}") private String username;
  @Value("${mqtt.password}") private String password;

  @Bean
  public IMqttClient mqttClient() throws Exception {
    String clientId = "spring-" + System.currentTimeMillis();
    return new MqttClient(brokerUrl, clientId);
  }

  @Bean
  public MqttConnectOptions mqttOptions() {
    MqttConnectOptions o = new MqttConnectOptions();
    o.setAutomaticReconnect(true);
    o.setCleanSession(true);
    o.setUserName(username);
    o.setPassword(password.toCharArray());
    o.setConnectionTimeout(10);
    o.setKeepAliveInterval(30);
    return o;
  }
}
