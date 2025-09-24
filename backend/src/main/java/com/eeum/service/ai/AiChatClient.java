package com.eeum.service.ai;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiChatClient {

    private final WebClient openAiWebClient;

    @Value("${OPENAI_MODEL}")
    private String model;

    /**
     * system + user 프롬프트로 단일 응답 텍스트 생성
     */
    public String chat(String systemPrompt, String userPrompt) {
        ChatRequest req = new ChatRequest();
        req.setModel(model);
        req.setTemperature(0.2);
        req.setMessages(List.of(
            new ChatMessage("system", systemPrompt),
            new ChatMessage("user", userPrompt)
        ));

        ChatResponse res = openAiWebClient
            .post()
            .uri("/v1/chat/completions")
            .bodyValue(req)
            .retrieve()
            .bodyToMono(ChatResponse.class)
            .block();

        if (res == null || res.choices == null || res.choices.isEmpty()) {
            throw new IllegalStateException("AI 응답이 비어 있습니다.");
        }
        String content = res.choices.get(0).message.content;
        if (content == null) content = "";
        return content.trim();
    }

    // ===== DTOs =====
    @Data
    static class ChatRequest {
        private String model;
        private Double temperature;
        private List<ChatMessage> messages;
        @JsonProperty("response_format")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Map<String, Object> responseFormat; // 필요 시 JSON 구조 강제 등 사용
    }

    @Data
    static class ChatMessage {
        private String role;
        private String content;

        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Data
    static class ChatResponse {
        public String id;
        public String object;
        public Long created;
        public List<Choice> choices;

        @Data
        public static class Choice {
            public int index;
            public Message message;
            @JsonProperty("finish_reason")
            public String finishReason;
        }

        @Data
        public static class Message {
            public String role;
            public String content;
        }
    }
}
