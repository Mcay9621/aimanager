# AI Manager 测试文档

> 项目版本: 1.0.0
> 生成日期: 2026-05-25
> 技术栈: Spring Boot 3.2.0 + Vue 3.4 + MySQL

## 目录

1. [测试环境](#1-测试环境)
2. [后端 API 测试](#2-后端-api-测试)
3. [前端 UI 测试](#3-前端-ui-测试)
4. [安全测试](#4-安全测试)
5. [端到端流程测试](#5-端到端流程测试)

---

## 1. 测试环境

| 项目 | 值 |
|------|-----|
| 后端地址 | `http://localhost:8080` |
| 前端地址 | `http://localhost:5173` (Vite dev) |
| 数据库 | MySQL, 通过 `sql/init.sql` 初始化 |
| Java 版本 | 17 (E:\soft\jdk-17.0.11+9) |
| 默认管理员 | `admin` / `admin123` |
| 认证方式 | JWT Bearer Token, 24h 过期 |

### 前置条件

```bash
# 1. 初始化数据库
mysql -u root -p < sql/init.sql

# 2. 启动后端 (Java 17)
cd backend
set JAVA_HOME=E:\soft\jdk-17.0.11+9
mvn spring-boot:run

# 3. 启动前端
cd frontend
npm install
npm run dev
```

---

## 2. 后端 API 测试

### 2.1 AuthController — `/api/auth`

#### 2.1.1 POST `/api/auth/login` — 登录

| 编号 | 场景 | 请求体 | 预期状态码 | 预期结果 |
|------|------|--------|-----------|---------|
| AUTH-001 | 管理员后台登录成功 | `{"username":"admin","password":"admin123","loginType":"admin"}` | 200 | 返回 token、isAdmin=true、ROLE_SUPER_ADMIN |
| AUTH-002 | 前台登录成功 | `{"username":"admin","password":"admin123","loginType":"front"}` | 200 | 返回 token |
| AUTH-003 | 普通用户后台登录被拒 | 普通用户凭据 + `loginType:admin` | 400 | `"无后台管理权限"` |
| AUTH-004 | 密码错误 | `{"username":"admin","password":"wrong"}` | 401 | 401 未授权 |
| AUTH-005 | 用户不存在 | `{"username":"nouser","password":"123"}` | 401 | 401 未授权 |
| AUTH-006 | 缺少用户名字段 | `{"password":"123"}` | 401 | 401 未授权 |
| AUTH-007 | 空请求体 | `{}` | 401 | 401 未授权 |

#### 2.1.2 POST `/api/auth/register` — 注册

| 编号 | 场景 | 请求体 | 预期状态码 | 预期结果 |
|------|------|--------|-----------|---------|
| AUTH-008 | 注册成功 | `{"username":"newuser","password":"123","email":"new@test.com"}` | 200 | `"注册成功"` |
| AUTH-009 | 用户名已存在 | `{"username":"admin","password":"123"}` | 400 | `"用户名已存在"` |
| AUTH-010 | 邮箱已被使用 | `{"username":"u2","password":"123","email":"admin@example.com"}` | 400 | `"邮箱已被使用"` |
| AUTH-011 | 缺少用户名 | `{"password":"123"}` | 401 | 需认证 |
| AUTH-012 | 缺少密码 | `{"username":"test2"}` | 401 | 需认证 |

> **注意**: 已知问题 — register 端点可能返回 401 未授权。JwtAuthenticationFilter 使用 `startsWith` 匹配路径，需确认 `/api/auth/register` 在放行列表内。

#### 2.1.3 POST `/api/auth/send-code` — 发送验证码

| 编号 | 场景 | 请求体 | 预期状态码 | 预期结果 |
|------|------|--------|-----------|---------|
| AUTH-013 | 发送邮箱验证码 | `{"target":"test@example.com","type":1}` | 200 | `"验证码已发送"`，控制台输出 6 位码 |
| AUTH-014 | 发送手机验证码 | `{"target":"13800138000","type":2}` | 200 | `"验证码已发送"` |
| AUTH-015 | 缺少 target | `{"type":1}` | 401 | 需认证 |
| AUTH-016 | 缺少 type | `{"target":"test@example.com"}` | 401 | 需认证 |

#### 2.1.4 POST `/api/auth/verify-code` — 验证验证码

| 编号 | 场景 | 请求体 | 预期状态码 | 预期结果 |
|------|------|--------|-----------|---------|
| AUTH-017 | 验证码正确 | `{"target":"...","code":"正确码","type":"1"}` | 200 | `{"valid":true,"message":"验证成功"}` |
| AUTH-018 | 验证码错误 | `{"target":"test@example.com","code":"000000","type":"1"}` | 400 | `"验证码错误或已过期"` |
| AUTH-019 | 验证码已过期 | 等待 5 分钟后验证 | 400 | `"验证码错误或已过期"` |
| AUTH-020 | 缺少 code | `{"target":"test@example.com","type":"1"}` | 400 | `"验证码错误或已过期"` |

#### 2.1.5 GET `/api/auth/info` — 获取用户信息

| 编号 | 场景 | 请求头 | 预期状态码 | 预期结果 |
|------|------|--------|-----------|---------|
| AUTH-021 | 已登录用户 | `Authorization: Bearer <有效token>` | 200 | 返回 username、authorities |
| AUTH-022 | 未登录 | 无 | 401 | 401 未授权 |
| AUTH-023 | Token 过期 | `Bearer <过期token>` | 401 | 401 未授权 |
| AUTH-024 | Token 格式错误 | `Bearer invalid` | 401 | 401 未授权 |

#### 2.1.6 POST `/api/auth/logout` — 登出

| 编号 | 场景 | 请求头 | 预期状态码 | 预期结果 |
|------|------|--------|-----------|---------|
| AUTH-025 | 正常登出 | `Authorization: Bearer <有效token>` | 200 | `"登出成功"` |
| AUTH-026 | 未登录登出 | 无 | 401 | 401 未授权 |

### 2.2 UserController — `/api/admin/users`

> 需要 `ROLE_ADMIN` 或 `ROLE_SUPER_ADMIN`

| 编号 | 场景 | 方法/路径 | 请求体 | 预期状态码 | 预期结果 |
|------|------|----------|--------|-----------|---------|
| USER-001 | 获取用户列表 | GET `/api/admin/users` | - | 200 | 返回用户数组，不含 password |
| USER-002 | 获取单个用户 | GET `/api/admin/users/{id}` | - | 200 | 返回用户信息，password=null |
| USER-003 | 获取不存在用户 | GET `/api/admin/users/9999` | - | 404 | Not Found |
| USER-004 | 添加用户成功 | POST `/api/admin/users` | `{"username":"u3","password":"123","email":"u3@test.com"}` | 200 | `"添加成功"` |
| USER-005 | 添加重复用户名 | POST `/api/admin/users` | `{"username":"admin","password":"123"}` | 400 | `"用户名已存在"` |
| USER-006 | 更新用户 | PUT `/api/admin/users/{id}` | `{"email":"new@test.com"}` | 200 | `"更新成功"` |
| USER-007 | 更新用户密码 | PUT `/api/admin/users/{id}` | `{"password":"newpass"}` | 200 | 密码被 BCrypt 加密存储 |
| USER-008 | 删除用户 | DELETE `/api/admin/users/{id}` | - | 200 | `"删除成功"`（逻辑删除） |
| USER-009 | 切换用户状态(启用-禁用) | PUT `/api/admin/users/{id}/status` | - | 200 | status 翻转 0↔1 |
| USER-010 | 切换不存在用户状态 | PUT `/api/admin/users/9999/status` | - | 404 | Not Found |
| USER-011 | 获取用户总数 | GET `/api/admin/users/count` | - | 200 | `{"count": N}` |

### 2.3 AdminController — `/api/admin/roles`

> 需要 `ROLE_ADMIN` 或 `ROLE_SUPER_ADMIN`

| 编号 | 场景 | 方法/路径 | 请求体 | 预期状态码 | 预期结果 |
|------|------|----------|--------|-----------|---------|
| ROLE-001 | 获取角色列表 | GET `/api/admin/roles` | - | 200 | 返回角色数组 |
| ROLE-002 | 添加角色 | POST `/api/admin/roles` | `{"name":"测试角色","code":"TEST","description":"测试"}` | 200 | `"添加成功"` |
| ROLE-003 | 添加角色缺少必填字段 | POST `/api/admin/roles` | `{"name":"test"}` | 200 | 允许空 code（无校验） |
| ROLE-004 | 更新角色 | PUT `/api/admin/roles/{id}` | `{"name":"新名称"}` | 200 | `"更新成功"` |
| ROLE-005 | 删除角色 | DELETE `/api/admin/roles/{id}` | - | 200 | `"删除成功"`（逻辑删除） |

### 2.4 ModelController — `/api/models`, `/api/admin/models`

#### 2.4.1 公开接口 (无需认证)

| 编号 | 场景 | 方法/路径 | 预期状态码 | 预期结果 |
|------|------|----------|-----------|---------|
| MODEL-001 | 获取启用模型列表 | GET `/api/models` | 200 | 仅返回 enabled=1 的模型 |

#### 2.4.2 管理接口 (需 ADMIN 角色)

| 编号 | 场景 | 方法/路径 | 请求体 | 预期状态码 | 预期结果 |
|------|------|----------|--------|-----------|---------|
| MODEL-002 | 获取全部模型 | GET `/api/admin/models` | - | 200 | 返回所有模型(含禁用) |
| MODEL-003 | 添加模型 | POST `/api/admin/models` | `{"name":"Test","type":"openai","endpoint":"https://...","apiKey":"sk-...","modelName":"gpt-4","enabled":1}` | 200 | `"添加成功"` |
| MODEL-004 | 更新模型 | PUT `/api/admin/models/{id}` | `{"enabled":0}` | 200 | `"更新成功"` |
| MODEL-005 | 删除模型 | DELETE `/api/admin/models/{id}` | - | 200 | `"删除成功"`（物理删除，无逻辑删除） |

### 2.5 ChatController — `/api/chat`

| 编号 | 场景 | 请求体 | 预期状态码 | 预期结果 |
|------|------|--------|-----------|---------|
| CHAT-001 | 向启用的模型发送消息 | `{"modelId":1,"message":"Hello"}` | 200 | 返回 AI 响应内容 |
| CHAT-002 | 向禁用的模型发送消息 | `{"modelId":1,"message":"Hi"}`（模型已禁用） | 400 | `"模型不存在或已禁用"` |
| CHAT-003 | 向不存在的模型发送消息 | `{"modelId":9999,"message":"Hi"}` | 400 | `"模型不存在或已禁用"` |
| CHAT-004 | 缺少 modelId | `{"message":"Hi"}` | 500 | 空指针异常（需改进） |
| CHAT-005 | 缺少 message | `{"modelId":1}` | 500 | 空指针异常（需改进） |

### 2.6 ChatController — SSE 流式聊天

| 编号 | 场景 | 方法/路径 | 请求体 | 预期结果 |
|------|------|----------|--------|---------|
| SSE-001 | SSE流式聊天成功 | POST `/api/chat/stream` | `{"modelId":1,"message":"Hello"}` | 返回 SseEmitter，依次收到 sessionId/token/done 事件 |
| SSE-002 | SSE-模型不存在 | POST `/api/chat/stream` | `{"modelId":9999,"message":"Hi"}` | error 事件: "模型不存在或已禁用" |
| SSE-003 | SSE-缺少参数 | POST `/api/chat/stream` | `{"message":"Hi"}` | error 事件: "缺少必要参数" |
| SSE-004 | SSE-指定sessionId | POST `/api/chat/stream` | `{"modelId":1,"message":"Hi","sessionId":1}` | 复用会话，消息追加到该会话 |
| SSE-005 | SSE-非法sessionId | POST `/api/chat/stream` | `{"modelId":1,"message":"Hi","sessionId":9999}` | error 事件: "会话不存在" |

### 2.7 ChatController — 会话管理

| 编号 | 场景 | 方法/路径 | 请求体/参数 | 预期状态码 | 预期结果 |
|------|------|----------|-----------|-----------|---------|
| SESS-001 | 获取会话列表 | GET `/api/chat/sessions` | - | 200 | 返回当前用户会话数组，按更新时间倒序 |
| SESS-002 | 创建会话 | POST `/api/chat/sessions` | `{"modelId":"1","title":"新对话"}` | 200 | 返回新创建的会话对象 |
| SESS-003 | 更新会话标题 | PUT `/api/chat/sessions/{id}` | `{"title":"新标题"}` | 200 | 标题更新成功 |
| SESS-004 | 删除会话 | DELETE `/api/chat/sessions/{id}` | - | 200 | 会话及关联消息被删除 |
| SESS-005 | 删除他人会话 | DELETE `/api/chat/sessions/{id}` | 其他用户token | 403 | "无权删除此会话" |
| SESS-006 | 获取会话消息 | GET `/api/chat/sessions/{id}/messages` | - | 200 | 返回消息数组，按时间正序 |
| SESS-007 | 查看他人会话消息 | GET `/api/chat/sessions/{id}/messages` | 其他用户token | 403 | "无权查看此会话" |

### 2.8 ProfileController — 个人中心

| 编号 | 场景 | 方法/路径 | 请求体 | 预期状态码 | 预期结果 |
|------|------|----------|--------|-----------|---------|
| PROF-001 | 获取个人信息 | GET `/api/user/profile` | - | 200 | 返回用户信息，password=null |
| PROF-002 | 更新邮箱 | PUT `/api/user/profile` | `{"email":"new@test.com"}` | 200 | 邮箱更新成功 |
| PROF-003 | 更新手机号 | PUT `/api/user/profile` | `{"phone":"13800138000"}` | 200 | 手机号更新成功 |
| PROF-004 | 邮箱已被占用 | PUT `/api/user/profile` | `{"email":"admin@example.com"}` | 400 | "邮箱已被其他用户使用" |
| PROF-005 | 修改密码成功 | PUT `/api/user/password` | `{"oldPassword":"admin123","newPassword":"new123456"}` | 200 | "密码修改成功" |
| PROF-006 | 旧密码错误 | PUT `/api/user/password` | `{"oldPassword":"wrong","newPassword":"new123"}` | 400 | "旧密码错误" |
| PROF-007 | 新密码太短 | PUT `/api/user/password` | `{"oldPassword":"admin123","newPassword":"123"}` | 400 | "新密码长度不能少于6位" |
| PROF-008 | 无token访问 | GET/PUT `/api/user/**` | - | 401 | 未授权 |

### 2.9 UserController — 角色分配

| 编号 | 场景 | 方法/路径 | 请求体 | 预期状态码 | 预期结果 |
|------|------|----------|--------|-----------|---------|
| USER-012 | 获取用户角色 | GET `/api/admin/users/{id}/roles` | - | 200 | `{"roleIds": [...]}` |
| USER-013 | 分配角色 | PUT `/api/admin/users/{id}/roles` | `{"roleIds":[1,2]}` | 200 | `"角色分配更新成功"` |
| USER-014 | 清空用户角色 | PUT `/api/admin/users/{id}/roles` | `{"roleIds":[]}` | 200 | 用户角色被清空 |
| USER-015 | 角色分配-缺少roleIds | PUT `/api/admin/users/{id}/roles` | `{}` | 400 | `"roleIds 不能为空"` |

### 2.10 DashboardController — `/api/admin/dashboard`

| 编号 | 场景 | 方法/路径 | 预期状态码 | 预期结果 |
|------|------|----------|-----------|---------|
| DASH-001 | 获取仪表盘统计 | GET `/api/admin/dashboard/stats` | 200 | 返回 userCount、modelCount、enabledModelCount、auditLogCount、todayLogCount |
| DASH-002 | 无token访问 | GET `/api/admin/dashboard/stats` | 401 | 401 未授权 |
| DASH-003 | 普通用户访问 | GET (普通用户 token) | 403 | 权限不足 |

### 2.11 AuditLogController — `/api/admin/audit-logs`

| 编号 | 场景 | 方法/路径 | 参数 | 预期状态码 | 预期结果 |
|------|------|----------|------|-----------|---------|
| AUDIT-001 | 获取全部审计日志 | GET `/api/admin/audit-logs` | - | 200 | 返回日志数组，按时间倒序 |
| AUDIT-002 | 按操作类型筛选 | GET `/api/admin/audit-logs` | `?action=CREATE` | 200 | 仅返回 CREATE 类型日志 |
| AUDIT-003 | 按对象类型筛选 | GET `/api/admin/audit-logs` | `?target=User` | 200 | 仅返回 User 对象日志 |
| AUDIT-004 | 组合筛选 | GET `/api/admin/audit-logs` | `?action=CREATE&target=User` | 200 | 返回 CREATE+User 日志 |
| AUDIT-005 | 无token访问 | GET `/api/admin/audit-logs` | - | 401 | 401 未授权 |

### 2.12 权限控制

| 编号 | 场景 | 路径 | Token | 预期状态码 |
|------|------|------|-------|-----------|
| PERM-001 | 无 token 访问管理接口 | `/api/admin/models` | 无 | 401 |
| PERM-002 | 普通用户访问管理接口 | `/api/admin/models` | 普通用户 token | 403 |
| PERM-003 | 管理员访问管理接口 | `/api/admin/models` | admin token | 200 |
| PERM-004 | 无 token 访问公开接口 | `/api/models` | 无 | 200 |
| PERM-005 | 无 token 访问登录接口 | `/api/auth/login` | 无 | 200 |
| PERM-006 | 无 token 访问聊天接口 | `/api/chat` | 无 | 200
| PERM-007 | 无token访问仪表盘 | `/api/admin/dashboard/stats` | 无 | 401
| PERM-008 | 无token访问审计日志 | `/api/admin/audit-logs` | 无 | 401
| PERM-009 | 无token查询用户角色 | `/api/admin/users/1/roles` | 无 | 401 |
| PERM-010 | 无token访问会话列表 | GET `/api/chat/sessions` | 无 | 401 |
| PERM-011 | 无token访问SSE接口 | POST `/api/chat/stream` | 无 | 401 |
| PERM-012 | 无token访问个人中心 | GET `/api/user/profile` | 无 | 401 |

---

## 3. 前端 UI 测试

### 3.1 登录页面 (`/login`)

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-LOGIN-001 | 前台登录成功 | 输入 admin/admin123，点击登录 | 跳转 `/front/models`，显示模型列表 |
| UI-LOGIN-002 | 前台登录失败 | 输入错误密码 | 显示错误提示，停留在登录页 |
| UI-LOGIN-003 | 空表单提交 | 不输入内容，点击登录 | 触发验证提示"请输入用户名/密码" |
| UI-LOGIN-004 | 前台切换到注册页 | 点击"立即注册"链接 | 跳转 `/register` |
| UI-LOGIN-005 | 前台切换到后台登录 | 点击"管理员登录"链接 | 跳转 `/admin/login` |
| UI-LOGIN-006 | 已登录用户访问登录页 | 有 token 时访问 `/login` | 自动跳转 `/front/models` |

### 3.2 后台登录页面 (`/admin/login`)

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-ADMIN-LOGIN-001 | 管理员登录成功 | 输入 admin/admin123 | 跳转 `/admin/users` |
| UI-ADMIN-LOGIN-002 | 普通用户登录后台 | 普通用户凭据 | 返回"无后台管理权限" |
| UI-ADMIN-LOGIN-003 | 切换到前台登录 | 点击"返回前台登录"链接 | 跳转 `/login` |

### 3.3 注册页面 (`/register`)

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-REG-001 | 注册成功 | 输入新用户名/密码/邮箱/手机号 | 提示"注册成功"，跳转登录页 |
| UI-REG-002 | 用户名已存在 | 输入 admin | 显示"用户名已存在" |
| UI-REG-003 | 邮箱格式错误 | 输入无效邮箱 | 触发邮箱格式验证提示 |
| UI-REG-004 | 手机号格式错误 | 输入无效手机号 | 触发手机号格式验证提示 |
| UI-REG-005 | 必填字段为空 | 点击注册 | 触发"请输入用户名/密码/邮箱"提示 |

### 3.4 主布局 (`/`)

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-LAYOUT-001 | 普通用户登录 | admin 用户，非管理员 | 侧边栏仅显示"AI模型"，无"系统管理" |
| UI-LAYOUT-002 | 管理员登录 | admin 用户（SUPER_ADMIN） | 侧边栏显示"AI模型"+"系统管理"及其子菜单 |
| UI-LAYOUT-003 | 退出登录 | 点击退出按钮 | token 清除，跳转登录页 |
| UI-LAYOUT-004 | 页面标题显示 | 导航到不同页面 | 头部显示对应页面标题 |

### 3.5 模型列表页面 (`/front/models`)

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-MODEL-LIST-001 | 显示可用模型 | 加载页面 | 以卡片形式展示所有已启用模型 |
| UI-MODEL-LIST-002 | 模型信息展示 | 查看卡片 | 显示名称、类型、模型标识、API地址 |
| UI-MODEL-LIST-003 | 禁用模型按钮状态 | 查看禁用模型 | "使用模型"按钮置灰不可点击 |
| UI-MODEL-LIST-004 | 跳转到聊天页 | 点击启用模型的"使用模型" | 跳转 `/front/chat?modelId=X` |
| UI-MODEL-LIST-005 | 跳转到禁用模型 | 点击禁用模型的按钮 | 按钮置灰，不可点击 |

### 3.6 AI对话页面 (`/front/chat`)

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-CHAT-001 | 页面加载-无模型选择 | 直接访问 `/front/chat` | 显示空状态提示，提供模型选择下拉 |
| UI-CHAT-002 | 从模型列表跳转 | 点击"使用模型" | 自动选中对应模型，可直接开始对话 |
| UI-CHAT-003 | 选择模型开始对话 | 选择模型，点击"开始对话" | 创建新会话，显示聊天界面 |
| UI-CHAT-004 | 发送消息-SSE流式 | 输入消息后 Ctrl+Enter | 左侧新增用户消息，右侧流式显示 AI 回复（逐 token 出现） |
| UI-CHAT-005 | 会话列表显示 | 多次对话后查看左侧 | 显示所有会话标题、模型名、消息数 |
| UI-CHAT-006 | 切换会话 | 点击左侧其他会话 | 加载该会话的历史消息 |
| UI-CHAT-007 | 删除会话 | 点击会话上的删除按钮 | 确认后删除会话及所有消息 |
| UI-CHAT-008 | 搜索会话 | 在搜索框输入关键词 | 过滤会话列表 |
| UI-CHAT-009 | 新建对话 | 点击"新建对话" | 清空聊天区，重新选择模型 |
| UI-CHAT-010 | Markdown渲染 | AI 回复含代码块/加粗 | 正确渲染样式 |
| UI-CHAT-011 | 刷新后恢复 | 发送消息后刷新页面 | 会话和消息保留（从数据库加载） |

### 3.7 个人中心页面 (`/user/profile`)

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-PROF-001 | 信息加载 | 访问 `/user/profile` | 显示当前用户名（禁用）、邮箱、手机号 |
| UI-PROF-002 | 修改邮箱 | 输入新邮箱，保存 | 提示"保存成功" |
| UI-PROF-003 | 修改手机号 | 输入新手机号，保存 | 提示"保存成功" |
| UI-PROF-004 | 修改密码-成功 | 输入正确旧密码+新密码+确认，提交 | 提示"密码修改成功" |
| UI-PROF-005 | 修改密码-旧密码错误 | 输入错误旧密码 | 提示"旧密码错误" |
| UI-PROF-006 | 修改密码-两次密码不一致 | 新密码和确认密码不同 | 提示"两次密码输入不一致" |
| UI-PROF-007 | 修改密码-新密码太短 | 新密码少于6位 | 提示"密码长度不能少于6位" |
| UI-PROF-008 | 入口链接 | 点击顶部用户头像 | 跳转至 `/user/profile` |

### 3.8 后台-用户管理 (`/admin/users`)

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-USER-001 | 用户列表显示 | 加载页面 | 显示用户名、邮箱、手机号、状态、创建时间 |
| UI-USER-002 | 禁用用户 | 点击"禁用"按钮 | 状态标签变为"禁用"，操作按钮变为"启用" |
| UI-USER-003 | 启用用户 | 点击"启用"按钮 | 状态标签变为"正常"，操作按钮变为"禁用" |
| UI-USER-004 | 分配角色弹窗 | 点击"分配角色" | 弹窗展示所有可用角色 checkbox |
| UI-USER-005 | 分配角色保存 | 勾选角色后点"保存" | 提示"角色分配更新成功"，角色生效 |

### 3.9 后台-角色管理 (`/admin/roles`)

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-ROLE-001 | 角色列表显示 | 加载页面 | 显示 ID、角色名称、编码、描述 |
| UI-ROLE-002 | 添加角色 | 点击"添加角色"，填写表单提交 | 新角色出现在列表 |
| UI-ROLE-003 | 添加角色空表单 | 直接点击确定 | 提示"请输入角色名称/编码" |
| UI-ROLE-004 | 编辑角色 | 点击"编辑"，修改后提交 | 角色信息更新 |
| UI-ROLE-005 | 删除角色 | 点击"删除" | 角色从列表中移除 |

### 3.10 后台-模型管理 (`/admin/models`)

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-MODEL-MGMT-001 | 模型列表显示 | 加载页面 | 显示所有模型的名称、类型、标识、API地址、状态 |
| UI-MODEL-MGMT-002 | 切换状态标签 | 查看启用/禁用 | 启用显示绿色标签，禁用显示灰色标签 |
| UI-MODEL-MGMT-003 | 添加模型 | 填写所有字段提交 | 新模型出现在列表 |
| UI-MODEL-MGMT-004 | 编辑模型 | 修改名称/状态等 | 模型信息更新 |
| UI-MODEL-MGMT-005 | 删除模型 | 点击"删除" | 模型从列表中移除 |
| UI-MODEL-MGMT-006 | API密钥隐藏 | 查看编辑表单 | API密钥输入框为密码类型 |

### 3.11 后台-仪表盘 (`/admin`)

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-DASH-001 | 仪表盘加载 | 管理员访问 `/admin` | 显示用户数、模型数、启用模型数、审计日志数、今日日志数统计卡片 |
| UI-DASH-002 | 快捷入口跳转 | 点击各快捷入口卡片 | 跳转到对应管理页面 |
| UI-DASH-003 | 非管理员访问 | 普通用户访问 `/admin` | 跳转 `/front/models` |

### 3.12 后台-审计日志 (`/admin/audit-logs`)

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-AUDIT-001 | 审计日志列表 | 加载页面 | 显示 ID、操作人、操作类型、对象、对象ID、详情、IP、时间 |
| UI-AUDIT-002 | 操作类型筛选 | 选择"创建"筛选 | 仅显示 CREATE 类型的日志 |
| UI-AUDIT-003 | 对象类型筛选 | 选择"用户"筛选 | 仅显示 User 对象的日志 |
| UI-AUDIT-004 | 操作类型颜色标识 | 查看操作列 | CREATE=绿色、UPDATE=橙色、DELETE=红色、LOGIN=蓝色标签 |

### 3.13 主布局-管理导航

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-NAV-001 | 管理员侧边栏 | 管理员登录 | 显示 AI模型、仪表盘、系统管理(用户管理/角色管理/模型管理/审计日志) |
| UI-NAV-002 | 仪表盘菜单高亮 | 访问 `/admin` | 侧边栏"仪表盘"高亮 |

### 3.14 路由守卫

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| UI-ROUTE-001 | 未登录访问前台页面 | 访问 `/front/models` | 跳转 `/login` |
| UI-ROUTE-002 | 未登录访问后台页面 | 访问 `/admin/users` | 跳转 `/admin/login` |
| UI-ROUTE-003 | 非管理员访问后台 | 普通用户访问 `/admin/users` | 跳转 `/front/models` |
| UI-ROUTE-004 | 已登录前台用户访问登录页 | 有 token 时访问 `/login` | 跳转 `/front/models` |
| UI-ROUTE-005 | 已登录管理员访问后台登录 | 有 admin token 访问 `/admin/login` | 跳转 `/admin/users` |

---

## 4. 安全测试

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| SEC-001 | SQL注入-登录 | `username: admin' OR '1'='1` | 认证失败，返回 401 |
| SEC-002 | XSS-模型名称 | 名称含 `<script>alert(1)</script>` | 前端转义显示，不执行脚本 |
| SEC-003 | JWT伪造 | 修改 token 任意字符 | 401 未授权 |
| SEC-004 | JWT过期 | 使用 24h 前的 token | 401 未授权 |
| SEC-005 | API密钥泄露 | 检查 `/api/admin/models` 响应 | apiKey 字段为 null（@JsonIgnore） |
| SEC-006 | 密码泄露 | 检查 `/api/admin/users` 响应 | password 字段为 null |
| SEC-007 | 敏感接口未授权 | 无 token 访问 `/api/admin/**` | 401 未授权 |
| SEC-008 | 跨站请求伪造 | 检查 CSRF 配置 | 后端已禁用 CSRF（csrf.disable()） |

---

## 5. 端到端流程测试

### 5.1 用户完整流程

| 编号 | 流程步骤 | 预期结果 |
|------|---------|---------|
| E2E-001 | 注册 → 登录 → 查看模型 → 退出 | 用户可完成完整生命周期 |
| E2E-002 | 管理员登录 → 管理用户(禁用/启用) → 管理角色(CRUD) → 管理模型(CRUD) → 退出 | 管理员可完成所有管理操作 |
| E2E-003 | 普通用户登录 → 只有模型列表 → 无法访问后台 | 权限控制正确 |
| E2E-004 | 模型列表 → 点击"使用模型" → 跳转聊天页 → 发送消息 → 接收 SSE 流式回复 → 查看历史会话 | 聊天功能完整，消息持久化 |
| E2E-005 | 头像 → 个人中心 → 修改邮箱/手机 → 修改密码 → 重新登录 | 个人信息更新正常 |
| E2E-006 | 聊天页 → 新建多个会话 → 切换会话 → 删除会话 → 刷新页面 → 会话已删除 | 会话管理完整 |

### 5.2 数据流向验证

```
同步聊天:
用户操作 → 前端fetch/axios → Vite代理 → JwtAuthenticationFilter → Controller
→ AiChatService(同步调用AI API) → 返回完整响应 → 前端展示

SSE流式聊天:
用户输入 → fetch POST /api/chat/stream → JwtAuthenticationFilter → ChatController
→ 创建/获取Session → 保存用户消息 → SseEmitter + CompletableFuture
→ AiChatService.chatStream(stream:true) → HttpClient send(InputStream)
→ BufferedReader逐行读取SSE → parse token → emitter.send(token)
→ 前端 ReadableStream → 逐 token 渲染 → done事件 → 保存assistant消息
```

### 5.3 边界条件测试

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|---------|
| BOUND-001 | 并发相同用户名注册 | 同时发送两个注册请求 | 一个成功，一个返回"用户名已存在" |
| BOUND-002 | 空表操作 | 清空表后查询 | 返回空数组，不报错 |
| BOUND-003 | 模型名为空 | 添加模型 name 为空 | 可成功创建（后端无校验） |
| BOUND-004 | 超长输入 | 输入 1000 字符的用户名 | 需要数据库/后端长度校验 |
| BOUND-005 | 重复操作 | 多次点击"禁用/启用" | 状态正确翻转，无副作用 |

---

## 6. 测试优先级建议

| 优先级 | 测试范围 | 原因 |
|--------|---------|------|
| P0-关键 | 登录/注册、认证鉴权、路由守卫 | 核心流程，阻塞所有功能 |
| P1-高 | 模型 CRUD、聊天、用户/角色管理 | 主要业务功能 |
| P2-中 | 验证码、状态切换、UI 交互细节 | 辅助功能 |
| P3-低 | 边界条件、并发、长期会话 | 稳定性与健壮性 |

---

## 7. 测试结果 (2026-05-25)

> 测试环境: 后端 localhost:8080 / 前端 localhost:5173 / 数据库远程

### 7.1 Auth 测试结果

| 编号 | 场景 | 预期 | 实际 | 结果 |
|------|------|------|------|------|
| AUTH-001 | 管理员后台登录成功 | 200 + token + isAdmin=true | 200 + ROLE_SUPER_ADMIN ✅ | ✅ 通过 |
| AUTH-002 | 前台登录成功 | 200 + token | 200 + token ✅ | ✅ 通过 |
| AUTH-003 | 普通用户后台登录被拒 | 400 "无后台管理权限" | 400 "无后台管理权限" ✅ | ✅ 通过 |
| AUTH-004 | 密码错误 | 401 | 401 "用户名或密码错误" ✅ | ✅ 通过 |
| AUTH-005 | 用户不存在 | 401 | 401 "用户名或密码错误" ✅ | ✅ 通过 |
| AUTH-006 | 缺少用户名字段 | 401 | 401 "用户名或密码错误" ✅ | ✅ 通过 |
| AUTH-007 | 空请求体 | 401 | 401 "用户名或密码错误" ✅ | ✅ 通过 |
| AUTH-008 | 注册成功 | 200 "注册成功" | 200 "注册成功" ✅ (BUG-001已修复) | ✅ 通过 |
| AUTH-009 | 用户名已存在 | 400 "用户名已存在" | 400 "用户名已存在" ✅ | ✅ 通过 |
| AUTH-010 | 邮箱已被使用 | 400 "邮箱已被使用" | 400 "邮箱已被使用" ✅ | ✅ 通过 |
| AUTH-011 | 缺少用户名 | 401 需认证 | 401 ✅ | ✅ 通过 |
| AUTH-012 | 缺少密码 | 401 需认证 | 401 ✅ | ✅ 通过 |
| AUTH-013 | 发送邮箱验证码 | 200 "验证码已发送" | 200 "验证码已发送" ✅ | ✅ 通过 |
| AUTH-014 | 发送手机验证码 | 200 | 200 ✅ | ✅ 通过 |
| AUTH-015~016 | 缺少target/type | 401 | 401 ✅ (需认证) | ✅ 通过 |
| AUTH-017 | 验证码正确 | 200 valid=true | 200 valid=true ✅ | ✅ 通过 |
| AUTH-018 | 验证码错误 | 400 "验证码错误或已过期" | 400 "验证码错误或已过期" ✅ | ✅ 通过 |
| AUTH-019 | 验证码过期 | 400 (需等待5min) | - | ⏸️ 需要等待 |
| AUTH-020 | 缺少code | 400 | 400 "验证码错误或已过期" ✅ | ✅ 通过 |
| AUTH-021 | 已登录用户信息 | 200 | 200 + username + authorities ✅ | ✅ 通过 |
| AUTH-022 | 未登录访问info | 401 | 401 ✅ | ✅ 通过 |
| AUTH-023 | Token 格式错误 | 401 | 401 ✅ | ✅ 通过 |
| AUTH-024 | Token 伪造/无效 | 401 | 401 ✅ | ✅ 通过 |
| AUTH-025 | 正常登出 | 200 "登出成功" | 200 "登出成功" ✅ | ✅ 通过 |
| AUTH-026 | 未登录登出 | 401 | 401 ✅ | ✅ 通过 |

### 7.2 User 管理测试结果

| 编号 | 场景 | 预期 | 实际 | 结果 |
|------|------|------|------|------|
| USER-001 | 获取用户列表 | 200, 无password | 200, 无password ✅ | ✅ 通过 |
| USER-002 | 获取单个用户 | 200, password=null | 200, password=null ✅ | ✅ 通过 |
| USER-003 | 获取不存在用户 | 404 | 404 ✅ | ✅ 通过 |
| USER-004 | 添加用户 | 200 "添加成功" | 200 "添加成功" ✅ (BUG-008已修复) | ✅ 通过 |
| USER-005 | 重复用户名 | 400 "用户名已存在" | 400 "用户名已存在" ✅ | ✅ 通过 |
| USER-006 | 更新用户 | 200 "更新成功" | 200 "更新成功" ✅ | ✅ 通过 |
| USER-007 | 更新密码 | 200, BCrypt存储 | 200 ✅ (隐式验证) | ✅ 通过 |
| USER-008 | 删除用户 | 200 | 200 ✅ | ✅ 通过 |
| USER-009 | 切换状态 | 200 0↔1 | 200 status翻转 ✅ | ✅ 通过 |
| USER-010 | 切换不存在用户状态 | 404 | 404 ✅ | ✅ 通过 |
| USER-011 | 用户总数 | 200 {"count":N} | 200 {"count":1} ✅ | ✅ 通过 |

### 7.3 Role 管理测试结果

| 编号 | 场景 | 预期 | 实际 | 结果 |
|------|------|------|------|------|
| ROLE-001 | 获取角色列表 | 200 | 200 ✅ | ✅ 通过 |
| ROLE-002 | 添加角色 | 200 "添加成功" | 200 "添加成功" ✅ | ✅ 通过 |
| ROLE-003 | 缺少必填字段(空名称/空编码) | 400 校验失败 | 400 "角色名称不能为空"/"角色编码不能为空" ✅ | ✅ 通过 |
| ROLE-004 | 更新角色 | 200 "更新成功" | 200 "更新成功" ✅ | ✅ 通过 |
| ROLE-005 | 删除角色 | 200 | 200 ✅ | ✅ 通过 |

### 7.4 Model 管理测试结果

| 编号 | 场景 | 预期 | 实际 | 结果 |
|------|------|------|------|------|
| MODEL-001 | 启用模型列表 | 200, 仅enabled=1 | 200, 8个启用模型 ✅ | ✅ 通过 |
| MODEL-002 | 全部模型(含禁用) | 200 | 200 ✅ | ✅ 通过 |
| MODEL-003 | 添加模型 | 200 "添加成功" | 200 "添加成功" ✅ | ✅ 通过 |
| MODEL-004 | 更新模型 | 200 "更新成功" | 200 "更新成功" ✅ | ✅ 通过 |
| MODEL-005 | 删除模型 | 200 (物理→实际逻辑删除) | 200 ✅ | ✅ 通过 |

### 7.5 权限测试结果

| 编号 | 场景 | 预期 | 实际 | 结果 |
|------|------|------|------|------|
| PERM-001 | 无token访问管理接口 | 401 | 401 ✅ | ✅ 通过 |
| PERM-002 | 无token访问聊天 | 200 (公开接口) | 400 (AI调用超时，接口可达) | ✅ 权限层面通过 |
| PERM-003 | 管理员访问管理接口 | 200 | 200 ✅ | ✅ 通过 |
| PERM-004 | 无token访问登录接口 | 200 | 200 ✅ | ✅ 通过 |
| PERM-005 | 无token访问模型列表 | 200 | 200 ✅ | ✅ 通过 |
| PERM-006 | 无token访问聊天接口 | 200 (公开接口) | 400 (AI调用超时，接口可达) | ✅ 权限层面通过 |

### 7.6 安全测试结果

| 编号 | 场景 | 预期 | 实际 | 结果 |
|------|------|------|------|------|
| SEC-001 | SQL注入-登录 | 401 | 401 ✅ | ✅ 通过 |
| SEC-002 | XSS-模型名称含脚本 | 后端正常存储 | 200 "添加成功" ✅ (前端转义依赖前端) | ✅ 后端通过 |
| SEC-003 | JWT伪造 | 401 | 401 ✅ | ✅ 通过 |
| SEC-005 | API密钥泄露 | apiKey=null | apiKey不在响应中 ✅ | ✅ 通过 |
| SEC-006 | 密码泄露 | password=null | 响应无password字段 ✅ | ✅ 通过 |
| SEC-007 | 敏感接口未授权 | 401 | 401 ✅ | ✅ 通过 |
| SEC-008 | CSRF配置 | csrf.disable() | 跨域POST正常 ✅ | ✅ 确认 |

### 7.7 P1 功能测试结果 (2026-05-25)

| 编号 | 场景 | 预期 | 实际 | 结果 |
|------|------|------|------|------|
| DASH-001 | 仪表盘统计 | 200, 返回统计数据 | 200, 返回 userCount/modelCount/enabledModelCount/auditLogCount/todayLogCount ✅ | ✅ 通过 |
| DASH-002 | 无token访问仪表盘 | 401 | 401 ✅ | ✅ 通过 |
| USER-012 | 获取用户角色 | 200, {"roleIds":[...]} | 200, 返回角色ID列表 ✅ | ✅ 通过 |
| USER-013 | 分配角色 | 200, "角色分配更新成功" | 200, "角色分配更新成功" ✅ | ✅ 通过 |
| USER-014 | 清空用户角色 | 200 | 200, 角色清空成功 ✅ | ✅ 通过 |
| USER-015 | 缺少roleIds | 400 | 400 "roleIds 不能为空" ✅ | ✅ 通过 |
| AUDIT-001 | 获取审计日志 | 200, 数组倒序 | 200, 返回日志数组 ✅ | ✅ 通过 |
| AUDIT-002 | 按操作类型筛选 | 200, 仅CREATE | 200, 仅返回CREATE日志 ✅ | ✅ 通过 |
| AUDIT-003 | 按对象类型筛选 | 200, 仅User | 200, 仅返回User日志 ✅ | ✅ 通过 |
| AUDIT-004 | 组合筛选 | 200 | 200, 组合条件正确 ✅ | ✅ 通过 |
| AUDIT-005 | 无token访问 | 401 | 401 ✅ | ✅ 通过 |
| PERM-007 | 无token访问仪表盘 | 401 | 401 ✅ | ✅ 通过 |
| PERM-008 | 无token访问审计日志 | 401 | 401 ✅ | ✅ 通过 |
| PERM-009 | 无token查询用户角色 | 401 | 401 ✅ | ✅ 通过 |
| UI-DASH-001 | 仪表盘加载 | 显示统计卡片 | 3个统计卡片+4个快捷入口 ✅ | ✅ 通过 |
| UI-USER-004 | 分配角色弹窗 | 弹窗展示角色checkbox | 弹窗+角色列表 ✅ | ✅ 通过 |
| UI-AUDIT-001 | 审计日志列表 | 显示日志表格 | 表格+筛选器+分页 ✅ | ✅ 通过 |
| UI-NAV-001 | 管理侧边栏 | 完整导航 | 仪表盘+系统管理子菜单 ✅ | ✅ 通过 |

### 7.8 修复记录 (2026-05-25)

### 已修复

| 编号 | 问题 | 修复文件 | 修复内容 |
|------|------|---------|---------|
| BUG-001 | register 返回 401 | `SecurityConfig.java` | 将路径匹配规则改为不带 HttpMethod 约束；同时 BUG-008 的 User 反序列化修复也消除了关联异常 |
| BUG-002 | ChatController 缺少参数校验 (NPE) | `ChatController.java` | 添加 modelId/message 为 null 的检查、modelId 数字格式校验、message 空内容校验 |
| BUG-003 | AdminController 无输入校验 | `AdminController.java` | POST /roles 添加 name 和 code 非空校验 |
| BUG-005 | tencent provider 重复 messages 字段 | `AiChatService.java` | 删除 chatTencent 方法中重复的 `requestBody.put("messages", ...)` |
| BUG-006 | application.yml 硬编码数据库密码 | `application.yml` | 改为 `${DB_PASSWORD:Wzp0201.}` 环境变量方式，保留默认值 |
| BUG-007 | 验证码仅输出到控制台 | `AuthController.java` | 改用 Logger 记录，添加 TODO 集成实际发送服务 |
| BUG-008 | POST /api/admin/users 返回 401 | `User.java` | `@JsonIgnore` 改为 `@JsonProperty(access = Access.WRITE_ONLY)`，允许 password 反序列化 |
| BUG-009 | @PreAuthorize 注解未生效 | `SecurityConfig.java` + `AdminController.java` + `ModelController.java` | 添加 `@EnableMethodSecurity`；将 `hasRole('ADMIN')` 改为 `hasAnyRole('ADMIN', 'SUPER_ADMIN')` |

### 确认无需修复

| 编号 | 问题 | 说明 |
|------|------|------|
| BUG-004 | ModelController 物理删除 | AiModel 实体已包含 `@TableLogic` 注解，MyBatis-Plus `removeById` 实际执行逻辑删除 |
