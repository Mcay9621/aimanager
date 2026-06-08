# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI Manager 是一个基于 Spring Boot + Vue 3 的 AI 模型管理平台，支持多 AI 提供商（OpenAI、Anthropic、阿里、百度、字节、腾讯）的 API 统一管理和在线对话。

## Commands

### Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run     # 启动后端服务 (端口 8080), 需要 Java 17
mvn clean package       # 构建 JAR 包
mvn test                # 运行测试
```

需要配置 Java 17 路径:
```bash
set JAVA_HOME=E:\soft\jdk-17.0.11+9
```

### Frontend (Vue 3)

```bash
cd frontend
npm install             # 安装依赖
npm run dev            # 启动开发服务器 (端口 5173)
npm run build          # 构建生产版本
npm run preview        # 预览构建结果
```

### Database

```bash
mysql -u root -p < sql/init.sql
```

默认管理员账号: `admin` / `admin123`

## Architecture

### Backend (Spring Boot)

```
backend/src/main/java/com/example/aimanager/
├── common/
│   └── Result.java              # 统一响应封装 (code/message/data)
├── config/
│   ├── SecurityConfig.java      # Spring Security 配置 + 公开路径 permitAll
│   ├── JwtAuthenticationFilter.java # JWT 过滤器 (OncePerRequestFilter)
│   └── BeanConfig.java          # PasswordEncoder (BCrypt)
├── controller/
│   ├── AuthController.java      # 登录/注册/验证码/用户信息
│   ├── AdminController.java     # 角色 CRUD (admin)
│   ├── UserController.java      # 用户 CRUD + 状态切换 (admin)
│   ├── ModelController.java     # AI 模型 CRUD + 公开模型列表
│   ├── ChatController.java      # AI 对话 + 会话管理 + SSE 流式接口
│   ├── ProfileController.java   # 个人中心 (修改个人信息/密码)
│   ├── DashboardController.java # 仪表盘统计
│   └── AuditLogController.java  # 审计日志查询
├── entity/                      # 实体 + MyBatis Plus @TableName
│   ├── User.java                # sys_user, @TableLogic deleted, @JsonIgnore password
│   ├── Role.java / Permission.java / UserRole.java / RolePermission.java
│   ├── AiModel.java             # ai_model, @JsonIgnore apiKey
│   ├── VerifyCode.java          # sys_verify_code
│   ├── AuditLog.java            # sys_audit_log
│   ├── ChatSession.java         # chat_session 聊天会话
│   └── ChatMessage.java         # chat_message 聊天消息
├── mapper/                      # MyBatis Plus Mapper 接口
├── service/
│   ├── UserService.java         # 实现 UserDetailsService, 含注册/用户名邮箱查重
│   ├── RoleService.java         # 基础 CRUD
│   ├── AiModelService.java      # getEnabledModels() 查询启用模型
│   ├── AiChatService.java       # 多提供商 API 调用, 流式 SSE 支持, 响应解析
│   ├── AuditLogService.java     # 审计日志 CRUD
│   ├── ChatSessionService.java  # 聊天会话 CRUD
│   ├── ChatMessageService.java  # 聊天消息 CRUD
│   └── VerifyCodeService.java   # 6位数字验证码, 5分钟过期
└── util/
    └── JwtUtil.java             # JJWT 0.12.3, HMAC-SHA, 24h过期
```

**依赖**: Spring Boot 3.2.0 + MyBatis Plus 3.5.5 + JJWT 0.12.3 + MySQL + Hutool 5.8.22 + Lombok

### Frontend (Vue 3)

```
frontend/src/
├── main.js               # 导入 Element Plus + 全部图标 + Pinia + Router
├── App.vue
├── router/index.js       # 路由 + 导航守卫 (auth/admin 检查)
├── stores/user.js        # Pinia store: token/userInfo/isAdmin, fetchUserInfo(), logout()
├── utils/request.js      # axios 封装: baseURL=/api, 自动带 Authorization, 错误 ElMessage
└── views/
    ├── auth/
    │   ├── Login.vue         # 前台登录 (科技感背景 + 磨砂玻璃卡片)
    │   ├── AdminLogin.vue    # 后台登录 (深色渐变 + 金色主题)
    │   └── Register.vue
    ├── layout/
    │   └── MainLayout.vue    # 深色侧边栏 + 明亮内容区 (含对话历史/个人中心导航)
    ├── front/
    │   ├── ModelList.vue     # 公开模型列表页 (点击使用跳转独立聊天页)
    │   └── ChatView.vue      # 流式 AI 对话页 (左侧会话列表 + 右侧 SSE 流式聊天)
    ├── user/
    │   └── UserProfile.vue   # 个人中心 (修改邮箱/手机 + 修改密码)
    └── admin/
        ├── Dashboard.vue     # 仪表盘 (统计卡片 + 快捷入口)
        ├── UserManage.vue    # 用户 CRUD + 启用/禁用 + 角色分配
        ├── RoleManage.vue    # 角色 CRUD
        ├── ModelManage.vue   # 模型 CRUD + 启用/禁用
        └── AuditLogManage.vue # 审计日志列表 + 筛选
