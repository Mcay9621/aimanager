package com.example.aimanager.service.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.util.Map;

/**
 * 腾讯云使用自定义 endpoint（非标准 /chat/completions），
 * 复用 OpenAiCompatibleStrategy 但覆盖 endpoint 后缀为空。
 */
public class TencentStrategy extends OpenAiCompatibleStrategy {

    public TencentStrategy(HttpClient httpClient, ObjectMapper objectMapper) {
        super(httpClient, objectMapper, "", Map.of());
    }
}
