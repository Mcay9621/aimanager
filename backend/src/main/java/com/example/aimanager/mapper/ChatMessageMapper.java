package com.example.aimanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aimanager.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
