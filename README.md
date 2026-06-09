# AI Manager - 大模型管理平台

一个包含登录、前后台、角色管理和大模型管理的全栈 Web 应用。

## 技术栈

- **前端**：Vue 3 + Element Plus + Pinia + Vite
- **后端**：Spring Boot 3 + MyBatis-Plus + MySQL + Redis
- **缓存**：Redis（模型连通性状态缓存，60s TTL）
- **认证**：JWT + Spring Security
- **服务发现**：Nacos（可选）

## 项目结构

```
project-root/
├── backend/                    # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/
│   │   ├── java/com/example/aimanager/
│   │   │   ├── AiManagerApplication.java    # 启动类
│   │   │   ├── config/                       # 配置类
│   │   │   │   ├── SecurityConfig.java       # Spring Security配置
│   │   │   │   ├── JwtAuthenticationFilter.java # JWT过滤器
│   │   │   │   ├── BeanConfig.java           # Bean配置
│   │   │   │   ├── RedisConfig.java          # Redis/Jedis配置
│   │   │   │   └── AuditLogAspect.java       # 审计日志AOP切面
│   │   │   ├── controller/                   # 控制器
│   │   │   │   ├── AuthController.java       # 认证接口
│   │   │   │   ├── AdminController.java      # 后台管理接口(角色管理)
│   │   │   │   ├── ModelController.java      # 模型接口
│   │   │   │   ├── UserController.java       # 用户管理+角色分配
│   │   │   │   ├── ChatController.java       # AI对话+会话管理+SSE流式接口
│   │   │   │   ├── ProfileController.java    # 个人中心(修改信息/密码)
│   │   │   │   ├── DashboardController.java  # 仪表盘统计
│   │   │   │   └── AuditLogController.java   # 审计日志查询
│   │   │   ├── entity/                       # 实体类
│   │   │   │   ├── User.java                 # 用户
│   │   │   │   ├── Role.java                 # 角色
│   │   │   │   ├── Permission.java           # 权限
│   │   │   │   ├── AiModel.java              # AI模型
│   │   │   │   ├── UserRole.java             # 用户角色关联
│   │   │   │   ├── RolePermission.java       # 角色权限关联
│   │   │   │   ├── VerifyCode.java           # 验证码
│   │   │   │   ├── AuditLog.java             # 审计日志
│   │   │   │   ├── ChatSession.java          # 聊天会话
│   │   │   │   └── ChatMessage.java          # 聊天消息
│   │   │   ├── service/                      # 服务层
│   │   │   │   ├── UserService.java          # 用户+UserDetailsService
│   │   │   │   ├── RoleService.java          # 角色CRUD
│   │   │   │   ├── AiModelService.java       # 模型服务
│   │   │   │   ├── ModelStatusService.java   # 模型连通性状态缓存(Redis)
│   │   │   │   ├── AiChatService.java        # AI对话(多提供商+SSE流式)
│   │   │   │   ├── ChatSessionService.java   # 聊天会话
│   │   │   │   ├── ChatMessageService.java   # 聊天消息
│   │   │   │   ├── AuditLogService.java      # 审计日志
│   │   │   │   └── VerifyCodeService.java    # 验证码
│   │   │   ├── mapper/                       # MyBatis映射
│   │   │   └── util/                         # 工具类
│   │   │       └── JwtUtil.java              # JWT工具
│   │   └── resources/
│   │       ├── application.yml              # 配置文件
│   │       └── bootstrap.yml                # Nacos引导配置
├── frontend/                   # Vue3 前端
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── router/
│       │   └── index.js                      # 路由配置
│       ├── stores/
│       │   └── user.js                       # 用户状态管理
│       ├── utils/
│       │   └── request.js                    # axios封装
│       └── views/
│           ├── auth/
│           │   ├── Login.vue                  # 登录页
│           │   └── Register.vue              # 注册页
│           ├── layout/
│           │   └── MainLayout.vue            # 主布局
│           ├── front/
│           │   ├── ModelList.vue             # 模型列表(前台)→跳转独立聊天页
│           │   └── ChatView.vue              # AI对话(SSE流式+会话管理)
│           ├── user/
│           │   └── UserProfile.vue           # 个人中心(修改信息/密码)
│           └── admin/
│               ├── UserManage.vue            # 用户管理(含角色分配)
│               ├── RoleManage.vue            # 角色管理
│               ├── ModelManage.vue           # 模型管理(后台)
│               ├── Dashboard.vue             # 仪表盘
│               └── AuditLogManage.vue        # 审计日志
└── sql/
    └── init.sql                               # 数据库脚本
```

## 功能说明

