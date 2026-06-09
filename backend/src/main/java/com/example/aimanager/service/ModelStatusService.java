package com.example.aimanager.service;

import com.example.aimanager.entity.AiModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
public class ModelStatusService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AiModelService aiModelService;
    private final Executor modelPingExecutor;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String KEY_PREFIX = "model:status:";
    private static final long TTL_SECONDS = 60;

    public ModelStatusService(RedisTemplate<String, Object> redisTemplate, AiModelService aiModelService,
                              @Qualifier("modelPingExecutor") Executor modelPingExecutor) {
        this.redisTemplate = redisTemplate;
        this.aiModelService = aiModelService;
        this.modelPingExecutor = modelPingExecutor;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getStatus(Long modelId) {
        return (Map<String, Object>) redisTemplate.opsForValue().get(KEY_PREFIX + modelId);
    }

    public void setStatus(Long modelId, boolean available, long latency) {
        Map<String, Object> status = new HashMap<>();
        status.put("available", available);
        status.put("latency", latency);
        status.put("timestamp", System.currentTimeMillis());
        redisTemplate.opsForValue().set(KEY_PREFIX + modelId, status, TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void deleteStatus(Long modelId) {
        redisTemplate.delete(KEY_PREFIX + modelId);
    }

    /**
     * 对单个模型执行 ping 并写入 Redis 缓存，返回状态信息
     */
    public Map<String, Object> pingModel(AiModel model) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", model.getId());
        long start = System.currentTimeMillis();
        try {
            boolean ok = testModel(model);
            long latency = System.currentTimeMillis() - start;
            setStatus(model.getId(), ok, latency);
            result.put("available", ok);
            result.put("latency", latency);
        } catch (Exception e) {
            setStatus(model.getId(), false, 0);
            result.put("available", false);
            result.put("latency", null);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 批量刷新所有模型的连通性状态，结果写入 Redis（并行执行）
     */
    public List<Map<String, Object>> refreshAll() {
        List<AiModel> models = aiModelService.listWithDecryptedKeys();
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();

        for (AiModel model : models) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                Map<String, Object> s = new HashMap<>();
                s.put("id", model.getId());
                try {
                    long start = System.currentTimeMillis();
                    boolean ok = testModel(model);
                    long latency = System.currentTimeMillis() - start;
                    setStatus(model.getId(), ok, latency);
                    s.put("available", ok);
                    s.put("latency", latency);
                } catch (Exception e) {
                    setStatus(model.getId(), false, 0);
                    s.put("available", false);
                    s.put("latency", null);
                }
                return s;
            }, modelPingExecutor));
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private boolean testModel(AiModel model) throws Exception {
        return switch (model.getType()) {
            case "openai", "ali", "baidu", "byte", "tencent", "deepseek" -> testOpenAICompatible(model);
            case "anthropic" -> testAnthropic(model);
            default -> false;
        };
    }

    public boolean testOpenAICompatible(AiModel model) throws Exception {
        String url = model.getEndpoint() + "/chat/completions";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model.getModelName());
        body.put("max_tokens", 1);
        body.putArray("messages").add(
                objectMapper.createObjectNode().put("role", "user").put("content", "hi"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + model.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    public boolean testAnthropic(AiModel model) throws Exception {
        String url = model.getEndpoint() + "/v1/messages";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model.getModelName());
        body.put("max_tokens", 1);
        body.putArray("messages").add(
                objectMapper.createObjectNode().put("role", "user").put("content", "hi"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("x-api-key", model.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }
}
