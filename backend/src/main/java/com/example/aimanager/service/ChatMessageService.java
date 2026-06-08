package com.example.aimanager.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.aimanager.entity.ChatMessage;
import com.example.aimanager.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageService extends ServiceImpl<ChatMessageMapper, ChatMessage> {
}
