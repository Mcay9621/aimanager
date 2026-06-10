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

public abstract class AbstractHttpStrategy implements AiProviderStrategy {

    protected final HttpClient httpClient;
    protected final ObjectMapper objectMapper;

    protected AbstractHttpStrategy(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    // === Abstract hooks ===
    protected abstract String getChatPath();
    protected abstract String getAuthHeaderName();
    protected String formatAuthValue(String apiKey) { return "Bearer " + apiKey; }
    protected Map<String, String> getExtraHeaders() { return Map.of(); }

    @Override
    public String chat(String endpoint, String apiKey, String modelName, String message) {
        try {
            String url = endpoint + getChatPath();
            String jsonBody = objectMapper.writeValueAsString(buildChatBody(modelName, message));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header(getAuthHeaderName(), formatAuthValue(apiKey))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseResponse(response.body());
            }
            throw new RuntimeException("API request failed: " + response.statusCode() + " - " + response.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("AI call failed: " + e.getMessage());
        }
    }

    private Map<String, Object> buildChatBody(String modelName, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("messages", List.of(Map.of("role", "user", "content", message)));
        body.put("max_tokens", 4096);
        return body;
    }

    @Override
    public void chatStream(String endpoint, String apiKey, String modelName,
                           List<Map<String, String>> messages, SseEmitter emitter,
                           StreamCallback callback) {
        String url = endpoint + getChatPath();
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
                        .header(getAuthHeaderName(), formatAuthValue(apiKey));
                getExtraHeaders().forEach(builder::header);
                builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));

                HttpResponse<InputStream> response = httpClient.send(builder.build(),
                        HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    String errorBody = new String(response.body().readAllBytes());
                    callback.onError("API request failed: " + response.statusCode());
                    emitter.send(SseEmitter.event().name("error").data("API request failed: " + response.statusCode()));
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
                            if (!processStreamData(data, contentBuilder, callback, emitter)) {
                                break;
                            }
                        }
                    }

                    callback.onDone(contentBuilder.toString());
                    emitter.send(SseEmitter.event().name("done").data(""));
                    emitter.complete();
                }
            } catch (Exception e) {
                try {
                    callback.onError("AI stream failed: " + e.getMessage());
                    emitter.send(SseEmitter.event().name("error").data("AI stream failed: " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        });
    }

    // Subclasses override to parse one SSE data line; return true to continue, false to stop
    protected boolean processStreamData(String data, StringBuilder contentBuilder,
                                        StreamCallback callback, SseEmitter emitter) throws Exception {
        return true;
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
