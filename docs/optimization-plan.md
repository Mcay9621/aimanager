# AI Manager 代码优化计划

> **目标:** 分阶段系统性优化，提升可维护性、性能和代码质量
> **技术栈:** Java 17, Spring Boot 3.2, MyBatis Plus 3.5, Vue 3.4, Element Plus 2.5, Pinia

---

## Phase 1: 快速修复（低风险高价值）

### Task 1: 统一 SecurityConfig 与 PublicPathsProperties

**问题:** SecurityConfig `.permitAll()` 硬编码路径，与 JwtAuthenticationFilter 的 PublicPathsProperties 重复。

- [ ] **修改** `backend/src/main/java/com/example/aimanager/config/SecurityConfig.java`
  - 注入 `PublicPathsProperties`，循环调用 `auth.requestMatchers(path).permitAll()`
  - 删掉原来硬编码的 7 条 `.permitAll()` 路径，只保留 `/api/models/refresh`、`/api/user/**`、`/api/admin/**`

- [ ] **验证:** 启动后端，访问 `/api/auth/login` 和 `GET /api/models` 确认返回 200，不需 token

**回退策略:** 如果路径遗漏，改回硬编码方式

### Task 2: 清理 AiChatService 重复的 StreamCallback

**问题:** AiChatService 内部定义了与 AiProviderStrategy.StreamCallback 相同的接口。

- [ ] **修改** `backend/src/main/java/com/example/aimanager/service/AiChatService.java`
  - 删掉内部 `StreamCallback` 接口定义
  - `chatStream()` 的参数类型从 `StreamCallback` 改为 `AiProviderStrategy.StreamCallback`

- [ ] **修改** `ChatController.java`
  - 所有 `new AiChatService.StreamCallback()` 改为 `new AiProviderStrategy.StreamCallback()`

- [ ] **验证:** `mvn compile` 通过，SSE 流式聊天正常

### Task 3: 为 Result 类添加 ErrorCode 枚举

- [ ] **新建** `backend/src/main/java/com/example/aimanager/common/ErrorCode.java`
  ```java
  public enum ErrorCode { SUCCESS(200), BAD_REQUEST(400), UNAUTHORIZED(401),
      FORBIDDEN(403), NOT_FOUND(404), INTERNAL_ERROR(500);
      public final int code; ErrorCode(int code) { this.code = code; } }
  ```

- [ ] **修改** `Result.java`，把 `200` → `ErrorCode.SUCCESS.code`、`500` → `ErrorCode.INTERNAL_ERROR.code` 等

- [ ] **验证:** `mvn compile` 通过，API 返回 status code 不变

### Task 4: 修复 userStore 模块级副作用

**问题:** stores/user.js 在模块顶层和 setToken() 中各设置了一次 axios 默认头。

- [ ] **修改** `frontend/src/stores/user.js`，删掉模块顶层的 `axios.defaults.headers.common[...]`
- [ ] **修改** `frontend/src/main.js`，在应用初始化时设置一次：`if (localStorage.getItem("token")) axios.defaults.headers.common["Authorization"] = ...`

- [ ] **验证:** 刷新页面后所有请求都带有 Authorization 头

### Task 5: 修复 AesUtil 中 blocking 的 SecureRandom

- [ ] **修改** `AesUtil.java`
  `SecureRandom.getInstanceStrong()` → `getNonBlockingRandom()`
  实现 fallback: try NativePRNGNonBlocking, catch → new SecureRandom()

- [ ] **验证:** 加密解密功能回归测试通过

---

## Phase 2: 性能与生产安全

### Task 6: 为 SSE 流式聊天添加独立线程池

- [ ] **修改** `AsyncConfig.java`
  - 新增 `@Bean("chatAsyncExecutor") Executor chatAsyncExecutor()`
  - corePoolSize = CPU*2, maxPoolSize = CPU*4, SynchronousQueue, AbortPolicy

- [ ] **修改** `ChatController.java` 注入 `@Qualifier("chatAsyncExecutor")`
  - `CompletableFuture.runAsync(() -> {...}, chatAsyncExecutor)`

### Task 7: 模型列表 Redis 缓存

- [ ] **修改** `AiModelService.java`，`getEnabledModels()` 先查 Redis，缓存 TTL=60s
- [ ] `save/updateById/removeById` 方法添加 `redisTemplate.delete(CACHE_KEY)`

### Task 8: 优化 saveAssistantMessage 减少 DB 查询

- [ ] **修改** `ChatController.java`，`saveAssistantMessage` 改为：
  `chatSessionService.lambdaUpdate()...setSql("message_count = message_count + 1").update()`
