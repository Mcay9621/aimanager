package com.example.aimanager.service;

import com.example.aimanager.dto.CompareChatRequest;
import com.example.aimanager.dto.CompareChatResponse;
import com.example.aimanager.dto.CompareResultItem;
import com.example.aimanager.entity.AiModel;
import com.example.aimanager.entity.ChatMessage;
import com.example.aimanager.entity.ChatSession;
import com.example.aimanager.service.strategy.AiProviderStrategy;
import com.example.aimanager.service.strategy.AiProviderStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class AiChatService {

    private final AiModelService aiModelService;
    private final AiProviderStrategyFactory strategyFactory;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final Executor compareExecutor;

    public AiChatService(AiModelService aiModelService, AiProviderStrategyFactory strategyFactory,
                         ChatSessionService chatSessionService, ChatMessageService chatMessageService) {
        this.aiModelService = aiModelService;
        this.strategyFactory = strategyFactory;
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
        this.compareExecutor = Executors.newFixedThreadPool(8);
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

    public CompareChatResponse compareChat(CompareChatRequest req, String username) {
        // 1. 创建或复用会话
        ChatSession session;
        if (req.getSessionId() != null) {
            session = chatSessionService.getById(req.getSessionId());
            if (session == null || !session.getUsername().equals(username)) {
                throw new RuntimeException("会话不存在");
            }
        } else {
            session = new ChatSession();
            session.setTitle(truncateTitle(req.getMessage()));
            session.setUsername(username);
            session.setSessionType("compare");
            session.setModelId(0L);
            session.setModelName("");
            session.setMessageCount(0);
            chatSessionService.save(session);
        }

        final Long sessionId = session.getId();

        // 2. 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(req.getMessage());
        chatMessageService.save(userMsg);

        // 3. 获取所有模型信息
        List<AiModel> models = new ArrayList<>();
        for (Long modelId : req.getModelIds()) {
            AiModel model = aiModelService.getById(modelId);
            if (model != null && model.getEnabled() == 1) {
                models.add(model);
            }
        }
        if (models.isEmpty()) {
            throw new RuntimeException("没有可用的在线模型");
        }

        // 4. 并行调用每个模型
        List<CompletableFuture<CompareResultItem>> futures = new ArrayList<>();
        for (AiModel model : models) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                CompareResultItem item = new CompareResultItem();
                item.setModelId(model.getId());
                item.setModelName(model.getName());
                long start = System.currentTimeMillis();
                try {
                    AiProviderStrategy strategy = strategyFactory.getStrategy(model.getType());
                    String content = strategy.chat(model.getEndpoint(), model.getApiKey(),
                            model.getModelName(), req.getMessage());
                    item.setContent(content);
                    item.setLatencyMs(System.currentTimeMillis() - start);

                    // 保存 assistant 消息
                    ChatMessage assistantMsg = new ChatMessage();
                    assistantMsg.setSessionId(sessionId);
                    assistantMsg.setRole("assistant");
                    assistantMsg.setContent(content);
                    assistantMsg.setModelName(model.getName());
                    chatMessageService.save(assistantMsg);
                } catch (Exception e) {
                    item.setError(e.getMessage());
                    item.setLatencyMs(System.currentTimeMillis() - start);
                }
                return item;
            }, compareExecutor).orTimeout(15, TimeUnit.SECONDS).exceptionally(e -> {
                CompareResultItem item = new CompareResultItem();
                item.setModelId(model.getId());
                item.setModelName(model.getName());
                item.setError("请求超时或失败: " + e.getMessage());
                return item;
            }));
        }

        // 5. 等待全部完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 6. 收集结果
        List<CompareResultItem> results = new ArrayList<>();
        for (CompletableFuture<CompareResultItem> f : futures) {
            results.add(f.join());
        }

        // 7. 更新会话消息数
        int count = (int) chatMessageService.count(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId));
        session.setMessageCount(count);
        chatSessionService.updateById(session);

        // 8. 组装响应
        CompareChatResponse response = new CompareChatResponse();
        response.setSessionId(sessionId);
        response.setResults(results);
        response.setUserMessage(req.getMessage());
        return response;
    }

    private String truncateTitle(String text) {
        if (text == null) return "新对话";
        String clean = text.replaceAll("\\s+", " ");
        return clean.length() > 50 ? clean.substring(0, 50) + "..." : clean;
    }

    public interface StreamCallback {
        void onToken(String token);
        default void onDone(String fullContent) {}
        default void onDone(String fullContent, Map<String, Integer> usage) { onDone(fullContent); }
        default void onError(String error) {}
    }

    public void chatStream(Long modelId, List<Map<String, String>> messages,
                           SseEmitter emitter, StreamCallback callback) {
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
                    model.getModelName(), messages, emitter, new AiProviderStrategy.StreamCallback() {
                @Override
                public void onToken(String token) {
                    callback.onToken(token);
                }

                @Override
                public void onDone(String content) {
                    callback.onDone(content);
                }

                @Override
                public void onDone(String content, Map<String, Integer> usage) {
                    callback.onDone(content, usage);
                }

                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
            });
        } catch (Exception e) {
            try {
                callback.onError("AI调用失败: " + e.getMessage());
                emitter.send(SseEmitter.event().name("error").data("AI调用失败: " + e.getMessage()));
                emitter.complete();
            } catch (Exception ignored) {}
        }
    }
}
