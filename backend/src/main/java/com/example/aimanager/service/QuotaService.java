package com.example.aimanager.service;

import com.example.aimanager.config.QuotaProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class QuotaService {

    private static final String KEY_PREFIX = "user:daily_msgs:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final QuotaProperties quotaProperties;
    private final UserService userService;

    public QuotaService(RedisTemplate<String, Object> redisTemplate,
                        QuotaProperties quotaProperties,
                        UserService userService) {
        this.redisTemplate = redisTemplate;
        this.quotaProperties = quotaProperties;
        this.userService = userService;
    }

    private String buildKey(Long userId) {
        return KEY_PREFIX + LocalDate.now() + ":" + userId;
    }

    /**
     * 检查并扣减配额
     * @param userId 用户 ID
     * @param cost 消耗次数（普通消息=1，对比=N个模型）
     * @return true=允许, false=超过配额
     */
    public boolean checkAndIncrement(Long userId, int cost) {
        int limit = getDailyLimit(userId);
        if (limit >= 999999) return true; // 管理员无限制

        String key = buildKey(userId);
        Integer used = (Integer) redisTemplate.opsForValue().get(key);
        if (used == null) {
            // 首次使用，设置 TTL 到当天结束
            long secondsUntilEnd = Duration.between(LocalTime.now(), LocalTime.MAX).getSeconds();
            redisTemplate.opsForValue().set(key, cost, secondsUntilEnd, TimeUnit.SECONDS);
            return cost <= limit;
        }
        if (used + cost > limit) return false;
        redisTemplate.opsForValue().increment(key, cost);
        return true;
    }

    /**
     * 获取用户当日剩余配额
     */
    public Map<String, Object> getRemaining(Long userId) {
        int limit = getDailyLimit(userId);
        String key = buildKey(userId);
        Integer used = (Integer) redisTemplate.opsForValue().get(key);
        int usedVal = used != null ? used : 0;
        Map<String, Object> result = new HashMap<>();
        result.put("used", usedVal);
        result.put("limit", limit);
        result.put("remaining", Math.max(0, limit - usedVal));
        return result;
    }

    /**
     * 获取用户每日上限（按最高角色）
     */
    public int getDailyLimit(Long userId) {
        List<String> roleCodes = userService.getUserRoleCodes(userId);
        Map<String, Integer> roleLimits = quotaProperties.getRoleLimits();
        int limit = quotaProperties.getDefaultDailyLimit();
        for (String role : roleCodes) {
            if (roleLimits != null && roleLimits.containsKey(role)) {
                int roleLimit = roleLimits.get(role);
                if (roleLimit > limit) limit = roleLimit;
            }
        }
        return limit;
    }
}
