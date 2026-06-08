package com.example.aimanager.dto;

import jakarta.validation.constraints.NotNull;

public class CreateSessionRequest {
    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    private String title;

    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
