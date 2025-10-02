package com.eeum.fcm;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnMissingBean(FcmService.class) 
public class NoopFcmService implements FcmService {

    @Override
    public void send(String token, String title, String body, Map<String, String> data) {
        int dataSize = (data == null) ? 0 : data.size();
        log.info("[FCM-NOOP] send skipped (fcm.enabled != true). token=***{}, title={}, body.len={}, data.size={}",
                mask(token), title, (body == null ? 0 : body.length()), dataSize);
    }
    
    @Override
    public void sendToTopic(String topic, String title, String body, Map<String, String> data) {
        int dataSize = (data == null) ? 0 : data.size();
        log.info("[FCM-NOOP] topic send skipped (fcm.enabled != true). topic={}, title={}, body.len={}, data.size={}",
                topic, title, (body == null ? 0 : body.length()), dataSize);
    }

    private String mask(String token) {
        if (token == null || token.length() <= 8) return "***";
        return token.substring(0,4) + "..." + token.substring(token.length()-4);
    }
}