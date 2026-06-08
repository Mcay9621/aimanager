package com.example.aimanager.service.strategy;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface AiProviderStrategy {

    String chat(String endpoint, String apiKey, String modelName, String message);

    void chatStream(String endpoint, String apiKey, String modelName,
                    List<Map<String, String>> messages, SseEmitter emitter,
                    StreamCallback callback);

    String parseResponse(String responseBody);

    interface StreamCallback {
        void onToken(String token);
        default void onDone(String fullContent) {}
        default void onError(String error) {}
    }
}
