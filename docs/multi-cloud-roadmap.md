# 多云资源统一管理控制台 — 功能规划

> 仿照阿里云/腾讯云控制台风格，统一管理多个云厂商的资源。

---

## 已实现功能

- [x] 项目基础框架（Spring Boot + Vue 3 + Element Plus）
- [x] RBAC 权限体系（AdminController + @PreAuthorize）
- [x] AES 加密组件（AesUtil 用于加密存储 Secret）
- [x] 全局异常处理 + DTO Validation
- [x] JWT + RefreshToken 鉴权

---

## 第一期：云账号管理 + 资源总览（当前）

### 1. 云账号管理 `/admin/cloud/accounts`

| 功能 | 状态 | 备注 |
|------|------|------|
| 云账号列表 | ❌ | 表格展示：厂商、别名、AccessKey ID、默认地域、状态、创建时间 |
| 添加云账号 | ❌ | 对话框表单：厂商(阿里/腾讯)、别名、AccessKey ID、AccessKey Secret、默认地域 |
| 编辑云账号 | ❌ | 同添加，Secret 脱敏展示 |
| 删除云账号 | ❌ | 确认后删除 |
| 测试连通性 | ❌ | 调用云厂商 DescribeRegions 或类似轻量 API 测试凭证有效性 |
| Secret 加密存储 | ❌ | 复用 AesUtil，service 层自动加解密 |

### 2. 云资源总览 `/admin/cloud/resources`

| 功能 | 状态 | 备注 |
|------|------|------|
| 地域选择器 | ❌ | 顶部切换地域，联动刷新全部资源 |
| 状态 Tab 筛选 | ❌ | 全部/运行中/已停止/异常 |
| 统一资源列表 | ❌ | 混合展示 ECS、BCC、OSS、BOS，加"厂商"标签列 |
| 快捷操作 | ❌ | 启动/停止/重启 + "更多"下拉菜单 |
| 搜索/过滤 | ❌ | 按名称、IP、类型搜索 |
| 自定义列 | ❌ | 列显隐、排序 |

### 3. 资源详情页 `/admin/cloud/resources/{id}`

| 功能 | 状态 | 备注 |
|------|------|------|
| 基本信息卡片 | ❌ | 名称、ID、地域、状态、配置 |
| 配置详情 | ❌ | CPU/内存/带宽/系统盘 |
| 网络信息 | ❌ | 公网IP、内网IP、VPC |
| 操作日志 | ❌ | 该资源的操作历史 |

---

## 第二期：增强操作（后续迭代）

### 4. 存储管理 `/admin/cloud/storage`

| 功能 | 状态 | 备注 |
|------|------|------|
| OSS/BOS Bucket 列表 | ❌ | 阿里云 OSS + 百度云 BOS 统一视图 |
| Bucket 文件浏览 | ❌ | 类似阿里云 OSS 控制台的文件管理 |
| 创建/删除 Bucket | ❌ | |

### 5. 操作记录与审计

| 功能 | 状态 | 备注 |
|------|------|------|
| 操作日志列表 | ❌ | 记录谁在什么时间对哪个云资源做了什么操作 |
| 操作结果回执 | ❌ | 异步操作的状态跟踪 |

### 6. 实例创建向导

| 功能 | 状态 | 备注 |
|------|------|------|
| 创建实例对话框 | ❌ | 选择镜像、规格、网络、安全组等 |
| 多厂商差异处理 | ❌ | 各厂商参数不同，需要适配层 |

---

## 第三期：高级功能（远期）

### 7. 监控与告警

| 功能 | 状态 | 备注 |
|------|------|------|
| CPU/内存/磁盘监控图表 | ❌ | 对接云厂商监控 API |
| 告警规则管理 | ❌ | |

### 8. 费用分析

| 功能 | 状态 | 备注 |
|------|------|------|
| 费用概览 | ❌ | 多厂商费用汇总 |
| 账单详情 | ❌ | |

### 9. 资源拓扑图

| 功能 | 状态 | 备注 |
|------|------|------|
| 资源关系可视化 | ❌ | 类似腾讯云拓扑图 |

---

## 数据库表设计

### cloud_account

```sql
CREATE TABLE cloud_account (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  provider      VARCHAR(32)  NOT NULL COMMENT 'aliyun / tencent / baidu',
  alias_name    VARCHAR(64)  NOT NULL COMMENT '账号别名',
  access_key    VARCHAR(128) NOT NULL COMMENT 'AccessKey ID',
  access_secret TEXT         NOT NULL COMMENT 'AccessKey Secret (AES加密)',
  region        VARCHAR(32)  DEFAULT '' COMMENT '默认地域',
  status        INT          DEFAULT 1 COMMENT '1=启用 0=禁用',
  deleted       INT          DEFAULT 0,
  create_time   DATETIME,
  update_time   DATETIME,
  UNIQUE KEY uk_alias (alias_name)
);
```

---

## API 接口设计

### 云账号 CRUD

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/cloud/accounts` | 获取账号列表 |
| POST | `/api/admin/cloud/accounts` | 添加账号 |
| PUT | `/api/admin/cloud/accounts/{id}` | 编辑账号 |
| DELETE | `/api/admin/cloud/accounts/{id}` | 删除账号 |
| POST | `/api/admin/cloud/accounts/{id}/test` | 测试连通性 |

### 云资源

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/cloud/resources` | 获取资源列表（支持 ?region=&status=&keyword=&provider=） |
| POST | `/api/admin/cloud/resources/{id}/start` | 启动实例 |
| POST | `/api/admin/cloud/resources/{id}/stop` | 停止实例 |
| POST | `/api/admin/cloud/resources/{id}/restart` | 重启实例 |
| GET | `/api/admin/cloud/resources/{id}` | 获取资源详情 |

---

## 前端页面结构

```
MainLayout.vue (侧边栏)
├── 多云管理 (el-sub-menu)          ← 新增一级导航
│   ├── 云资源总览                   ← /admin/cloud/resources
│   └── 云账号管理                   ← /admin/cloud/accounts
├── 系统管理 (已有)
│   ├── 用户管理
│   └── ...
```

### 云资源总览页面布局

```
┌─────────────────────────────────────────────────────────┐
│  [云厂商 ▼] [地域 ▼]     [搜索框...]  [+添加账号] [刷新] │
├─────────────────────────────────────────────────────────┤
│  [全部] [运行中] [已停止] [异常]         [阿里云 ▼ 腾讯 ▼]│
├─────────────────────────────────────────────────────────┤
│  已选 2 项  [启动] [停止] [重启]                        │
├─────────────────────────────────────────────────────────┤
│ ☐ 资源名称   │厂商│类型│状态│配置  │IP      │地域│操作     │
│ ☐ web-01    │阿里│ECS │●运行│2C4G   │1.2.3.4│华东1│[启][停]│
│ ☐ bcc-01    │腾讯│BCC │●运行│4C8G   │5.6.7.8│华北1│[更多▼]│
│ ☐ oss-data  │阿里│OSS │●正常│100GB  │—      │华东1│[更多▼]│
└─────────────────────────────────────────────────────────┘
```

---

## 技术选型

| 层面 | 选择 | 原因 |
|------|------|------|
| 云厂商 SDK | 阿里云 SDK for Java, 腾讯云 SDK for Java | 官方SDK，成熟稳定 |
| 调用方式 | 同步调用 | 第一版简化，后续可加异步 |
| 缓存 | 不缓存（直接调API） | 第一版简化，每次实时查询 |
| UI 组件 | Element Plus | 项目已有，保持一致 |
| 图标 | el-icon + 自定义图标 | 厂商Logo用文字标签 |
