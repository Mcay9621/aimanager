package com.example.aimanager.controller;

import com.example.aimanager.common.Result;
import com.example.aimanager.entity.AiModel;
import com.example.aimanager.service.AiModelService;
import com.example.aimanager.service.ModelStatusService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.aimanager.common.LogAudit;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminModelController {

    private final AiModelService aiModelService;
    private final ModelStatusService modelStatusService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminModelController(AiModelService aiModelService, ModelStatusService modelStatusService) {
        this.aiModelService = aiModelService;
        this.modelStatusService = modelStatusService;
    }

    @GetMapping("/models")
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

    @PostMapping("/models")
    @LogAudit(action = "CREATE", target = "Model", detail = "addModel")
    public ResponseEntity<?> addModel(@RequestBody AiModel model) {
        aiModelService.save(model);
        return ResponseEntity.ok(Result.success(Map.of("message", "Added")));
    }

    @PutMapping("/models/{id}")
    @LogAudit(action = "UPDATE", target = "Model", detail = "updateModel")
    public ResponseEntity<?> updateModel(@PathVariable Long id, @RequestBody AiModel model) {
        model.setId(id);
        aiModelService.updateById(model);
        return ResponseEntity.ok(Result.success(Map.of("message", "Updated")));
    }

    @DeleteMapping("/models/{id}")
    @LogAudit(action = "DELETE", target = "Model", detail = "deleteModel")
    public ResponseEntity<?> deleteModel(@PathVariable Long id) {
        aiModelService.removeById(id);
        modelStatusService.deleteStatus(id);
        return ResponseEntity.ok(Result.success(Map.of("message", "Deleted")));
    }

    @PostMapping("/models/{id}/test")
    public ResponseEntity<?> testModel(@PathVariable Long id) {
        AiModel model = aiModelService.getById(id);
        if (model == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("Model not found"));
        }
        try {
            boolean connected = switch (model.getType()) {
                case "openai", "ali", "baidu", "byte", "tencent", "deepseek" -> modelStatusService.testOpenAICompatible(model);
                case "anthropic" -> modelStatusService.testAnthropic(model);
                default -> false;
            };
            return ResponseEntity.ok(Result.success(Map.of("message", connected ? "Connected" : "Failed", "connected", connected)));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.success(Map.of("message", "Failed: " + e.getMessage(), "connected", false)));
        }
    }

    @PostMapping("/models/fetch")
    public ResponseEntity<?> fetchModels(@RequestBody Map<String, String> request) {
        String endpoint = request.get("endpoint");
        String apiKey = request.get("apiKey");
        if (endpoint == null || apiKey == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("endpoint and apiKey required"));
        }
        try {
            String baseUrl = endpoint.replaceAll("/chat/completions?$", "");
            if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            String modelsUrl = baseUrl + "/models";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(modelsUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(resp.body());
                JsonNode data = root.path("data");
                List<Map<String, String>> modelList = new ArrayList<>();
                if (data.isArray()) {
                    for (JsonNode node : data) {
                        modelList.add(Map.of("id", node.path("id").asText()));
                    }
                }
                return ResponseEntity.ok(Result.success(modelList));
            }
            return ResponseEntity.ok(Result.badRequest("Fetch failed: " + resp.statusCode()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.badRequest("Request failed: " + e.getMessage()));
        }
    }
}
