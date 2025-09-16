package com.eeum.config;

import java.nio.charset.StandardCharsets;

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

    @Value("${mqtt.clientId:controller-server}")
    private String clientId;

    @Value("${mqtt.stateTopic:hub/controller-server/state}")
    private String stateTopic;

    @Bean
    public IMqttClient mqttClient() throws Exception {
        return new MqttClient(brokerUrl, clientId);
    }

    @Bean
    public MqttConnectOptions mqttOptions() {
        MqttConnectOptions o = new MqttConnectOptions();
        o.setAutomaticReconnect(true);
        o.setConnectionTimeout(10);
        o.setKeepAliveInterval(30);
        o.setCleanSession(false);
        o.setUserName(username);
        o.setPassword(password.toCharArray());
        o.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        o.setMaxInflight(100);
        byte[] will = ("{\"status\":\"offline\"}").getBytes(StandardCharsets.UTF_8);
        o.setWill(stateTopic, will, /*qos*/1, /*retained*/true);
        return o;
    }
}
