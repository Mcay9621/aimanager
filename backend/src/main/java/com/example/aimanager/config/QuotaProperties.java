package com.example.aimanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.quota")
public class QuotaProperties {

    private int defaultDailyLimit = 20;
    private Map<String, Integer> roleLimits;

    public int getDefaultDailyLimit() {
        return defaultDailyLimit;
    }

    public void setDefaultDailyLimit(int defaultDailyLimit) {
        this.defaultDailyLimit = defaultDailyLimit;
    }

    public Map<String, Integer> getRoleLimits() {
        return roleLimits;
    }

    public void setRoleLimits(Map<String, Integer> roleLimits) {
        this.roleLimits = roleLimits;
    }
}
