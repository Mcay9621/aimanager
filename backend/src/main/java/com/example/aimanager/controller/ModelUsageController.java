package com.example.aimanager.controller;

import com.example.aimanager.common.Result;
import com.example.aimanager.entity.AiModel;
import com.example.aimanager.mapper.ChatMessageMapper;
import com.example.aimanager.service.AiModelService;
import com.example.aimanager.service.ModelStatusService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/models")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class ModelUsageController {

    private final AiModelService aiModelService;
    private final ChatMessageMapper chatMessageMapper;
    private final ModelStatusService modelStatusService;
    private final Executor modelPingExecutor;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModelUsageController(AiModelService aiModelService, ChatMessageMapper chatMessageMapper,
                                ModelStatusService modelStatusService,
                                @Qualifier("modelPingExecutor") Executor modelPingExecutor) {
        this.aiModelService = aiModelService;
        this.chatMessageMapper = chatMessageMapper;
        this.modelStatusService = modelStatusService;
        this.modelPingExecutor = modelPingExecutor;
    }

    @GetMapping("/balances")
    public ResponseEntity<?> getModelBalances() {
        List<AiModel> models = aiModelService.listWithDecryptedKeys();
        List<Map<String, Object>> modelStats = chatMessageMapper.selectModelUsageStats();
        Map<Long, Map<String, Object>> statsByModelId = new HashMap<>();
        for (Map<String, Object> stat : modelStats) {
            Object mid = stat.get("model_id");
            if (mid instanceof Number) {
                statsByModelId.put(((Number) mid).longValue(), stat);
            }
        }

        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
        for (AiModel model : models) {
            Map<String, Object> stat = statsByModelId.get(model.getId());
            Map<String, Object> baseItem = new HashMap<>();
            baseItem.put("id", model.getId());
            baseItem.put("name", model.getName());
            baseItem.put("type", model.getType());
            baseItem.put("modelName", model.getModelName());
            baseItem.put("enabled", model.getEnabled());
            baseItem.put("sessionCount", stat != null ? ((Number) stat.getOrDefault("session_count", 0)).intValue() : 0);
            baseItem.put("messageCount", stat != null ? ((Number) stat.getOrDefault("message_count", 0)).intValue() : 0);
            baseItem.put("totalTokens", stat != null ? ((Number) stat.getOrDefault("total_tokens", 0)).longValue() : 0L);
            baseItem.put("lastUsed", stat != null ? stat.get("last_used") : null);

            if ("deepseek".equals(model.getType())) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    Map<String, Object> item = new HashMap<>(baseItem);
                    try {
                        long start = System.currentTimeMillis();
                        item.put("available", modelStatusService.testOpenAICompatible(model));
                        item.put("latency", System.currentTimeMillis() - start);
                    } catch (Exception e) {
                        item.put("available", false);
                        item.put("latency", null);
                    }
                    try {
                        item.put("balance", fetchDeepSeekBalance(model));
                    } catch (Exception e) {
                        Map<String, Object> errBalance = new HashMap<>();
                        errBalance.put("supported", true);
                        errBalance.put("total", null);
                        errBalance.put("error", e.getMessage());
                        item.put("balance", errBalance);
                    }
                    return item;
                }, modelPingExecutor));
            } else {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    Map<String, Object> item = new HashMap<>(baseItem);
                    try {
                        long start = System.currentTimeMillis();
                        boolean available = switch (model.getType()) {
                            case "openai", "ali", "baidu", "byte", "tencent" -> modelStatusService.testOpenAICompatible(model);
                            case "anthropic" -> modelStatusService.testAnthropic(model);
                            default -> false;
                        };
                        item.put("available", available);
                        item.put("latency", System.currentTimeMillis() - start);
                    } catch (Exception e) {
                        item.put("available", false);
                        item.put("latency", null);
                    }
                    Map<String, Object> noBalance = new HashMap<>();
                    noBalance.put("supported", false);
                    noBalance.put("total", null);
                    noBalance.put("message", "Balance query not supported");
                    item.put("balance", noBalance);
                    return item;
                }, modelPingExecutor));
            }
        }

        List<Map<String, Object>> result = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/usage")
    public ResponseEntity<?> getModelUsage(@RequestParam(defaultValue = "7") int days) {
        Map<String, Object> overall = chatMessageMapper.selectOverallUsage();
        if (overall == null) overall = Map.of("total_tokens", 0, "total_messages", 0, "total_sessions", 0);
        List<Map<String, Object>> dailyTrend = chatMessageMapper.selectDailyUsage(days);
        List<Map<String, Object>> topModels = chatMessageMapper.selectModelUsageStats();
        long activeModels = aiModelService.lambdaQuery().eq(AiModel::getEnabled, 1).count();

        Map<String, Object> result = new HashMap<>();
        result.put("summary", Map.of(
                "totalTokens", ((Number) overall.getOrDefault("total_tokens", 0)).longValue(),
                "totalMessages", ((Number) overall.getOrDefault("total_messages", 0)).intValue(),
                "totalSessions", ((Number) overall.getOrDefault("total_sessions", 0)).intValue(),
                "activeModels", (int) activeModels
        ));
        result.put("dailyTrend", dailyTrend.stream().map(row -> {
            Map<String, Object> d = new HashMap<>(row);
            for (String key : List.of("prompt_tokens", "completion_tokens", "total_tokens", "message_count")) {
                if (d.get(key) instanceof Number) {
                    d.put(key, ((Number) d.get(key)).longValue());
                }
            }
            return d;
        }).collect(Collectors.toList()));
        result.put("topModels", topModels.stream().map(row -> {
            Map<String, Object> m = new HashMap<>();
            m.put("modelId", row.get("model_id"));
            m.put("modelName", row.get("model_name"));
            m.put("promptTokens", row.getOrDefault("prompt_tokens", 0));
            m.put("completionTokens", row.getOrDefault("completion_tokens", 0));
            m.put("totalTokens", row.getOrDefault("total_tokens", 0));
            m.put("messageCount", row.getOrDefault("message_count", 0));
            m.put("sessionCount", row.getOrDefault("session_count", 0));
            return m;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/usage/deepseek")
    public ResponseEntity<?> getDeepSeekUsage(@RequestParam(defaultValue = "30") int days) {
        List<AiModel> deepSeekModels = aiModelService.listWithDecryptedKeys().stream()
                .filter(m -> "deepseek".equals(m.getType()))
                .collect(Collectors.toList());
        if (deepSeekModels.isEmpty()) {
            return ResponseEntity.ok(Result.success(Map.of("hasDeepSeek", false, "message", "No DeepSeek model configured")));
        }
        LocalDate startDate = LocalDate.now().minusDays(days);
        String modelIds = deepSeekModels.stream()
                .map(m -> String.valueOf(m.getId()))
                .collect(Collectors.joining(","));
        List<Map<String, Object>> localUsage = chatMessageMapper.selectModelDailyUsage(
                modelIds, startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        long totalTokens = 0;
        int totalApiCalls = 0;
        List<Map<String, Object>> dailyBreakdown = new ArrayList<>();
        for (Map<String, Object> row : localUsage) {
            int calls = ((Number) row.getOrDefault("api_calls", 0)).intValue();
            long tokens = ((Number) row.getOrDefault("total_tokens", 0)).longValue();
            totalApiCalls += calls;
            totalTokens += tokens;
            dailyBreakdown.add(Map.of("date", row.get("date"), "apiCalls", calls, "tokens", tokens));
        }
        return ResponseEntity.ok(Result.success(Map.of(
                "hasDeepSeek", true, "source", "local",
                "totalApiCalls", totalApiCalls, "totalTokens", totalTokens,
                "dailyBreakdown", dailyBreakdown)));
    }

    private Map<String, Object> fetchDeepSeekBalance(AiModel model) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/user/balance"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + model.getApiKey())
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode balanceInfos = root.path("balance_infos");
            if (balanceInfos.isArray() && balanceInfos.size() > 0) {
                return Map.of("supported", true, "total", balanceInfos.get(0).path("total_balance").asText(), "currency", "CNY");
            }
        }
        Map<String, Object> errResult = new HashMap<>();
        errResult.put("supported", true);
        errResult.put("total", null);
        errResult.put("error", "Failed to fetch balance");
        return errResult;
    }
}
