package com.example.aimanager.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aimanager.entity.ChatSession;
import com.example.aimanager.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;

@Service
public class ChatSessionService extends ServiceImpl<ChatSessionMapper, ChatSession> {
}