- [ ] `deleteMessage` 同理使用 `setSql("message_count = message_count - 1")`

### Task 9: 添加 Rate Limiting

- [ ] **新建** `RateLimitFilter.java`
  - login(5/分钟), send-code(3/分钟), register(3/分钟), chat/stream(30/分钟)
  - 基于客户端 IP + 路径 + 滑动窗口的简单 Token Bucket
  - 超限返回 429 + JSON

---

## Phase 3: 核心重构

### Task 10: 提取 AbstractHttpStrategy 基类

- [ ] **新建** `AbstractHttpStrategy.java`
  - 模板方法: chat(), chatStream(), parseResponse()
  - 子类需实现: getChatPath(), getAuthHeaderName(), buildRequestBody(), processStreamLine()

- [ ] **重构** `OpenAiCompatibleStrategy` 继承 AbstractHttpStrategy
- [ ] **重构** `AnthropicStrategy` 继承 AbstractHttpStrategy
- [ ] 每个子类约从 161 行减少到 60-80 行

### Task 11: 拆分 ModelController

- [ ] **新建** `AdminModelController.java`（管理员 CRUD + 连通性测试 + 拉取模型列表）
- [ ] **新建** `ModelUsageController.java`（余额查询 + 用量统计）
- [ ] **精简** `ModelController.java` 只保留公开接口（getEnabledModels, pingModel, refreshModels）

### Task 12: 统一 AuditLogAspect 为注解驱动

- [ ] **删除** AuditLogAspect 中的 5 个 pointcut 定义（User/Role/Model/Cloud/CloudAccount 操作）
- [ ] 在 UserController/RoleController/ModelController 等对应的增删改方法上添加 `@LogAudit` 注解
- [ ] 保留 `logAnnotatedOperation` 和 `logLogin` 两个方法

---

## Phase 4: 前端优化

### Task 13: 抽取模型类型共用逻辑

- [ ] **新建** `frontend/src/composables/useModelTypes.js`
  - 定义 getTypeLabel(), getTypeColor(), getTypeBg()
- [ ] **修改** ModelManage.vue, ModelUsage.vue, Dashboard.vue
  - 引入 `useModelTypes` composable
  - 删掉本地的 getTypeBg/getTypeColor/getTypeLabel 定义

### Task 14: 分解 ChatView.vue（1234 行）

- [ ] **新建** `composables/useSseChat.js`（SSE 连接 + AbortController + 重连状态）
- [ ] **新建** `composables/useChatSessions.js`（会话列表 CRUD + 消息加载）
- [ ] **新建** `components/SessionList.vue`（左侧面板）
- [ ] **新建** `components/MessageList.vue`（消息渲染 + 日期分隔 + Markdown）
- [ ] **新建** `components/ChatInput.vue`（输入框 + Ctrl+Enter 发送 + 快捷键）
- [ ] **精简** `ChatView.vue` 为 ~200 行的编排组件

---

## Phase 5: 架构级优化

### Task 15: CloudResource 多表合并

- [ ] **新建** 单表实体 `CloudResource.java`（含 resourceType 判别字段 + extra JSON 列）
- [ ] **创建** 迁移 SQL `sql/migration_merge_cloud_resources.sql`
- [ ] **重构** `CloudResourceService.java`（删掉 39 个 Mapper 依赖）
- [ ] **删除** 39 对空壳 Entity/Mapper 文件
- [ ] **修改** `CloudResourceController.java` 适配单表查询

### Task 16: 添加 API 版本号

- [ ] 设置 `server.servlet.context-path=/api/v1`
- [ ] Controller 去掉 `/api` 前缀
- [ ] 前端 request.js 和 vite.config.js 同步

---

## 执行建议

| 顺序 | 任务 | 风险 | 价值 |
|------|------|------|------|
| 1 | Tasks 1-3 (路径/StreamCallback/ErrorCode) | 低 | 高 |
| 2 | Tasks 4-5 (userStore/SecureRandom) | 低 | 中 |
| 3 | Tasks 6-8 (线程池/缓存/DB) | 中 | 高 |
| 4 | Task 9 (Rate Limiting) | 低 | 高 |
| 5 | Tasks 10-12 (策略/Controller/AOP) | 中 | 中 |
| 6 | Tasks 13-14 (前端 composable+拆分) | 中 | 中 |
| 7 | Tasks 15-16 (架构级) | 高 | 高 |

**建议:** 每完成一个 task，执行 `mvn compile` 和对应的回归测试，再继续下一个。
