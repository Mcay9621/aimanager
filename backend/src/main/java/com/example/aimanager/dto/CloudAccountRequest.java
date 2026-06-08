package com.example.aimanager.dto;

import jakarta.validation.constraints.NotBlank;

public class CloudAccountRequest {
    private String provider;

    @NotBlank(message = "账号别名不能为空")
    private String aliasName;

    private String accessKey;
    private String accessSecret;
    private String region;
    private Long parentId;
    private String type;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getAccessSecret() { return accessSecret; }
    public void setAccessSecret(String accessSecret) { this.accessSecret = accessSecret; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
