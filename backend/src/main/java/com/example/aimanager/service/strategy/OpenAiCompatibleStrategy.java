package com.example.aimanager.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.http.HttpClient;
import java.util.*;

public class OpenAiCompatibleStrategy extends AbstractHttpStrategy {

    private final String endpointSuffix;
    private final Map<String, String> extraHeaders;

    public OpenAiCompatibleStrategy(HttpClient httpClient, ObjectMapper objectMapper,
                                    String endpointSuffix, Map<String, String> extraHeaders) {
        super(httpClient, objectMapper);
        this.endpointSuffix = endpointSuffix;
        this.extraHeaders = extraHeaders != null ? extraHeaders : new HashMap<>();
    }

    public OpenAiCompatibleStrategy(HttpClient httpClient, ObjectMapper objectMapper) {
        this(httpClient, objectMapper, "/chat/completions", new HashMap<>());
    }

    @Override
    protected String getChatPath() { return endpointSuffix; }

    @Override
    protected String getAuthHeaderName() { return "Authorization"; }

    @Override
    protected Map<String, String> getExtraHeaders() { return extraHeaders; }

    @Override
    protected boolean processStreamData(String data, StringBuilder contentBuilder,
                                        StreamCallback callback, SseEmitter emitter) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(data);
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
        return true;
    }

    @Override
    public String parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Parse response failed: " + e.getMessage());
        }
    }
}
