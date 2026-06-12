package com.example.aimanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CompareChatRequest {
    @NotBlank(message = "消息内容不能为空")
    private String message;

    @Size(min = 2, max = 4, message = "请选择 2-4 个模型进行对比")
    private List<Long> modelIds;

    private Long sessionId;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<Long> getModelIds() { return modelIds; }
    public void setModelIds(List<Long> modelIds) { this.modelIds = modelIds; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
}
