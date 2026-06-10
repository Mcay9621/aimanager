package com.example.aimanager.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.http.HttpClient;
import java.util.*;

public class AnthropicStrategy extends AbstractHttpStrategy {

    public AnthropicStrategy(HttpClient httpClient, ObjectMapper objectMapper) {
        super(httpClient, objectMapper);
    }

    @Override
    protected String getChatPath() { return "/v1/messages"; }

    @Override
    protected String getAuthHeaderName() { return "x-api-key"; }

    @Override
    protected String formatAuthValue(String apiKey) { return apiKey; }

    @Override
    protected Map<String, String> getExtraHeaders() {
        return Map.of("anthropic-version", "2023-06-01");
    }

    @Override
    protected boolean processStreamData(String data, StringBuilder contentBuilder,
                                        StreamCallback callback, SseEmitter emitter) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(data);
        String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "";
        if ("content_block_delta".equals(type)) {
            JsonNode delta = jsonNode.path("delta");
            if (delta.has("text")) {
                String token = delta.get("text").asText();
                contentBuilder.append(token);
                callback.onToken(token);
                emitter.send(SseEmitter.event().name("token").data(token));
            }
        }
        if ("message_stop".equals(type)) {
            return false;
        }
        return true;
    }

    @Override
    public String parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("content").get(0).path("text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Parse response failed: " + e.getMessage());
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
            int input = usage.path("input_tokens").asInt(0);
            int output = usage.path("output_tokens").asInt(0);
            Map<String, Integer> result = new HashMap<>();
            result.put("prompt_tokens", input);
            result.put("completion_tokens", output);
            result.put("total_tokens", input + output);
            return result;
        } catch (Exception e) {
            return Map.of("prompt_tokens", 0, "completion_tokens", 0, "total_tokens", 0);
        }
    }
}
