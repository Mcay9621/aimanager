package com.example.aimanager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aimanager.entity.Dict;
import com.example.aimanager.mapper.DictMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DictService extends ServiceImpl<DictMapper, Dict> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Dict> getItemsByType(String typeCode) {
        return this.list(new LambdaQueryWrapper<Dict>()
                .eq(Dict::getTypeCode, typeCode)
                .eq(Dict::getStatus, 1)
                .orderByAsc(Dict::getSortOrder));
    }

    public List<Map<String, Object>> getModelTypes() {
        return getItemsByType("model_type").stream().map(d -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("key", d.getItemKey());
            item.put("label", d.getItemValue());
            // 从 remark 解析额外属性
            if (d.getRemark() != null && !d.getRemark().isEmpty()) {
                try {
                    Map<String, Object> extra = objectMapper.readValue(d.getRemark(),
                            new TypeReference<Map<String, Object>>() {});
                    item.putAll(extra);
                } catch (Exception ignored) {}
            }
            return item;
        }).collect(Collectors.toList());
    }
}
