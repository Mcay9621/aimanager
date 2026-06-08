package com.example.aimanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StreamChatRequest {
    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private Long sessionId;

    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
}
