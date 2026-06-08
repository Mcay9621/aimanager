package com.example.aimanager.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AnthropicStrategy implements AiProviderStrategy {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AnthropicStrategy(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(String endpoint, String apiKey, String modelName, String message) {
        try {
            String url = endpoint + "/v1/messages";
            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName);
            body.put("max_tokens", 4096);
            body.put("messages", List.of(Map.of("role", "user", "content", message)));

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseResponse(response.body());
            }
            throw new RuntimeException("API 请求失败: " + response.statusCode() + " - " + response.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("AI 调用失败: " + e.getMessage());
        }
    }

    @Override
    public void chatStream(String endpoint, String apiKey, String modelName,
                           List<Map<String, String>> messages, SseEmitter emitter,
                           StreamCallback callback) {
        String url = endpoint + "/v1/messages";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("stream", true);
        requestBody.put("max_tokens", 4096);
        requestBody.put("model", modelName);
        requestBody.put("messages", messages);

        CompletableFuture.runAsync(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(120))
                        .header("Content-Type", "application/json")
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    String errorBody = new String(response.body().readAllBytes());
                    callback.onError("API请求失败: " + response.statusCode() + " - " + errorBody);
                    emitter.send(SseEmitter.event().name("error").data("API请求失败: " + response.statusCode()));
                    emitter.complete();
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    StringBuilder contentBuilder = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) break;

                            try {
                                JsonNode jsonNode = objectMapper.readTree(data);
                                if (jsonNode.has("type")
                                        && "content_block_delta".equals(jsonNode.get("type").asText())) {
                                    JsonNode delta = jsonNode.path("delta");
                                    if (delta.has("text")) {
                                        String token = delta.get("text").asText();
                                        contentBuilder.append(token);
                                        callback.onToken(token);
                                        emitter.send(SseEmitter.event().name("token").data(token));
                                    }
                                }
                                if (jsonNode.has("type")
                                        && "message_stop".equals(jsonNode.get("type").asText())) {
                                    break;
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    callback.onDone(contentBuilder.toString());
                    emitter.send(SseEmitter.event().name("done").data(""));
                    emitter.complete();
                }
            } catch (Exception e) {
                try {
                    callback.onError("AI调用失败: " + e.getMessage());
                    emitter.send(SseEmitter.event().name("error").data("AI调用失败: " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        });
    }

    @Override
    public String parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("content").get(0).path("text").asText();
        } catch (Exception e) {
            throw new RuntimeException("解析响应失败: " + e.getMessage());
        }
    }
}
