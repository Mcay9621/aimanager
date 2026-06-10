package com.example.aimanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aimanager.entity.CloudAccount;
import com.example.aimanager.entity.CloudResource;
import com.example.aimanager.mapper.CloudResourceMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
public class CloudResourceService {

    private final Map<String, TableHandler> typeHandlers = new LinkedHashMap<>();
    private final Map<String, FieldExtractor> fieldExtractors = new LinkedHashMap<>();
    private final Map<String, FieldExtractor> detailFieldExtractors = new LinkedHashMap<>();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        private final CloudResourceMapper cloudResourceMapper;

    public CloudResourceService(CloudResourceMapper cloudResourceMapper) {
        this.cloudResourceMapper = cloudResourceMapper;
        initFieldExtractors();
    }

    private void register(String type, String resourceType) {
        Function<Long, List<?>> queryFn = accountId -> selectByAccountAndType(accountId, resourceType);
        Function<Long, Object> getByIdFn = id -> cloudResourceMapper.selectById(id);
        typeHandlers.put(type, new TableHandler(type, queryFn, getByIdFn));
    }

    private List<?> selectByAccountAndType(Long accountId, String resourceType) {
        QueryWrapper<CloudResource> qw = new QueryWrapper<>();
        qw.eq("account_id", accountId).eq("resource_type", resourceType);
        return cloudResourceMapper.selectList(qw);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<?> selectByAccount(BaseMapper mapper, Long accountId) {
        QueryWrapper<Object> qw = new QueryWrapper<>();
        qw.eq("account_id", accountId);
        return mapper.selectList(qw);
    }

    public List<Map<String, Object>> getResourcesByAccounts(List<CloudAccount> accounts, String providerFilter) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (CloudAccount account : accounts) {
            if (account.getStatus() != 1) continue;
            if (providerFilter != null && !providerFilter.isEmpty()
                    && !providerFilter.equals(account.getProvider())) continue;

            for (TableHandler handler : typeHandlers.values()) {
                List<?> entities = handler.queryFn.apply(account.getId());
                for (Object entity : entities) {
                    Map<String, Object> map = toMap((CloudResource) entity, handler.type);
                    if (map != null) {
                        map.put("accountAlias", account.getAliasName());
                        result.add(map);
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getResourceDetail(String compositeId) {
        int dash = compositeId.indexOf('-');
        if (dash < 0) return null;
        String type = compositeId.substring(0, dash);
        try {
            Long id = Long.parseLong(compositeId.substring(dash + 1));
            TableHandler handler = typeHandlers.get(type);
            if (handler == null) return null;
            Object entity = handler.getByIdFn.apply(id);
            if (entity == null) return null;
            return toDetailMap((CloudResource) entity, type);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ========== Map conversion ==========

    private void putCommon(Map<String, Object> map, CloudResource e, String type) {
        map.put("id", type + "-" + e.getId());
        map.put("resourceType", type);
        map.put("provider", e.getProvider());
        map.put("status", e.getStatus());
        map.put("region", e.getRegion());
        if (e.getCreateTime() != null) map.put("createTime", e.getCreateTime().format(DTF));
    }

    private String safeStr(Object v) { return v != null ? v.toString() : ""; }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(CloudResource entity, String type) {
        Map<String, Object> map = new LinkedHashMap<>();
        putCommon(map, entity, type);
        FieldExtractor extractor = fieldExtractors.get(type);
        if (extractor != null) {
            extractor.extract(map, entity);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toDetailMap(CloudResource entity, String type) {
        Map<String, Object> map = toMap(entity, type);
        if (map == null) return null;

        FieldExtractor extractor = detailFieldExtractors.get(type);
        if (extractor != null) {
            extractor.extract(map, entity);
        }
        return map;
    }

    private String fmt(LocalDateTime dt) {
        return dt != null ? dt.format(DTF) : "";
    }

    private record TableHandler(String type,
                                Function<Long, List<?>> queryFn,
                                Function<Long, Object> getByIdFn) {}

    @FunctionalInterface
    private interface FieldExtractor {
        void extract(Map<String, Object> map, CloudResource entity);
    }

    private void registerFieldExtractor(String type, BiConsumer<Map<String, Object>, CloudResource> listFields,
                                         BiConsumer<Map<String, Object>, CloudResource> detailFields) {
        fieldExtractors.put(type, (map, entity) -> listFields.accept(map, entity));
        if (detailFields != null) {
            detailFieldExtractors.put(type, (map, entity) -> detailFields.accept(map, entity));
        }
    }

    private void initFieldExtractors() {
        // Field extractors read type-specific data from the resource's name and extra JSON field.
        // Currently uses name from common fields. For full extraction, parse extra as JSON.
        for (String type : typeHandlers.keySet()) {
            fieldExtractors.put(type, (map, r) -> {
                map.put("name", r.getName() != null ? r.getName() : "");
            });
        }
    }
}
