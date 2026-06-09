package com.example.aimanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aimanager.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("SELECT cs.model_id, cs.model_name, " +
            "SUM(COALESCE(cm.prompt_tokens, 0)) as prompt_tokens, " +
            "SUM(COALESCE(cm.completion_tokens, 0)) as completion_tokens, " +
            "SUM(COALESCE(cm.total_tokens, 0)) as total_tokens, " +
            "COUNT(DISTINCT cm.session_id) as session_count, " +
            "COUNT(cm.id) as message_count, " +
            "MAX(cm.create_time) as last_used " +
            "FROM chat_message cm " +
            "JOIN chat_session cs ON cm.session_id = cs.id " +
            "WHERE cs.model_id IS NOT NULL AND cs.deleted = 0 " +
            "GROUP BY cs.model_id, cs.model_name " +
            "ORDER BY total_tokens DESC")
    List<Map<String, Object>> selectModelUsageStats();

    @Select("SELECT DATE(cm.create_time) as date, " +
            "SUM(COALESCE(cm.prompt_tokens, 0)) as prompt_tokens, " +
            "SUM(COALESCE(cm.completion_tokens, 0)) as completion_tokens, " +
            "SUM(COALESCE(cm.total_tokens, 0)) as total_tokens, " +
            "COUNT(cm.id) as message_count " +
            "FROM chat_message cm " +
            "JOIN chat_session cs ON cm.session_id = cs.id " +
            "WHERE cm.role = 'assistant' " +
            "AND cm.create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "AND cs.deleted = 0 " +
            "GROUP BY DATE(cm.create_time) " +
            "ORDER BY date")
    List<Map<String, Object>> selectDailyUsage(@Param("days") int days);

    @Select("SELECT COALESCE(SUM(COALESCE(cm.total_tokens, 0)), 0) as total_tokens, " +
            "COUNT(cm.id) as total_messages, " +
            "COUNT(DISTINCT cm.session_id) as total_sessions " +
            "FROM chat_message cm " +
            "JOIN chat_session cs ON cm.session_id = cs.id " +
            "WHERE cs.deleted = 0")
    Map<String, Object> selectOverallUsage();

    @Select("SELECT DATE(cm.create_time) as date, " +
            "COUNT(cm.id) as api_calls, " +
            "SUM(COALESCE(cm.total_tokens, 0)) as total_tokens " +
            "FROM chat_message cm " +
            "JOIN chat_session cs ON cm.session_id = cs.id " +
            "WHERE cs.model_id IN (${modelIds}) " +
            "AND cm.role = 'assistant' " +
            "AND cm.create_time >= #{startDate} " +
            "AND cs.deleted = 0 " +
            "GROUP BY DATE(cm.create_time) " +
            "ORDER BY date")
    List<Map<String, Object>> selectModelDailyUsage(@Param("modelIds") String modelIds,
                                                     @Param("startDate") String startDate);
}
