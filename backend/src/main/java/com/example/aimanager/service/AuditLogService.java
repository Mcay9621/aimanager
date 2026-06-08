package com.example.aimanager.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aimanager.entity.AuditLog;
import com.example.aimanager.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService extends ServiceImpl<AuditLogMapper, AuditLog> {
}
