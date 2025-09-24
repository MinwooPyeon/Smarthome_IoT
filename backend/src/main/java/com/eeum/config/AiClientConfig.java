package com.eeum.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiClientConfig {

    @Bean(name = "openAiWebClient")
    public WebClient openAiWebClient(
        @Value("${openai.base-url}") String base,
        @Value("${openai.api-key}") String key,
        @Value("${openai.host-header:}") String hostHeader
    ) {
        WebClient.Builder b = WebClient.builder()
            .baseUrl(base)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (!hostHeader.isBlank()) {
            b.defaultHeader(HttpHeaders.HOST, hostHeader);
        }
        return b.build();
    }
}
