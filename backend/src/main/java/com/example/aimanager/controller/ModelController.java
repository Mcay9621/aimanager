package com.example.aimanager.controller;

import com.example.aimanager.common.Result;
import com.example.aimanager.entity.AiModel;
import com.example.aimanager.service.AiModelService;
import com.example.aimanager.service.ModelStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class ModelController {

    private final AiModelService aiModelService;
    private final ModelStatusService modelStatusService;

    public ModelController(AiModelService aiModelService, ModelStatusService modelStatusService) {
        this.aiModelService = aiModelService;
        this.modelStatusService = modelStatusService;
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

    @GetMapping("/models/{id}/ping")
    public ResponseEntity<?> pingModel(@PathVariable Long id) {
        AiModel model = aiModelService.getById(id);
        if (model == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("Model not found"));
        }
        Map<String, Object> result = modelStatusService.pingModel(model);
        return ResponseEntity.ok(Result.success(result));
    }

    @PostMapping("/models/refresh")
    public ResponseEntity<?> refreshModels() {
        List<Map<String, Object>> statuses = modelStatusService.refreshAll();
        return ResponseEntity.ok(Result.success(statuses));
    }
}
