package com.example.aimanager.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aimanager.entity.VerifyCode;
import com.example.aimanager.mapper.VerifyCodeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VerifyCodeService extends ServiceImpl<VerifyCodeMapper, VerifyCode> {

    private static final int CODE_EXPIRE_MINUTES = 5;

    public String generateCode(String target, Integer type) {
        String code = RandomUtil.randomNumbers(6);
        VerifyCode verifyCode = new VerifyCode();
        verifyCode.setTarget(target);
        verifyCode.setCode(code);
        verifyCode.setType(type);
        verifyCode.setUsed(0);
        verifyCode.setExpireTime(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
        verifyCode.setCreateTime(LocalDateTime.now());
        this.save(verifyCode);
        return code;
    }

    public boolean verifyCode(String target, String code, Integer type) {
        VerifyCode verifyCode = this.getOne(new LambdaQueryWrapper<VerifyCode>()
                .eq(VerifyCode::getTarget, target)
                .eq(VerifyCode::getCode, code)
                .eq(VerifyCode::getType, type)
                .eq(VerifyCode::getUsed, 0)
                .gt(VerifyCode::getExpireTime, LocalDateTime.now()));
        if (verifyCode != null) {
            verifyCode.setUsed(1);
            this.updateById(verifyCode);
            return true;
        }
        return false;
    }
}
