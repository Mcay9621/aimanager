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

public class OpenAiCompatibleStrategy implements AiProviderStrategy {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String endpointSuffix;
    private final Map<String, String> extraHeaders;

    public OpenAiCompatibleStrategy(HttpClient httpClient, ObjectMapper objectMapper,
                                    String endpointSuffix, Map<String, String> extraHeaders) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.endpointSuffix = endpointSuffix;
        this.extraHeaders = extraHeaders != null ? extraHeaders : new HashMap<>();
    }

    public OpenAiCompatibleStrategy(HttpClient httpClient, ObjectMapper objectMapper) {
        this(httpClient, objectMapper, "/chat/completions", new HashMap<>());
    }

    @Override
    public String chat(String endpoint, String apiKey, String modelName, String message) {
        try {
            String url = endpoint + endpointSuffix;
            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName);
            body.put("messages", List.of(Map.of("role", "user", "content", message)));
            body.put("max_tokens", 4096);

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
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
        String url = endpoint + endpointSuffix;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("stream", true);
        requestBody.put("max_tokens", 4096);
        requestBody.put("model", modelName);
        requestBody.put("messages", messages);

        CompletableFuture.runAsync(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(120))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey);

                extraHeaders.forEach(builder::header);
                builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));

                HttpResponse<InputStream> response = httpClient.send(builder.build(),
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
                    Map<String, Integer> finalUsage = null;

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) break;

                            try {
                                JsonNode jsonNode = objectMapper.readTree(data);
                                // Check for usage in this chunk (providers like DeepSeek send it before [DONE])
                                JsonNode usage = jsonNode.path("usage");
                                if (!usage.isMissingNode() && !usage.isNull()) {
                                    Map<String, Integer> usageMap = new HashMap<>();
                                    usageMap.put("prompt_tokens", usage.path("prompt_tokens").asInt(0));
                                    usageMap.put("completion_tokens", usage.path("completion_tokens").asInt(0));
                                    usageMap.put("total_tokens", usage.path("total_tokens").asInt(0));
                                    finalUsage = usageMap;
                                }
                                JsonNode choices = jsonNode.path("choices");
                                if (choices.isArray() && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).path("delta");
                                    if (delta.has("content")) {
                                        String token = delta.get("content").asText();
                                        contentBuilder.append(token);
                                        callback.onToken(token);
                                        emitter.send(SseEmitter.event().name("token").data(token));
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    if (finalUsage != null) {
                        callback.onDone(contentBuilder.toString(), finalUsage);
                    } else {
                        callback.onDone(contentBuilder.toString());
                    }
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
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("解析响应失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Integer> parseUsage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode usage = root.path("usage");
            if (usage.isMissingNode() || usage.isNull()) {
                return Map.of("prompt_tokens", 0, "completion_tokens", 0, "total_tokens", 0);
            }
            Map<String, Integer> result = new HashMap<>();
            result.put("prompt_tokens", usage.path("prompt_tokens").asInt(0));
            result.put("completion_tokens", usage.path("completion_tokens").asInt(0));
            result.put("total_tokens", usage.path("total_tokens").asInt(0));
            return result;
        } catch (Exception e) {
            return Map.of("prompt_tokens", 0, "completion_tokens", 0, "total_tokens", 0);
        }
    }
}