### 1. 登录认证
- 账户密码登录
- 用户注册
- 验证码功能（暂模拟发送至控制台）
- JWT Token 认证

### 2. 前台功能
- **AI模型列表** (`/front/models`)：查看可用的大模型，包括：
  - GPT-4o / GPT-4o-mini (OpenAI)
  - Claude 3.5 Sonnet / Claude 3 Haiku (Anthropic)
  - 通义千问 (阿里云)
  - 文心一言 (百度)
  - 豆包 (字节跳动)
  - 腾讯混元 (腾讯)
  - DeepSeek Chat / DeepSeek Reasoner (DeepSeek)
  - 模型连通性状态（在线/离线/未检测），缓存至 Redis，60s TTL
  - 一键刷新状态按钮
- **AI对话** (`/front/chat`)：SSE 流式 AI 对话，功能包括：
  - 左侧会话历史列表（支持搜索、删除）
  - 右侧流式聊天区域，逐 token 实时显示回复
  - Markdown 简单渲染（代码块、加粗等）
  - 多轮对话自动保存，刷新不丢失
- **个人中心** (`/user/profile`)：修改邮箱、手机号和密码

### 3. 后台管理
- **仪表盘** (`/admin`)：综合数据统计看板，展示：
  - 用户总数、模型总数、启用模型数
  - 审计日志总量、今日操作次数
  - 快捷入口链接至各管理模块
- **用户管理** (`/admin/users`)：查看用户列表、禁用/启用用户、**分配角色**
- **角色管理** (`/admin/roles`)：角色的增删改查
- **模型管理** (`/admin/models`)：配置 AI 模型，包括：
  - 模型名称、类型（OpenAI/Anthropic/阿里云/百度/字节/腾讯/DeepSeek）
  - API 地址和密钥、模型标识、启用/禁用状态
  - 连通性检测（在线/离线/未检测）
  - 批量刷新连通性、单模型测试
  - 用量统计（对话次数、Token 数、余额查询）
  - 从 OpenAI 兼容 API 同步模型列表
- **审计日志** (`/admin/audit-logs`)：操作审计记录，支持按操作类型和对象类型筛选，记录内容包括：
  - 操作人、操作类型（创建/更新/删除/登录）
  - 操作对象、对象ID、详情
  - IP 地址、操作时间

### 4. 角色权限
- 超级管理员 (SUPER_ADMIN)：拥有所有权限
- 普通管理员 (ADMIN)：除用户管理和角色管理外的权限
- 普通用户 (USER)：前台操作权限

## 启动步骤

### 1. 数据库配置

```bash
# 创建数据库和表
mysql -u root -p < sql/init.sql

# 或在 MySQL 客户端执行 sql/init.sql 内容
```

### 2. Redis 配置（可选，用于模型连通性缓存）

```bash
# 安装 Redis
apt install redis-server  # 或 yum install redis

# 修改密码（可选）
redis-cli CONFIG SET requirepass your_password

# 启动 Redis
systemctl start redis
```

如果不配置 Redis，模型列表和状态功能仍可正常工作，只是可用性状态显示为"未检测"。

### 3. 后端启动

```bash
cd backend

# 使用 Maven 启动
mvn spring-boot:run
```

后端默认端口：`http://localhost:8080`

**注意**：需要 Java 17 环境，配置 JDK 路径：
```bash
set JAVA_HOME=E:\soft\jdk-17.0.11+9
```

### 4. 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端默认端口：`http://localhost:5173`（如被占用会自动切换）

### 5. 访问系统

1. 打开浏览器访问 `http://localhost:5173`
2. 使用默认管理员账号登录：
   - 用户名：`admin`
   - 密码：`admin123`

## API 接口

### 认证接口
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/auth/login | 登录 | 否 |
| POST | /api/auth/register | 注册 | 否 |
| POST | /api/auth/send-code | 发送验证码 | 否 |
| POST | /api/auth/verify-code | 验证验证码 | 否 |
| GET | /api/auth/info | 获取用户信息 | 是 |
| POST | /api/auth/logout | 登出 | 是 |

### 模型接口
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/models | 获取可用模型列表（含 Redis 缓存状态） | 否 |
| POST | /api/models/refresh | 批量刷新所有模型连通性（结果写入 Redis） | 否 |
| GET | /api/admin/models | 获取所有模型（含缓存状态） | 是(管理员) |
| POST | /api/admin/models | 添加模型 | 是(管理员) |
| PUT | /api/admin/models/{id} | 更新模型 | 是(管理员) |
| DELETE | /api/admin/models/{id} | 删除模型 | 是(管理员) |
| POST | /api/admin/models/{id}/test | 测试单个模型连通性 | 是(管理员) |
| POST | /api/admin/models/fetch | 从 OpenAI 兼容 API 同步模型列表 | 是(管理员) |
| GET | /api/admin/models/balances | 模型用量统计+余额查询 | 是(管理员) |
| GET | /api/admin/models/usage | 整体用量+按天趋势+模型排行 | 是(管理员) |
| GET | /api/admin/models/usage/deepseek | DeepSeek 用量明细 | 是(管理员) |

