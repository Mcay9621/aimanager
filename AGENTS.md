# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

# Project Overview

AI Manager 是一个基于 Spring Boot + Vue 3 的 AI 模型管理平台，支持多 AI 提供商（OpenAI、Anthropic、阿里、百度、字节、腾讯、DeepSeek）的 API 统一管理、在线对话和多云资源管理。

# Workflow Rules

## 1. Plan-First 原则

所有对话优先出一版方案，等用户确认后再执行。禁止在未经确认的情况下直接修改代码（紧急修复除外，需说明）。

- 收到需求后，先分析影响范围
- 输出实施方案（文件变更、风险点、回退策略）
- 等待确认后再动手

## 2. 通信规范

- 思考和回答使用中文
- 代码中的注释使用英文
- 每次修改说明做了什么、为什么
- 编译/验证通过后再声称完成，不提前断言

## 3. 代码安全

**提交时务必处理以下信息，不得以明文形式出现在 git log / commit message / 远程仓库中：**

- 数据库密码（DB_PASSWORD）
- JWT 密钥（JWT_SECRET）
- API 密钥（API_KEY_SECRET）
- AI 模型 API Key（各厂商的 apiKey）
- 云厂商 AccessKey / AccessSecret
- SSH 密码、服务器 IP

**处理方式：**
- 使用环境变量读取（现有项目已使用 @Value 和  模式）
- .env.example 可保留模板，但填充实际值后必须加入 .gitignore
- 部署配置使用 Nacos 配置中心（Data ID: ai-manager.yaml），不在代码中硬编码
- git commit 前检查是否有密钥泄露

## 4. 代码风格

- 遵守项目已有的风格（Lombok + MyBatis Plus 优先，Hutool 工具类优先）
- 不引入新的框架或依赖，除非有明确必要性并经确认
- 类名、方法名、变量名使用英文
- 简单优先：能用 Map<String, Object> 就不用新建 DTO 类
- 匹配现有模式：不单方面重构代码风格
- 不做超前抽象：直到有明显重复才提取共同逻辑

## 5. 变更边界

- 不改动未在方案范围内且未要求修改的文件
- 不改动用户未要求的现有行为
- 涉及删除文件前确认没有被其他代码引用
- 批量文件操作（如删除 39 个实体类）前说明数量级和影响

## 6. Git 工作流

代码重构或功能开发时，按以下流程执行：

1. **拉取最新代码**: git pull --rebase 确保基于最新 master
2. **创建分支**: git checkout -b codex/<feature-name>
3. **修改代码**: 按方案执行变更
4. **提交前确认**: 代码无误（编译通过、功能正常）且用户确认后，提交分支内容
5. **合并到 master**: 提交后合并回 master 分支

禁止直接在 master 分支上修改代码。

## 7. 验证

- 后端改动后必须运行 mvn compile 确认编译通过
- 前端改动后目测检查语法正确性
- 大规模重构前说明风险和回退策略

# Environment

## Java 17 (本地开发)

`ash
set JAVA_HOME=E:\soft\jdk-17.0.11+9
mvn spring-boot:run -f backend/pom.xml
`

PowerShell:

`powershell
C:\Program Files\Java\jdk-9.0.4="E:\soft\jdk-17.0.11+9"
cd backend
mvn spring-boot:run
`

## Frontend

`ash
cd frontend
npm install
npm run dev
`

## Database

`ash
mysql -u root -p < sql/init.sql
`

默认管理员: admin / admin123

# Git Repositories

项目关联两个远程仓库，提交时分别推送：

## Gitee

- 地址: git@gitee.com:Mcay/ai-manager.git
- 远程名: gitee

## GitHub

- 地址: git@github.com:Mcay9621/aimanager.git
- 远程名: github

## 提交命令参考

`ash
git add .
git commit -m "feat: description"
git push gitee master
git push github master
`

# Server

地址:  122.51.136.129
端口:  22
用户:  root
SSH:   ssh -i ~/.ssh/id_rsa root@122.51.136.129

## MySQL

地址:  localhost:3306
数据库: ai_manager
用户:  root
JDBC URL: jdbc:mysql://localhost:3306/ai_manager?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
环境变量: DB_URL, DB_USERNAME=root

## Redis

地址:  127.0.0.1:6379

## Nacos

控制台: http://122.51.136.129:8848/nacos
用户: nacos
Data ID: ai-manager.yaml

# Known Issues

- Register 端点返回 401: 检查 JwtAuthenticationFilter 和 SecurityConfig 中的路径配置
- SSE 超时: SseEmitter 设为 2 分钟超时，长时间回复可能超时断开
- Anthropic SSE 兼容性: content_block_delta 事件格式与其他厂商不同
- 验证码仅输出到控制台: 无实际邮件/短信发送
