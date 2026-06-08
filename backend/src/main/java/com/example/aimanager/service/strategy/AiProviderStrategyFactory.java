package com.example.aimanager.service.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiProviderStrategyFactory {

    private final Map<String, AiProviderStrategy> strategyCache = new ConcurrentHashMap<>();
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiProviderStrategyFactory() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public AiProviderStrategy getStrategy(String type) {
        return strategyCache.computeIfAbsent(type, this::createStrategy);
    }

    private AiProviderStrategy createStrategy(String type) {
        return switch (type) {
            case "openai", "ali", "baidu", "byte" ->
                    new OpenAiCompatibleStrategy(httpClient, objectMapper);
            case "tencent" -> new TencentStrategy(httpClient, objectMapper);
            case "anthropic" -> new AnthropicStrategy(httpClient, objectMapper);
            default -> throw new IllegalArgumentException("不支持的模型类型: " + type);
        };
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
