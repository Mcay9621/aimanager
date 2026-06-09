package com.example.aimanager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aimanager.entity.AiModel;
import com.example.aimanager.mapper.AiModelMapper;
import com.example.aimanager.util.AesUtil;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
public class AiModelService extends ServiceImpl<AiModelMapper, AiModel> {

    private final AesUtil aesUtil;

    public AiModelService(AesUtil aesUtil) {
        this.aesUtil = aesUtil;
    }

    public List<AiModel> getEnabledModels() {
        return this.list(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getEnabled, 1));
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
        return super.save(entity);
    }

    @Override
    public boolean updateById(AiModel entity) {
        if (entity.getApiKey() != null) {
            entity.setApiKey(aesUtil.encrypt(entity.getApiKey()));
        }
        return super.updateById(entity);
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
