package com.example.aimanager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aimanager.entity.Dict;
import com.example.aimanager.mapper.DictMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DictService extends ServiceImpl<DictMapper, Dict> {

    public List<Dict> getItemsByType(String typeCode) {
        return this.list(new LambdaQueryWrapper<Dict>()
                .eq(Dict::getTypeCode, typeCode)
                .eq(Dict::getStatus, 1)
                .orderByAsc(Dict::getSortOrder));
    }
}
