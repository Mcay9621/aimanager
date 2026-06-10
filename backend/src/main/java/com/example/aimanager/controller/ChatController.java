package com.example.aimanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimanager.config.SseProperties;
import com.example.aimanager.dto.CreateSessionRequest;
import com.example.aimanager.dto.StreamChatRequest;
import com.example.aimanager.dto.UpdateSessionRequest;
import com.example.aimanager.dto.ChatRequest;
import com.example.aimanager.common.Result;
import com.example.aimanager.entity.ChatMessage;
import com.example.aimanager.entity.ChatSession;
import com.example.aimanager.entity.AiModel;
import com.example.aimanager.service.strategy.AiProviderStrategy;
import com.example.aimanager.service.AiChatService;
import com.example.aimanager.service.AiModelService;
import com.example.aimanager.service.ChatMessageService;
import com.example.aimanager.service.ChatSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final AiChatService aiChatService;
    private final ChatSessionService chatSessionService;
    private final java.util.concurrent.Executor chatAsyncExecutor;
    private final ChatMessageService chatMessageService;
    private final AiModelService aiModelService;
    private final SseProperties sseProperties;

    public ChatController(AiChatService aiChatService, ChatSessionService chatSessionService,
                          ChatMessageService chatMessageService, AiModelService aiModelService,
                          SseProperties sseProperties,
                           java.util.concurrent.Executor chatAsyncExecutor) {
        this.aiChatService = aiChatService;
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
        this.aiModelService = aiModelService;
        this.sseProperties = sseProperties;
        this.chatAsyncExecutor = chatAsyncExecutor;
    }

    // ==================== 同步聊天（向后兼容） ====================

    @PostMapping
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request) {
        try {
            Map<String, Object> result = aiChatService.chat(request.getModelId(), request.getMessage());
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Result.badRequest(e.getMessage()));
        }
    }

    // ==================== SSE 流式聊天 ====================

    @PostMapping("/stream")
    public SseEmitter streamChat(@Valid @RequestBody StreamChatRequest request, Authentication auth) {
        SseEmitter emitter = new SseEmitter(sseProperties.getTimeout());

        String username = auth != null ? auth.getName() : "anonymous";

        // 校验 sessionId（如果提供）
        Long existingSessionId = null;
        if (request.getSessionId() != null) {
            existingSessionId = request.getSessionId();
            ChatSession s = chatSessionService.getById(existingSessionId);
            if (s == null || !s.getUsername().equals(username)) {
                sendError(emitter, "会话不存在");
                return emitter;
            }
        }

        // 校验模型
        AiModel model = aiModelService.getById(request.getModelId());
        if (model == null || model.getEnabled() != 1) {
            sendError(emitter, "模型不存在或已禁用");
            return emitter;
        }

        // 创建或更新会话
        final Long sessionId = createOrGetSession(existingSessionId, model, request.getMessage(), username);
        if (sessionId == null) {
            sendError(emitter, "创建会话失败");
            return emitter;
        }

        // 发送 sessionId 给前端
        sendSessionIdEvent(emitter, sessionId);

        // 保存用户消息
        saveMessage(sessionId, "user", request.getMessage());

        // 构建多轮对话上下文：加载历史消息 + 当前用户消息
        List<Map<String, String>> messages = buildChatMessages(sessionId);

        // 在异步线程中执行流式调用
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                aiChatService.chatStream(request.getModelId(), messages, emitter, new AiProviderStrategy.StreamCallback() {
                    final StringBuilder fullContent = new StringBuilder();

                    @Override
                    public void onToken(String token) {
                        fullContent.append(token);
                    }

                    @Override
                    public void onDone(String content) {
                        saveAssistantMessage(sessionId, fullContent.toString(), null);
                    }

                    @Override
                    public void onDone(String content, Map<String, Integer> usage) {
                        saveAssistantMessage(sessionId, fullContent.toString(), usage);
                    }

                    @Override
                    public void onError(String error) {
                        if (fullContent.length() > 0) {
                            saveMessage(sessionId, "assistant", fullContent.toString() + "\n[错误: " + error + "]");
                        }
                    }
                });
            } catch (Exception e) {
                sendError(emitter, "流式调用失败: " + e.getMessage());
            }
        }, chatAsyncExecutor);

        return emitter;
    }

    /**
     * 构建多轮对话消息列表：从数据库加载历史消息 + 当前消息已保存
     */
    private List<Map<String, String>> buildChatMessages(Long sessionId) {
        List<ChatMessage> history = chatMessageService.list(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime));

        List<Map<String, String>> messages = new java.util.ArrayList<>();
        for (ChatMessage msg : history) {
            Map<String, String> m = new java.util.HashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            messages.add(m);
        }
        return messages;
    }

    private Long createOrGetSession(Long existingSessionId, AiModel model, String message, String username) {
        if (existingSessionId != null) {
            return existingSessionId;
        }
        ChatSession session = new ChatSession();
        session.setTitle(truncateTitle(message));
        session.setModelId(model.getId());
        session.setModelName(model.getName());
        session.setUsername(username);
        session.setMessageCount(0);
        chatSessionService.save(session);
        return session.getId();
    }

    private void sendSessionIdEvent(SseEmitter emitter, Long sessionId) {
        try {
            emitter.send(SseEmitter.event().name("sessionId").data(String.valueOf(sessionId)));
        } catch (Exception e) {
            // ignore
        }
    }

    private void saveAssistantMessage(Long sessionId, String content, Map<String, Integer> usage) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole("assistant");
        msg.setContent(content);
        if (usage != null) {
            msg.setPromptTokens(usage.getOrDefault("prompt_tokens", 0));
            msg.setCompletionTokens(usage.getOrDefault("completion_tokens", 0));
            msg.setTotalTokens(usage.getOrDefault("total_tokens", 0));
        }
        chatMessageService.save(msg);

        // 原子更新会话消息计数，省去一次 DB 查询
        chatSessionService.lambdaUpdate()
                .eq(ChatSession::getId, sessionId)
                .setSql("message_count = message_count + 1")
                .update();
    }

    private ChatMessage saveMessage(Long sessionId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        chatMessageService.save(msg);
        return msg;
    }

    private void sendError(SseEmitter emitter, String error) {
        try {
            emitter.send(SseEmitter.event().name("error").data(error));
            emitter.complete();
        } catch (Exception e) {
            // ignore
        }
    }

    // ==================== 会话管理 ====================

    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(Authentication auth) {
        String username = auth.getName();
        List<ChatSession> sessions = chatSessionService.list(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUsername, username)
                        .orderByDesc(ChatSession::getUpdateTime));
        return ResponseEntity.ok(Result.success(sessions));
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@Valid @RequestBody CreateSessionRequest request, Authentication auth) {
        AiModel model = aiModelService.getById(request.getModelId());
        if (model == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("模型不存在"));
        }

        ChatSession session = new ChatSession();
        session.setTitle(request.getTitle() != null ? request.getTitle() : "新对话");
        session.setModelId(model.getId());
        session.setModelName(model.getName());
        session.setUsername(auth.getName());
        session.setMessageCount(0);
        chatSessionService.save(session);

        return ResponseEntity.ok(Result.success(session));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable Long id, Authentication auth) {
        ChatSession session = chatSessionService.getById(id);
        if (session == null) {
            return ResponseEntity.status(404).body(Result.notFound("会话不存在"));
        }
        if (!session.getUsername().equals(auth.getName())) {
            return ResponseEntity.status(403).body(Result.forbidden("无权删除此会话"));
        }
        chatMessageService.remove(
                new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, id));
        chatSessionService.removeById(id);
        return ResponseEntity.ok(Result.success(Map.of("message", "删除成功")));
    }

    @PutMapping("/sessions/{id}")
    public ResponseEntity<?> updateSession(@PathVariable Long id, @Valid @RequestBody UpdateSessionRequest request,
                                           Authentication auth) {
        ChatSession session = chatSessionService.getById(id);
        if (session == null) {
            return ResponseEntity.status(404).body(Result.notFound("会话不存在"));
        }
        if (!session.getUsername().equals(auth.getName())) {
            return ResponseEntity.status(403).body(Result.forbidden("无权修改此会话"));
        }
        if (request.getTitle() != null) {
            session.setTitle(request.getTitle());
        }
        chatSessionService.updateById(session);
        return ResponseEntity.ok(Result.success(session));
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long id, Authentication auth) {
        ChatSession session = chatSessionService.getById(id);
        if (session == null) {
            return ResponseEntity.status(404).body(Result.notFound("会话不存在"));
        }
        if (!session.getUsername().equals(auth.getName())) {
            return ResponseEntity.status(403).body(Result.forbidden("无权查看此会话"));
        }
        List<ChatMessage> messages = chatMessageService.list(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, id)
                        .orderByAsc(ChatMessage::getCreateTime));
        return ResponseEntity.ok(Result.success(messages));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long id, Authentication auth) {
        ChatMessage msg = chatMessageService.getById(id);
        if (msg == null) {
            return ResponseEntity.status(404).body(Result.notFound("消息不存在"));
        }
        ChatSession session = chatSessionService.getById(msg.getSessionId());
        if (session == null || !session.getUsername().equals(auth.getName())) {
            return ResponseEntity.status(403).body(Result.forbidden("无权删除此消息"));
        }
        chatMessageService.removeById(id);

        // 原子更新会话消息计数
        chatSessionService.lambdaUpdate()
                .eq(ChatSession::getId, msg.getSessionId())
                .setSql("CASE WHEN message_count > 0 THEN message_count - 1 ELSE 0 END")
                .update();
        return ResponseEntity.ok(Result.success(Map.of("message", "删除成功")));
    }

    private String truncateTitle(String text) {
        if (text == null) return "新对话";
        String clean = text.replaceAll("\\s+", " ");
        return clean.length() > 50 ? clean.substring(0, 50) + "..." : clean;
    }
}
