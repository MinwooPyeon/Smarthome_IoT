package com.eeum.config;

import java.io.IOException;
import java.io.InputStream;

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
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("[FCM] FirebaseApp reused");
            return FirebaseApp.getInstance();
        }

        GoogleCredentials credentials;
        ClassPathResource key = new ClassPathResource("firebase/service-account.json");
        if (key.exists()) {
            try (InputStream in = key.getInputStream()) {
                credentials = GoogleCredentials.fromStream(in);
                log.info("[FCM] Using service-account.json from classpath");
            }
        } else {
            credentials = GoogleCredentials.getApplicationDefault(); // ADC
            log.info("[FCM] Using Application Default Credentials (env/metadata)");
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options);
        log.info("[FCM] FirebaseApp initialized");
        return app;
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
        return FirebaseMessaging.getInstance(app);
    }
}