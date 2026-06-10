package com.example.aimanager.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        String clientIp = getClientIp(req);

        int limit = switch (path) {
            case "/api/auth/login" -> 5;
            case "/api/auth/send-code" -> 3;
            case "/api/auth/register" -> 3;
            case "/api/chat/stream" -> 30;
            default -> -1;
        };

        if (limit > 0) {
            String key = path + ":" + clientIp;
            Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(limit));
            if (!bucket.tryConsume()) {
                res.setStatus(429);
                res.setContentType("application/json;charset=UTF-8");
                com.example.aimanager.common.Result<Void> rateLimitResult = com.example.aimanager.common.Result.error(429, "Request rate limited, please try again later");
                com.fasterxml.jackson.databind.ObjectMapper rateLimitMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                res.setContentType("application/json;charset=UTF-8");
                rateLimitMapper.writeValue(res.getOutputStream(), rateLimitResult);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private static class Bucket {
        private final int limit;
        private final AtomicInteger counter = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        Bucket(int limit) { this.limit = limit; }

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60_000) {
                counter.set(0);
                windowStart = now;
            }
            return counter.incrementAndGet() <= limit;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}
