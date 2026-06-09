-- chat_message 表添加 token 用量字段
ALTER TABLE chat_message
ADD COLUMN prompt_tokens INT DEFAULT 0 COMMENT '输入 tokens',
ADD COLUMN completion_tokens INT DEFAULT 0 COMMENT '输出 tokens',
ADD COLUMN total_tokens INT DEFAULT 0 COMMENT '总 tokens';

-- 更新已有消息的 token 计数为 0（默认值，无历史数据）
UPDATE chat_message SET prompt_tokens = 0, completion_tokens = 0, total_tokens = 0 WHERE total_tokens IS NULL;
