package com.example.aimanager.service.cloud;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CloudClientRegistry {

    private final Map<String, CloudClient> clientMap = new ConcurrentHashMap<>();

    public CloudClientRegistry(List<CloudClient> clients) {
        clients.forEach(c -> clientMap.put(c.getClass().getSimpleName(), c));
    }

    public CloudClient getClient(String provider) {
        return clientMap.values().stream()
                .filter(c -> c.supports(provider))
                .findFirst()
                .orElse(null);
    }
}
