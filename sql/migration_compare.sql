-- AI 对话多模型对比功能 数据库迁移
-- 运行方式: mysql -u root -p ai_manager < sql/migration_compare.sql

ALTER TABLE chat_session
  ADD COLUMN session_type VARCHAR(20) NOT NULL DEFAULT 'single'
  COMMENT '会话类型: single-单模型对话, compare-多模型对比'
  AFTER username;

ALTER TABLE chat_message
  ADD COLUMN model_name VARCHAR(100) DEFAULT NULL
  COMMENT '对比模式下对应的模型名称'
  AFTER content;
