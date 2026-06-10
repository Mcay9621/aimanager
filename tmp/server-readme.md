# AI Manager 部署说明

## 项目目录

/home/cloudapp/application/
├── backend/
│   ├── bin/                   # 管理脚本
│   │   └── ai-manager          # 统一脚本 (start|stop|restart|status)
│   ├── etc/                   # jar 包与配置文件
│   │   ├── ai-manager-1.0.0.jar
│   │   ├── logback-spring.xml  # 日志配置（每日切分，保留15天）
│   │   └── application-prod.yml (已迁移至 Nacos)
│   ├── log/                   # 运行日志
│   │   ├── backend.log
│   │   └── backend-error.log
│   └── nacos/                 # Nacos 服务端
│       ├── bin/               # Nacos 启动/关闭脚本
│       ├── conf/              # Nacos 配置文件 + MySQL 初始化 SQL
│       ├── target/
│       │   └── nacos-server.jar
│       └── logs/              # Nacos 运行日志
├── frontend/                 # 前端静态文件 (Vue 3 SPA)
│   ├── index.html
│   └── assets/
└── README.md

## 服务管理

| 服务 | 管理命令 | 端口 |
|------|----------|------|
| Nacos | systemctl start|stop|restart|status nacos | 8848 |
| 后端 (Spring Boot) | systemctl start|stop|restart|status ai-manager | 8080 |
| Nginx | systemctl start|stop|restart|reload|status nginx | 80 |
| MySQL | systemctl start|stop|restart|status mysqld | 3306 |

**启动顺序：** MySQL → Nacos → 后端 → Nginx

## 配置文件位置

| 文件 | 说明 |
|------|------|
| /home/cloudapp/application/backend/etc/application-prod.yml | 已废弃，配置已迁移至 Nacos 配置中心 |
| /home/cloudapp/application/backend/etc/logback-spring.xml | Logback 日志配置（每日切分，保留15天） |
| /home/cloudapp/application/backend/nacos/conf/application.properties | Nacos 服务端配置（数据源、认证等） |
| /etc/nginx/conf.d/ai-manager.conf | Nginx 站点配置（反向代理 /api/ -> :8080） |
| /etc/systemd/system/nacos.service | Nacos systemd 服务定义 |
| /etc/systemd/system/ai-manager.service | 后端 systemd 服务定义 |
| /home/cloudapp/application/backend/bin/ai-manager | 管理脚本 (start/stop/restart/status) |

## Nacos 配置中心

- 控制台地址: http://122.51.136.129:8848/nacos
- 认证: 当前未开启（内网环境）
- Data ID: `ai-manager-prod.yaml` (Group: DEFAULT_GROUP)
- 包含配置: 数据库连接、JWT 密钥、API 密钥、MyBatis-Plus 日志

## 关键路径

- 前端静态文件: /home/cloudapp/application/frontend/
- 后端 jar 包: /home/cloudapp/application/backend/etc/ai-manager-1.0.0.jar
- 后端配置: Nacos 配置中心 `ai-manager-prod.yaml`
- 启停脚本: /home/cloudapp/application/backend/bin/ai-manager {start|stop|restart|status}
- 日志目录: /home/cloudapp/application/backend/log/
- Nacos 日志: /home/cloudapp/application/backend/nacos/logs/

## 数据库

- 类型: MySQL 8.0
- 库名: ai_manager（业务库）、nacos_config（Nacos 配置库）
- 连接地址: localhost:3306
- 配置位置: Nacos 配置中心 `ai-manager-prod.yaml`

## 运行用户

项目文件归属用户 cloudapp，后端和 Nacos 服务也以此用户运行。

## 部署更新

```bash
# 更新后端
# 1. 上传新 jar 到 etc/ 目录
# 2. 重启
systemctl restart ai-manager

# 更新 Nacos 配置
# 在控制台 http://122.51.136.129:8848/nacos 编辑 ai-manager-prod.yaml

# 更新前端
cd /home/cloudapp/application/frontend
# 上传新 dist/ 文件到此处
```
