package com.eeum.fcm;

import java.util.Map;

public interface FcmService {
    void send(String token, String title, String body, Map<String, String> data) throws Exception;
    
    void sendToTopic(String topic, String title, String body, Map<String, String> data) throws Exception;
}