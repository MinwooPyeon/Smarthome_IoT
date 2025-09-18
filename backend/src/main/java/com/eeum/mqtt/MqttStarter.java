package com.eeum.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttStarter {

    private final IMqttClient client;
    private final MqttConnectOptions opts;
    private final MqttIoService ioService;
    
    // 서버가 구독할 토픽
    private static final String ENV_TOPIC       = "hub/+/env";
    private static final String IR_TOPIC 	    = "hub/+/irSignal";
    private static final String IRPROTO_TOPIC   = "hub/+/irProtocol";
    private static final String ERROR_TOPIC     = "hub/+/error";
    private static final String REQUEST_TOPIC   = "hub/+/request";
    
    @Value("${mqtt.stateTopic:hub/controller-server/state}")
    private String stateTopic; 
    
    // Spring Boot 애플리케이션이 완전히 구동된 뒤 실행
    @EventListener(ApplicationReadyEvent.class)
    public void connectAndSubscribe() throws Exception {
        ensureConnected();

        client.setCallback(new MqttCallbackExtended() {
            
        	// MQTT 연결이 완료되거나 재연결되었을 때 호출
        	@Override
            public void connectComplete(boolean reconnect, String serverURI) {
                try {
                	// 재구독
                	publishOnlineState();
                    subscribeAll(); 
                    log.info("[MQTT] connected (reconnect={}) -> {}", reconnect, serverURI);
                } catch (Exception e) {
                    log.error("[MQTT] resubscribe failed", e);
                }
            }
        	
        	// 브로커와 연결이 끊어졌을 때 호출
            @Override
            public void connectionLost(Throwable cause) {
                log.warn("[MQTT] connection lost: {}", (cause != null ? cause.getMessage() : "unknown"));
            }
            
            // 구독한 토픽으로 메시지가 들어왔을 때 호출
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // 처리 서비스로 원본 바이트 전달 (파싱/검증/중복제거는 서비스에서)
                ioService.onMessage(topic, message.getPayload());

                // 간단 로그 (원하시면 유지)
                 String payload = new String(message.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
                 if (topic.endsWith("/env")) {
                     log.info("[ENV ] {}", payload);
                 } else if (topic.endsWith("/irSignal")) { 
                     log.info("[IR  ] {}", payload);
                 } else if (topic.endsWith("/irProtocol")) { 
                     log.info("[IRP ] {}", payload);
                 } else if (topic.endsWith("/error")) { 
                     log.info("[ERR ] {}", payload);
                 } else if (topic.endsWith("/request")) { 
                     log.info("[REQ ] {}", payload);
                 } else {
                     log.info("[MQTT] {} -> {}", topic, payload);
                 }
            }

            // publish한 메시지가 브로커에 정상적으로 전달 완료되었을 때 호출
            @Override
            public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
                log.info("[MQTT] deliveryComplete: {}", token.getMessageId());
            }
        });
        
        // 최초 구독 실행
        subscribeAll();
        publishOnlineState();
    }
    
    // 브로커와 연결이 안 되어 있으면 실행.
    private void ensureConnected() throws Exception {
        if (!client.isConnected()) {
            client.connect(opts);
        }
    }
    
    // 서버가 구독할 토픽들을 등록
    private void subscribeAll() throws Exception {
        ensureConnected();
        
        // 서버 수준 1 - 적어도 한번 전달
        client.subscribe(ENV_TOPIC, 1);
        client.subscribe(IR_TOPIC, 1);        
        client.subscribe(IRPROTO_TOPIC, 1);   
        client.subscribe(ERROR_TOPIC, 1);     
        client.subscribe(REQUEST_TOPIC, 1);  
        log.info("[MQTT] subscribed: {}, {}, {}, {}, {}",
            ENV_TOPIC, IR_TOPIC, IRPROTO_TOPIC, ERROR_TOPIC, REQUEST_TOPIC);
    }
    
    // online 상태를 Retain으로 게시
    private void publishOnlineState() throws Exception {
        ensureConnected();
        long ts = System.currentTimeMillis();
        String json = "{\"status\":\"online\",\"ts\":"+ts+"}";
        MqttMessage m = new MqttMessage(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        m.setQos(1);
        m.setRetained(true);
        client.publish(stateTopic, m);
        log.info("[STATE] online published (retain) -> {} : {}", stateTopic, json);
    }
}
