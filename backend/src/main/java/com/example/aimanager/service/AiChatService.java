package com.example.aimanager.service;

import com.example.aimanager.entity.AiModel;
import com.example.aimanager.service.strategy.AiProviderStrategy;
import com.example.aimanager.service.strategy.AiProviderStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    private final AiModelService aiModelService;
    private final AiProviderStrategyFactory strategyFactory;

    public AiChatService(AiModelService aiModelService, AiProviderStrategyFactory strategyFactory) {
        this.aiModelService = aiModelService;
        this.strategyFactory = strategyFactory;
    }

    public Map<String, Object> chat(Long modelId, String message) {
        AiModel model = aiModelService.getById(modelId);
        if (model == null || model.getEnabled() != 1) {
            throw new RuntimeException("模型不存在或已禁用");
        }

        try {
            AiProviderStrategy strategy = strategyFactory.getStrategy(model.getType());
            String response = strategy.chat(model.getEndpoint(), model.getApiKey(),
                    model.getModelName(), message);

            Map<String, Object> result = new HashMap<>();
            result.put("content", response);
            result.put("model", model.getName());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("AI 调用失败: " + e.getMessage());
        }
    }


    public void chatStream(Long modelId, List<Map<String, String>> messages,
                           SseEmitter emitter, AiProviderStrategy.StreamCallback callback) {
        AiModel model = aiModelService.getById(modelId);
        if (model == null || model.getEnabled() != 1) {
            try {
                emitter.send(SseEmitter.event().name("error").data("模型不存在或已禁用"));
                emitter.complete();
            } catch (Exception ignored) {}
            return;
        }

        try {
            AiProviderStrategy strategy = strategyFactory.getStrategy(model.getType());
            strategy.chatStream(model.getEndpoint(), model.getApiKey(),
                    model.getModelName(), messages, emitter, callback);
        } catch (Exception e) {
            try {
                callback.onError("AI调用失败: " + e.getMessage());
                emitter.send(SseEmitter.event().name("error").data("AI调用失败: " + e.getMessage()));
                emitter.complete();
            } catch (Exception ignored) {}
        }
    }
}
