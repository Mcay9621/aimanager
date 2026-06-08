# Claude Code Skills 参考表

> 生成时间: 2026-05-25
> 来源: 系统内置 skills + `~/.claude/plugins/` 插件注册 skills

## 内置技能 (Built-in Skills)

| 技能名称 | 说明 | 调用方式 |
|----------|------|----------|
| `agent-browser` | 浏览器自动化 — 页面导航、表单填写、截图、数据抓取、Web 应用测试 | Claude 自动 / 用户触发 |
| `brainstorming` | 创意工作前置探索 — 创建功能、构建组件前探索用户意图和需求 | 自动触发 |
| `dispatching-parallel-agents` | 将 2 个以上独立任务分派给并行 agent 处理 | Claude 自动 |
| `executing-plans` | 在独立会话中执行实施计划，含审查检查点 | Claude 自动 |
| `finishing-a-development-branch` | 开发完成后的合并/PR/清理引导 | 用户触发 |
| `frontend-design` | 创建高质量前端界面 — 组件、页面、仪表盘、HTML/CSS/React/Vue | 用户触发 |
| `imagen` | 使用 Google Gemini 生成图片 — UI 原型、图标、插画、概念图 | 用户触发 |
| `receiving-code-review` | 接收 Code Review 反馈后使用，验证反馈技术准确性 | 自动触发 |
| `requesting-code-review` | 完成任务/合并前验证工作是否满足需求 | 用户触发 |
| `subagent-driven-development` | 在当前会话中执行独立任务的实施计划 | Claude 自动 |
| `systematic-debugging` | 遇到 bug/测试失败时系统化调试 | 自动触发 |
| `test-driven-development` | 实现功能/bugfix 前先用 TDD 编写测试 | 自动触发 |
| `using-git-worktrees` | 创建隔离工作区进行特性开发 | 自动 / 用户触发 |
| `using-superpowers` | 会话启动时建立技能认知和使用方法 | 自动触发 |
| `verification-before-completion` | 完成前验证 | 自动触发 |
| `writing-plans` | 编写实施计划 | Claude 自动 |
| `writing-skills` | 编写 skills | 用户触发 |
| `update-config` | 配置 settings.json — 权限、环境变量、hooks | 用户触发 |
| `keybindings-help` | 自定义键盘快捷键/和弦绑定 | 用户触发 |
| `simplify` | 审查变更代码并优化复用性、质量和效率 | 用户/自动 |
| `fewer-permission-prompts` | 扫描常用工具调用并添加 allowlist 以减少权限提示 | 用户触发 |
| `loop` | 设置循环任务 — 定期轮询/检查 | 用户触发 |
| `claude-api` | 构建/调试/优化 Claude API / Anthropic SDK 应用 | 用户触发 |
| `init` | 初始化/更新 CLAUDE.md | 用户触发 |
| `review` | 审查 Pull Request | 用户触发 |
| `security-review` | 安全审查 | 用户触发 |

## 插件技能 (Plugin Skills)

### 工作流 & 自动化

| 技能名称 | 来源插件 | 说明 |
|----------|----------|------|
| `claude-automation-recommender` | claude-code-setup | 分析代码库并推荐 Claude Code 自动化配置（hooks、子 agent、skills、MCP） |
| `session-report` | session-report | 生成 Claude Code 会话使用报告 HTML（tokens、缓存、子 agent、skills） |
| `claude-md-improver` | claude-md-management | 审计和改进 CLAUDE.md 文件质量 |
| `writing-hookify-rules` | hookify | 创建 hookify 规则 — 定义监控模式和触发消息 |
| `playground` | playground | 创建交互式 HTML playground — 带控件、预览和复制按钮的探索工具 |

### 前端 & 设计

| 技能名称 | 来源插件 | 说明 |
|----------|----------|------|
| `frontend-design` | frontend-design | 创建高质量、高辨识度的前端界面（已在内置列表中注册） |

