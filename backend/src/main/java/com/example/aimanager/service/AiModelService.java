package com.example.aimanager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aimanager.entity.AiModel;
import com.example.aimanager.mapper.AiModelMapper;
import com.example.aimanager.util.AesUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AiModelService extends ServiceImpl<AiModelMapper, AiModel> {

    private final AesUtil aesUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY = "ai:models:enabled";
    private static final long CACHE_TTL = 60;

    public AiModelService(AesUtil aesUtil, RedisTemplate<String, Object> redisTemplate) {
        this.aesUtil = aesUtil;
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public List<AiModel> getEnabledModels() {
        List<AiModel> cached = (List<AiModel>) redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            return cached;
        }
        List<AiModel> models = this.list(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getEnabled, 1));
        if (models != null && !models.isEmpty()) {
            redisTemplate.opsForValue().set(CACHE_KEY, models, CACHE_TTL, TimeUnit.SECONDS);
        }
        return models;
    }

    public List<AiModel> listWithDecryptedKeys() {
        List<AiModel> models = this.list();
        models.forEach(this::decryptApiKey);
        return models;
    }

    @Override
    public boolean save(AiModel entity) {
        if (entity.getApiKey() != null) {
            entity.setApiKey(aesUtil.encrypt(entity.getApiKey()));
        }
        boolean saved = super.save(entity);
        if (saved) {
            redisTemplate.delete(CACHE_KEY);
        }
        return saved;
    }

    @Override
    public boolean updateById(AiModel entity) {
        if (entity.getApiKey() != null) {
            entity.setApiKey(aesUtil.encrypt(entity.getApiKey()));
        }
        boolean updated = super.updateById(entity);
        if (updated) {
            redisTemplate.delete(CACHE_KEY);
        }
        return updated;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean removed = super.removeById(id);
        if (removed) {
            redisTemplate.delete(CACHE_KEY);
        }
        return removed;
    }

    @Override
    public AiModel getById(Serializable id) {
        AiModel model = super.getById(id);
        decryptApiKey(model);
        return model;
    }

    private void decryptApiKey(AiModel model) {
        if (model == null || model.getApiKey() == null || model.getApiKey().isEmpty()) return;
        try {
            model.setApiKey(aesUtil.decrypt(model.getApiKey()));
        } catch (Exception e) {
            // 非加密数据（空密钥或明文），保持原样
        }
    }
}
