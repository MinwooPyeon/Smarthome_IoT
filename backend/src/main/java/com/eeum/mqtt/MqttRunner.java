package com.eeum.mqtt;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MqttRunner implements CommandLineRunner {
    private final MqttTestService mqttTestService;

    @Override
    public void run(String... args) throws Exception {
        mqttTestService.subscribe();
        Thread.sleep(1000);
        mqttTestService.publish();
    }
}