```

**依赖**: Vue 3.4 + Vite 5 + Pinia + Element Plus 2.5 + Axios + Vue Router 4

### Two-Login Design

系统有独立的前台和后台登录页面:
- `/login` → 前台登录 (普通用户), 跳转 `/front/models`
- `/admin/login` → 后台登录 (需管理员角色), 跳转 `/admin/users`
- AuthController.login() 根据 `loginType` 参数 (`front`/`admin`) 区分, 后台登录校验 `ROLE_ADMIN` 或 `ROLE_SUPER_ADMIN`
- 前端路由守卫根据 `token` 和 `userStore.isAdmin` 决定跳转逻辑

### AI Chat Flow

**同步模式 (向后兼容):** `POST /api/chat` → `ChatController.chat()` → `AiChatService.chat()` 根据 `model.type` 分发:

**流式 SSE 模式:** `POST /api/chat/stream` → `ChatController.streamChat()` → `AiChatService.chatStream()` 使用 `SseEmitter` + `CompletableFuture` 异步流式调用:
  - 请求体: `{modelId, message, sessionId?}`
  - SSE 事件: `event: sessionId` → `event: token` (逐 token) → `event: done`
  - 自动创建/复用会话，保存用户消息和助手消息到数据库
  - 前端使用 `fetch()` + `ReadableStream` 解析 SSE 事件流

**会话管理:** `GET/POST/DELETE /api/chat/sessions` 和 `GET /api/chat/sessions/{id}/messages`：
  - 每个会话关联模型 + 所属用户
  - 会话标题自动从首条消息截取 (50字)
  - 删除会话同时删除所有关联消息

| type | API 格式 | 认证头 |
|------|----------|--------|
| openai | /chat/completions | Bearer token |
| anthropic | /v1/messages | x-api-key |
| ali | /chat/completions | Bearer token |
| baidu | /chat/completions | Bearer token |
| byte | /chat/completions | Bearer token |
| tencent | 自定义 endpoint | Bearer token |

响应解析: OpenAI/阿里/百度/字节/腾讯从 `choices[0].message.content` 提取, Anthropic 从 `content[0].text` 提取。

### Database

- `sys_user` + `sys_role` + `sys_permission` + `sys_user_role` + `sys_role_permission` — RBAC 五表
- `sys_verify_code` — 验证码 (类型 1=邮箱 2=手机, 5分钟过期)
- `ai_model` — AI 模型配置 (含 api_key、endpoint、type、enabled 状态)
- 所有业务表使用 `deleted` 字段实现 MyBatis Plus 逻辑删除

## Key Patterns

- **JWT 认证**: 请求头 `Authorization: Bearer <token>`。`JwtAuthenticationFilter` 是 OncePerRequestFilter, 对公开路径 (login/register/send-code/verify-code/models/chat/sessions*) 直接放行, 其余路径解析 JWT 设置 SecurityContext
  - `/api/chat/sessions/**` 和 `/api/chat/stream` 需要认证
  - `/api/user/**` 需要认证
  - `/api/admin/**` 需要 ADMIN 或 SUPER_ADMIN 角色
- **RBAC**: SecurityConfig 对 `/api/admin/**` 要求 `ROLE_ADMIN` 或 `ROLE_SUPER_ADMIN`。Controller 层额外使用 `@PreAuthorize`。`UserService.loadUserByUsername()` 查询用户角色, 无角色默认赋予 `ROLE_USER`
- **统一响应格式**: 所有 Controller 统一返回 `ResponseEntity<Result<T>>`。`Result<T>` 提供 `success()/error()/badRequest()/unauthorized()/forbidden()/notFound()` 静态工厂。成功时 `code=200`、`message="success"`、`data` 为实际负载。前端 `request.js` 的响应拦截器自动解包 `Result.data`，业务代码直接使用数据无需感知 Result 包装
- **前端请求**: `request.js` 基于 axios, baseURL = `/api`, 自动附加 token, 错误时 ElMessage 提示。Vite 代理 `/api` → `localhost:8080`
- **后端 API 使用 `Map<String, Object>`** 作为请求体/响应体, 无统一 DTO 类

## Personal Center

`GET/PUT /api/user/profile` 获取/更新个人信息 (email, phone），`PUT /api/user/password` 修改密码（需验证旧密码）。
前端路径 `/user/profile`，点击顶部用户头像进入。需要认证 (`/api/user/**` 在 SecurityConfig 中配置为 `.authenticated()`)。
**注**: 当前用户无 nickname/avatar 字段，仅支持修改邮箱和手机号。

## Known Issues

- **Register 端点可能返回 401**: `JwtAuthenticationFilter` 使用 `startsWith` 匹配 `/api/auth/register`, 但命名可能产生意外误匹配。如遇注册失败, 检查 filter 和 SecurityConfig 中的路径配置
- **SSE 超时**: `SseEmitter(120_000L)` 设为 2 分钟超时，长时间回复可能超时断开
- **Anthropic SSE 兼容性**: Anthropic 流式格式与其他厂商不同，需验证 `content_block_delta` 事件格式
- **验证码仅输出到控制台**: `AuthController.sendCode()` 仅日志输出, 无实际邮件/短信发送
