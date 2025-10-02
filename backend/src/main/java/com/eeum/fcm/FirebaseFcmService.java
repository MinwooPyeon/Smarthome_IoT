package com.eeum.fcm;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "fcm.enabled", havingValue = "true")
public class FirebaseFcmService implements FcmService {

    private final FirebaseMessaging messaging;

    @Override
    public void send(String token, String title, String body, Map<String, String> data) throws Exception {
        Notification notif = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message.Builder mb = Message.builder()
                .setToken(token)
                .setNotification(notif);

        if (data != null && !data.isEmpty()) {
            mb.putAllData(data);
        }

        String messageId = messaging.send(mb.build());
        log.info("[FCM] sent ok: id={}, token=***{} (masked)", 
                 messageId, mask(token));
    }
    
    @Override
    public void sendToTopic(String topic, String title, String body, Map<String, String> data) throws Exception {
        Notification notif = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message.Builder mb = Message.builder()
                .setTopic(topic)
                .setNotification(notif);

        if (data != null && !data.isEmpty()) {
            mb.putAllData(data);
        }

        String messageId = messaging.send(mb.build());
        log.info("[FCM] topic sent ok: id={}, topic={}", messageId, topic);
    }
    

    private String mask(String token) {
        if (token == null || token.length() <= 8) return "***";
        return token.substring(0,4) + "..." + token.substring(token.length()-4);
    }
}
