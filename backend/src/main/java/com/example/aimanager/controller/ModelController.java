package com.example.aimanager.controller;

import com.example.aimanager.common.Result;
import com.example.aimanager.entity.AiModel;
import com.example.aimanager.service.AiModelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ModelController {

    private final AiModelService aiModelService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public ModelController(AiModelService aiModelService) {
        this.aiModelService = aiModelService;
    }

    @GetMapping("/models")
    public ResponseEntity<?> getEnabledModels() {
        List<AiModel> models = aiModelService.getEnabledModels();
        return ResponseEntity.ok(Result.success(models));
    }

    @GetMapping("/admin/models")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getAllModels() {
        List<AiModel> models = aiModelService.list();
        return ResponseEntity.ok(Result.success(models));
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
                case "openai", "ali", "baidu", "byte", "tencent" -> testOpenAICompatible(model);
                case "anthropic" -> testAnthropic(model);
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

    private boolean testOpenAICompatible(AiModel model) throws Exception {
        String url = model.getEndpoint() + "/chat/completions";
        String body = "{\"model\":\"" + model.getModelName() + "\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"max_tokens\":1}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + model.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    private boolean testAnthropic(AiModel model) throws Exception {
        String url = model.getEndpoint() + "/v1/messages";
        String body = "{\"model\":\"" + model.getModelName() + "\",\"max_tokens\":1,\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("x-api-key", model.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }
}
