package com.example.aimanager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aimanager.entity.CloudAccount;
import com.example.aimanager.mapper.CloudAccountMapper;
import com.example.aimanager.util.AesUtil;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
public class CloudAccountService extends ServiceImpl<CloudAccountMapper, CloudAccount> {

    private final AesUtil aesUtil;

    public CloudAccountService(AesUtil aesUtil) {
        this.aesUtil = aesUtil;
    }

    @Override
    public boolean save(CloudAccount entity) {
        if ("master".equals(entity.getType())) {
            entity.setProvider("");
            entity.setAccessKey("");
            entity.setAccessSecret("");
            entity.setRegion("");
        } else {
            // sub account: encrypt secret
            if (entity.getAccessSecret() != null && !entity.getAccessSecret().isEmpty()) {
                entity.setAccessSecret(aesUtil.encrypt(entity.getAccessSecret()));
            }
        }
        return super.save(entity);
    }

    @Override
    public boolean updateById(CloudAccount entity) {
        if ("master".equals(entity.getType())) {
            entity.setProvider("");
            entity.setAccessKey("");
            entity.setAccessSecret("");
            entity.setRegion("");
        } else if (entity.getAccessSecret() != null && !entity.getAccessSecret().isEmpty()) {
            entity.setAccessSecret(aesUtil.encrypt(entity.getAccessSecret()));
        }
        return super.updateById(entity);
    }

    @Override
    public CloudAccount getById(Serializable id) {
        CloudAccount account = super.getById(id);
        decryptSecret(account);
        return account;
    }

    public CloudAccount getDecryptedById(Long id) {
        CloudAccount account = super.getById(id);
        if (account != null && account.getAccessSecret() != null) {
            account.setAccessSecret(aesUtil.decrypt(account.getAccessSecret()));
        }
        return account;
    }

    private void decryptSecret(CloudAccount account) {
        if (account != null && account.getAccessSecret() != null) {
            try {
                account.setAccessSecret(aesUtil.decrypt(account.getAccessSecret()));
            } catch (Exception e) {
                // 解密失败时不暴露密文
            }
        }
    }

    public List<CloudAccount> getMasters() {
        return lambdaQuery().isNull(CloudAccount::getParentId).list();
    }

    public List<CloudAccount> getSubsByParentId(Long parentId) {
        return lambdaQuery().eq(CloudAccount::getParentId, parentId).list();
    }

    public boolean hasSubAccounts(Long masterId) {
        return lambdaQuery().eq(CloudAccount::getParentId, masterId).count() > 0;
    }
}