### 插件开发 (plugin-dev 系列)

| 技能名称 | 来源插件 | 说明 |
|----------|----------|------|
| `agent-development` | plugin-dev | 创建 Claude Code agent — 子进程、触发条件、系统提示词设计 |
| `command-development` | plugin-dev | 创建斜杠命令 — YAML frontmatter、动态参数、bash 执行 |
| `hook-development` | plugin-dev | 创建 hooks — 事件驱动自动化脚本（PreToolUse、PostToolUse 等） |
| `mcp-integration` | plugin-dev | 集成 MCP 服务器到 Claude Code 插件 |
| `plugin-settings` | plugin-dev | 插件配置存储模式 — `.local.md` 文件 + YAML frontmatter |
| `plugin-structure` | plugin-dev | 插件目录结构、manifest、组件组织最佳实践 |
| `skill-development` | plugin-dev | 创建 skills — 模块化专业知识和流程包 |

### MCP 服务器开发 (mcp-server-dev 系列)

| 技能名称 | 来源插件 | 说明 |
|----------|----------|------|
| `build-mcp-server` | mcp-server-dev | MCP 服务器开发入口 — 需求分析、部署模型选择、工具设计 |
| `build-mcp-app` | mcp-server-dev | MCP App 开发 — 为 MCP 服务器添加交互式 UI Widget |
| `build-mcpb` | mcp-server-dev | 打包 MCPB — 将 MCP 服务器与运行时捆绑分发的本地服务器 |

### 硬件 & 嵌入式

| 技能名称 | 来源插件 | 说明 |
|----------|----------|------|
| `m5-onboard` | cwc-makers | M5Stack ESP32 设备开箱 — 检测、刷固件、安装 MicroPython 应用 |
| `cardputer-buddy` | cwc-makers | Cardputer MicroPython 应用迭代 — 添加应用、推送脚本、查看串口日志 |

### 数学

| 技能名称 | 来源插件 | 说明 |
|----------|----------|------|
| `math-olympiad` | math-olympiad | 解决竞赛数学问题（IMO、Putnam、USAMO、AIME）— 对抗验证 |

### 外部渠道 (External Plugins)

| 技能名称 | 来源插件 | 说明 | 用户可调用 |
|----------|----------|------|-----------|
| `discord:access` | discord | 管理 Discord 频道访问 — 配对审批、允许列表 | 是 |
| `discord:configure` | discord | 设置 Discord 频道 — 保存 bot token、访问策略 | 是 |
| `imessage:access` | imessage | 管理 iMessage 频道访问 | 是 |
| `imessage:configure` | imessage | 检查 iMessage 频道设置和访问策略 | 是 |
| `telegram:access` | telegram | 管理 Telegram 频道访问 | 是 |
| `telegram:configure` | telegram | 设置 Telegram 频道 — 保存 bot token | 是 |

### 示例

| 技能名称 | 来源插件 | 说明 |
|----------|----------|------|
| `example-skill` | example-plugin | 技能开发参考模板 |
| `example-command` | example-plugin | 命令格式示例（skills 目录布局） |

### 其他

| 技能名称 | 来源插件 | 说明 |
|----------|----------|------|
| `skill-creator` | skill-creator | 创建、修改、评估和优化 skills 的完整工作流 |

---

## 技能调用方式

| 方式 | 说明 |
|------|------|
| **用户手动调用** | 输入 `/<skill-name>` 调用用户可调用技能 |
| **Claude 自动触发** | 根据描述(description)字段匹配当前任务上下文自动触发 |
| **用户触发** | 用户明确请求相关功能时触发 |

## 插件来源路径

所有插件技能存储在 `~/.claude/plugins/marketplaces/claude-plugins-official/` 下:
- 内置插件: `plugins/<name>/skills/<skill-name>/SKILL.md`
- 外部插件: `external_plugins/<name>/skills/<skill-name>/SKILL.md`
