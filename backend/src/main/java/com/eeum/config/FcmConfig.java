package com.eeum.config;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "fcm.enabled", havingValue = "true")
public class FcmConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // 이미 초기화되어 있으면 재사용
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("[FCM] FirebaseApp reused");
            return FirebaseApp.getInstance();
        }

    
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options);
        log.info("[FCM] FirebaseApp initialized via ADC");
        return app;
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
        return FirebaseMessaging.getInstance(app);
    }
}
