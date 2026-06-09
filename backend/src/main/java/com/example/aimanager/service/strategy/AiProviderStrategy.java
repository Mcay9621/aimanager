package com.example.aimanager.service.strategy;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface AiProviderStrategy {

    String chat(String endpoint, String apiKey, String modelName, String message);

    void chatStream(String endpoint, String apiKey, String modelName,
                    List<Map<String, String>> messages, SseEmitter emitter,
                    StreamCallback callback);

    String parseResponse(String responseBody);

    default Map<String, Integer> parseUsage(String responseBody) {
        return Map.of("prompt_tokens", 0, "completion_tokens", 0, "total_tokens", 0);
    }

    interface StreamCallback {
        void onToken(String token);
        default void onDone(String fullContent) {}
        default void onDone(String fullContent, Map<String, Integer> usage) { onDone(fullContent); }
        default void onError(String error) {}
    }
}
