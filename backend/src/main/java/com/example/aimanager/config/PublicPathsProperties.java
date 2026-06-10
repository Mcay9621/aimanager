package com.example.aimanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.security")
public class PublicPathsProperties {

    private List<String> publicPaths = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/send-code",
            "/api/v1/auth/verify-code",
            "/api/v1/auth/refresh",
            "/api/v1/models/**",
            "/api/v1/chat",
            "/api/v1/dict/**"
    );

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }
}