### 用户管理接口
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/admin/users | 用户列表 | 是(管理员) |
| GET | /api/admin/users/{id} | 获取单个用户 | 是(管理员) |
| POST | /api/admin/users | 添加用户 | 是(管理员) |
| PUT | /api/admin/users/{id} | 更新用户 | 是(管理员) |
| DELETE | /api/admin/users/{id} | 删除用户 | 是(管理员) |
| PUT | /api/admin/users/{id}/status | 切换启用/禁用 | 是(管理员) |
| GET | /api/admin/users/{id}/roles | 获取用户角色 | 是(管理员) |
| PUT | /api/admin/users/{id}/roles | 分配用户角色 | 是(管理员) |

### 角色管理接口
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/admin/roles | 角色列表 | 是(管理员) |
| POST | /api/admin/roles | 添加角色 | 是(管理员) |
| PUT | /api/admin/roles/{id} | 更新角色 | 是(管理员) |
| DELETE | /api/admin/roles/{id} | 删除角色 | 是(管理员) |

### 仪表盘接口
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/admin/dashboard/stats | 获取统计数据 | 是(管理员) |

### 审计日志接口
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/admin/audit-logs | 审计日志列表(支持?action=&target=筛选) | 是(管理员) |

### 聊天对话接口
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/chat | 同步聊天（向后兼容） | 否 |
| POST | /api/chat/stream | SSE 流式聊天（逐 token 返回） | 是 |
| GET | /api/chat/sessions | 获取当前用户会话列表 | 是 |
| POST | /api/chat/sessions | 创建新会话 | 是 |
| DELETE | /api/chat/sessions/{id} | 删除会话（同时删除消息） | 是 |
| PUT | /api/chat/sessions/{id} | 更新会话标题 | 是 |
| GET | /api/chat/sessions/{id}/messages | 获取会话消息列表 | 是 |

### 个人中心接口
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/user/profile | 获取个人信息 | 是 |
| PUT | /api/user/profile | 更新个人信息（email/phone） | 是 |
| PUT | /api/user/password | 修改密码（验证旧密码） | 是 |

## 配置说明

### 环境变量配置

项目使用环境变量注入敏感配置：

```bash
# 数据库配置
DB_URL=jdbc:mysql://localhost:3306/ai_manager?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
DB_USERNAME=root
DB_PASSWORD=your_database_password

# JWT 密钥（可选，有默认值）
JWT_SECRET=your-256-bit-secret-key-must-be-at-least-32-characters

# Redis 配置（可选，默认 127.0.0.1:6379）
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password
```

**启动前设置环境变量：**

```bash
# Windows CMD
set DB_PASSWORD=your_password

# Windows PowerShell
$env:DB_PASSWORD="your_password"

# Linux/Mac
export DB_PASSWORD=your_password
```

### 数据库配置

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD}
```

### Redis 配置

```yaml
spring:
  redis:
    host: ${REDIS_HOST:127.0.0.1}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:redis}
    timeout: 3000
    client-type: jedis
    jedis:
      pool:
        max-active: 8
        max-idle: 4
        min-idle: 0
```

Redis 用于缓存模型连通性状态（60s TTL），前端批量刷新时由后端写入 Redis，所有用户共享缓存。

### JWT 配置

```yaml
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-here-must-be-at-least-32-characters-long}
  expiration: 86400000  # 24小时
```

## 数据库表

| 表名 | 说明 |
|------|------|
| sys_user | 用户表 |
| sys_role | 角色表 |
| sys_permission | 权限表 |
| sys_user_role | 用户角色关联 |
| sys_role_permission | 角色权限关联 |
| sys_verify_code | 验证码表 |
| ai_model | AI模型配置表 |
| sys_audit_log | 审计日志表 |
| chat_session | 聊天会话表（关联模型、所属用户） |
| chat_message | 聊天消息表（用户/助手消息） |

## UI 特性

- 登录页：科技感图片背景 + 磨砂玻璃登录卡片
- 主布局：深色渐变侧边栏 + 明亮内容区域
- 模型列表：在线/离线标签（Redis 缓存，60s 自动过期）
- 模型管理：可用性列 + 连通性检测按钮
- 响应式设计，现代化视觉风格
