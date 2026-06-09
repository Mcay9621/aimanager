package com.example.aimanager.controller;

import com.example.aimanager.common.Result;
import com.example.aimanager.entity.AiModel;
import com.example.aimanager.mapper.ChatMessageMapper;
import com.example.aimanager.service.AiModelService;
import com.example.aimanager.service.ModelStatusService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ModelController {

    private final AiModelService aiModelService;
    private final ChatMessageMapper chatMessageMapper;
    private final ModelStatusService modelStatusService;
    private final Executor modelPingExecutor;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModelController(AiModelService aiModelService, ChatMessageMapper chatMessageMapper,
                           ModelStatusService modelStatusService,
                           @Qualifier("modelPingExecutor") Executor modelPingExecutor) {
        this.aiModelService = aiModelService;
        this.chatMessageMapper = chatMessageMapper;
        this.modelStatusService = modelStatusService;
        this.modelPingExecutor = modelPingExecutor;
    }

    @GetMapping("/models")
    public ResponseEntity<?> getEnabledModels() {
        List<AiModel> models = aiModelService.getEnabledModels();
        List<Map<String, Object>> result = models.stream().map(m -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("name", m.getName());
            item.put("type", m.getType());
            item.put("modelName", m.getModelName());
            item.put("enabled", m.getEnabled());
            Map<String, Object> cached = modelStatusService.getStatus(m.getId());
            if (cached != null) {
                item.put("_available", cached.get("available"));
                item.put("_latency", cached.get("latency"));
            } else {
                item.put("_available", null);
                item.put("_latency", null);
            }
            return item;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/admin/models")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getAllModels() {
        List<AiModel> models = aiModelService.list();
        List<Map<String, Object>> result = models.stream().map(m -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("name", m.getName());
            item.put("type", m.getType());
            item.put("modelName", m.getModelName());
            item.put("endpoint", m.getEndpoint());
            item.put("enabled", m.getEnabled());
            Map<String, Object> cached = modelStatusService.getStatus(m.getId());
            if (cached != null) {
                item.put("_available", cached.get("available"));
                item.put("_latency", cached.get("latency"));
            } else {
                item.put("_available", null);
                item.put("_latency", null);
            }
            return item;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Result.success(result));
    }

    @PostMapping("/admin/models")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> addModel(@RequestBody AiModel model) {
        aiModelService.save(model);
        return ResponseEntity.ok(Result.success(Map.of("message", "添加成功")));
    }

    @PutMapping("/admin/models/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateModel(@PathVariable Long id, @RequestBody AiModel model) {
        model.setId(id);
        aiModelService.updateById(model);
        return ResponseEntity.ok(Result.success(Map.of("message", "更新成功")));
    }

    @DeleteMapping("/admin/models/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deleteModel(@PathVariable Long id) {
        aiModelService.removeById(id);
        modelStatusService.deleteStatus(id);
        return ResponseEntity.ok(Result.success(Map.of("message", "删除成功")));
    }

    @PostMapping("/admin/models/{id}/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> testModel(@PathVariable Long id) {
        AiModel model = aiModelService.getById(id);
        if (model == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("模型不存在"));
        }

        try {
            boolean connected = switch (model.getType()) {
                case "openai", "ali", "baidu", "byte", "tencent", "deepseek" -> modelStatusService.testOpenAICompatible(model);
                case "anthropic" -> modelStatusService.testAnthropic(model);
                default -> false;
            };

            if (connected) {
                return ResponseEntity.ok(Result.success(Map.of("message", "连接成功", "connected", true)));
            } else {
                return ResponseEntity.ok(Result.success(Map.of("message", "连接失败：请检查 API 地址和密钥是否正确", "connected", false)));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Result.success(Map.of("message", "连接失败: " + e.getMessage(), "connected", false)));
        }
    }

    @GetMapping("/models/{id}/ping")
    public ResponseEntity<?> pingModel(@PathVariable Long id) {
        AiModel model = aiModelService.getById(id);
        if (model == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("模型不存在"));
        }
        Map<String, Object> result = modelStatusService.pingModel(model);
        return ResponseEntity.ok(Result.success(result));
    }

    @PostMapping("/models/refresh")
    public ResponseEntity<?> refreshModels() {
        List<Map<String, Object>> statuses = modelStatusService.refreshAll();
        return ResponseEntity.ok(Result.success(statuses));
    }

    @PostMapping("/admin/models/fetch")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> fetchModels(@RequestBody Map<String, String> request) {
        String endpoint = request.get("endpoint");
        String apiKey = request.get("apiKey");

        if (endpoint == null || apiKey == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("endpoint 和 apiKey 不能为空"));
        }

        try {
            // 反推 base URL: 去掉末尾的 /chat/completions 或最后一段路径
            String baseUrl = endpoint.replaceAll("/chat/completions?$", "");
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String modelsUrl = baseUrl + "/models";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(modelsUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String responseBody = response.body();
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode data = root.path("data");
                List<Map<String, String>> modelList = new ArrayList<>();
                if (data.isArray()) {
                    for (JsonNode node : data) {
                        String modelId = node.path("id").asText();
                        Map<String, String> item = new HashMap<>();
                        item.put("id", modelId);
                        modelList.add(item);
                    }
                }
                return ResponseEntity.ok(Result.success(modelList));
            } else {
                String errorBody = response.body();
                String message = "获取模型列表失败: " + response.statusCode();
                try {
                    JsonNode errorNode = objectMapper.readTree(errorBody);
                    if (errorNode.has("error")) {
                        message += " - " + errorNode.path("error").path("message").asText();
                    }
                } catch (Exception ignored) {}
                return ResponseEntity.ok(Result.badRequest(message));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Result.badRequest("请求失败: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/models/balances")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
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

        // 并行执行各模型的连通性检测和余额查询
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

            // DeepSeek 余额查询（在连通性检测前并行执行）
            if ("deepseek".equals(model.getType())) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    Map<String, Object> item = new HashMap<>(baseItem);
                    try {
                        long start = System.currentTimeMillis();
                        boolean available = modelStatusService.testOpenAICompatible(model);
                        item.put("available", available);
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
                    noBalance.put("message", "该厂商不支持通过 API 查询余额");
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

    @GetMapping("/admin/models/usage")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getModelUsage(@RequestParam(defaultValue = "7") int days) {
        // 整体统计
        Map<String, Object> overall = chatMessageMapper.selectOverallUsage();
        if (overall == null) overall = Map.of("total_tokens", 0, "total_messages", 0, "total_sessions", 0);

        // 按天趋势
        List<Map<String, Object>> dailyTrend = chatMessageMapper.selectDailyUsage(days);

        // 按模型聚合 (Top 排行榜)
        List<Map<String, Object>> topModels = chatMessageMapper.selectModelUsageStats();

        // 活跃模型数
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
            // Convert Decimal/Integer to Long for JSON
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

    @GetMapping("/admin/models/usage/deepseek")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getDeepSeekUsage(@RequestParam(defaultValue = "30") int days) {
        // 找出所有 DeepSeek 类型的模型（使用 listWithDecryptedKeys 解密 API key）
        List<AiModel> deepSeekModels = aiModelService.listWithDecryptedKeys().stream()
                .filter(m -> "deepseek".equals(m.getType()))
                .collect(Collectors.toList());
        if (deepSeekModels.isEmpty()) {
            return ResponseEntity.ok(Result.success(Map.of(
                    "hasDeepSeek", false,
                    "message", "未配置 DeepSeek 模型"
            )));
        }

        // 从本地 chat_message 表统计 DeepSeek 用量（token 数 + 调用次数）
        LocalDate startDate = LocalDate.now().minusDays(days);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String modelIds = deepSeekModels.stream()
                .map(m -> String.valueOf(m.getId()))
                .collect(Collectors.joining(","));

        List<Map<String, Object>> localUsage = chatMessageMapper.selectModelDailyUsage(
                modelIds, startDate.format(fmt));

        long totalTokens = 0;
        int totalApiCalls = 0;
        List<Map<String, Object>> dailyBreakdown = new ArrayList<>();

        for (Map<String, Object> row : localUsage) {
            int calls = ((Number) row.getOrDefault("api_calls", 0)).intValue();
            long tokens = ((Number) row.getOrDefault("total_tokens", 0)).longValue();
            totalApiCalls += calls;
            totalTokens += tokens;
            Map<String, Object> d = new HashMap<>();
            d.put("date", row.get("date"));
            d.put("apiCalls", calls);
            d.put("tokens", tokens);
            dailyBreakdown.add(d);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("hasDeepSeek", true);
        result.put("source", "local");
        result.put("totalApiCalls", totalApiCalls);
        result.put("totalTokens", totalTokens);
        result.put("dailyBreakdown", dailyBreakdown);
        return ResponseEntity.ok(Result.success(result));
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
                String totalBalance = balanceInfos.get(0).path("total_balance").asText();
                return Map.of(
                        "supported", true,
                        "total", totalBalance,
                        "currency", "CNY"
                );
            }
        }
        Map<String, Object> errResult = new HashMap<>();
        errResult.put("supported", true);
        errResult.put("total", null);
        errResult.put("error", "无法获取余额");
        return errResult;
    }

}
