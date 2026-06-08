-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_manager DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_manager;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    master_account_id BIGINT DEFAULT NULL COMMENT '关联主账号ID',
    status TINYINT DEFAULT 1 COMMENT '0:禁用 1:正常',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_master_account (master_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 验证码表
CREATE TABLE IF NOT EXISTS sys_verify_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target VARCHAR(100) NOT NULL COMMENT '邮箱或手机号',
    code VARCHAR(10) NOT NULL,
    type TINYINT NOT NULL COMMENT '1:邮箱 2:手机',
    used TINYINT DEFAULT 0 COMMENT '0:未使用 1:已使用',
    expire_time DATETIME NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_target (target),
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验证码表';

-- AI模型表
CREATE TABLE IF NOT EXISTS ai_model (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '模型显示名称',
    type VARCHAR(20) NOT NULL COMMENT '模型类型: openai, anthropic, ali, baidu, byte, tencent',
    endpoint VARCHAR(255) COMMENT 'API地址',
    api_key VARCHAR(255) COMMENT 'API密钥',
    model_name VARCHAR(100) NOT NULL COMMENT '模型标识',
    enabled TINYINT DEFAULT 1 COMMENT '0:禁用 1:启用',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) COMMENT '操作人用户名',
    action VARCHAR(50) COMMENT '操作类型: CREATE, UPDATE, DELETE, LOGIN',
    target VARCHAR(50) COMMENT '操作对象: User, Role, Model, Auth',
    target_id VARCHAR(50) COMMENT '对象ID',
    detail VARCHAR(500) COMMENT '操作详情',
    ip VARCHAR(50) COMMENT '操作IP',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_action (action),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 插入默认管理员账号 (密码: admin123)
INSERT INTO sys_user (username, password, email, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@example.com', 1);

-- 插入默认角色
INSERT INTO sys_role (name, code, description) VALUES
('超级管理员', 'SUPER_ADMIN', '拥有所有权限'),
('普通管理员', 'ADMIN', '除用户管理和角色管理外的权限'),
('普通用户', 'USER', '前台操作权限');

-- 插入默认权限
INSERT INTO sys_permission (name, code, description) VALUES
('用户管理', 'user:manage', '用户管理'),
('角色管理', 'role:manage', '角色管理'),
('模型管理', 'model:manage', 'AI模型管理'),
('模型使用', 'model:use', '使用AI模型');

-- 关联管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 关联角色权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4),
(2, 3), (2, 4),
(3, 4);

-- 聊天会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) COMMENT '会话标题',
    model_id BIGINT NOT NULL COMMENT '关联模型ID',
    model_name VARCHAR(100) COMMENT '模型名称',
    username VARCHAR(50) NOT NULL COMMENT '所属用户',
    user_id BIGINT DEFAULT NULL COMMENT '关联用户ID',
    message_count INT DEFAULT 0 COMMENT '消息数',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_user_id (user_id),
    INDEX idx_model_id (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- 聊天消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT 'user 或 assistant',
    content TEXT NOT NULL COMMENT '消息内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- 云账号表
CREATE TABLE IF NOT EXISTS cloud_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider VARCHAR(32) NOT NULL COMMENT '云厂商: aliyun, tencent, baidu',
    alias_name VARCHAR(64) NOT NULL COMMENT '账号别名',
    access_key VARCHAR(128) NOT NULL COMMENT 'AccessKey ID',
    access_secret TEXT NOT NULL COMMENT 'AccessKey Secret (AES加密)',
    parent_id BIGINT DEFAULT NULL COMMENT '所属主账号ID',
    type VARCHAR(32) DEFAULT 'sub' COMMENT '账号类型: master/sub',
    region VARCHAR(32) DEFAULT '' COMMENT '默认地域',
    status TINYINT DEFAULT 1 COMMENT '1:启用 0:禁用',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_alias (alias_name),
    INDEX idx_provider (provider),
    INDEX idx_status (status),
    INDEX idx_parent (parent_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云账号表';

-- 字典表
CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_code VARCHAR(64) NOT NULL COMMENT '字典类型编码',
    type_name VARCHAR(100) COMMENT '字典类型名称',
    item_key VARCHAR(100) NOT NULL COMMENT '字典项键',
    item_value VARCHAR(255) NOT NULL COMMENT '字典项值',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '0:禁用 1:启用',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type_code (type_code),
    INDEX idx_item_key (item_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典表';

-- 插入云区域字典数据
INSERT INTO sys_dict (type_code, type_name, item_key, item_value, sort_order) VALUES
-- 阿里云区域
('aliyun_region', '阿里云区域', 'cn-qingdao', '华北1（青岛）', 1),
('aliyun_region', '阿里云区域', 'cn-beijing', '华北2（北京）', 2),
('aliyun_region', '阿里云区域', 'cn-zhangjiakou', '华北3（张家口）', 3),
('aliyun_region', '阿里云区域', 'cn-huhehaote', '华北5（呼和浩特）', 4),
('aliyun_region', '阿里云区域', 'cn-wulanchabu', '华北6（乌兰察布）', 5),
('aliyun_region', '阿里云区域', 'cn-hangzhou', '华东1（杭州）', 6),
('aliyun_region', '阿里云区域', 'cn-shanghai', '华东2（上海）', 7),
('aliyun_region', '阿里云区域', 'cn-nanjing', '华东5（南京）', 8),
('aliyun_region', '阿里云区域', 'cn-fuzhou', '华东6（福州）', 9),
('aliyun_region', '阿里云区域', 'cn-shenzhen', '华南1（深圳）', 10),
('aliyun_region', '阿里云区域', 'cn-guangzhou', '华南2（广州）', 11),
('aliyun_region', '阿里云区域', 'cn-chengdu', '西南1（成都）', 12),
('aliyun_region', '阿里云区域', 'cn-hongkong', '香港', 13),
('aliyun_region', '阿里云区域', 'ap-northeast-1', '日本（东京）', 14),
('aliyun_region', '阿里云区域', 'ap-northeast-2', '韩国（首尔）', 15),
('aliyun_region', '阿里云区域', 'ap-southeast-1', '新加坡', 16),
('aliyun_region', '阿里云区域', 'ap-southeast-2', '澳大利亚（悉尼）', 17),
('aliyun_region', '阿里云区域', 'ap-southeast-3', '马来西亚（吉隆坡）', 18),
('aliyun_region', '阿里云区域', 'ap-southeast-5', '印度尼西亚（雅加达）', 19),
('aliyun_region', '阿里云区域', 'ap-southeast-6', '菲律宾（马尼拉）', 20),
('aliyun_region', '阿里云区域', 'ap-southeast-7', '泰国（曼谷）', 21),
('aliyun_region', '阿里云区域', 'ap-south-1', '印度（孟买）', 22),
('aliyun_region', '阿里云区域', 'us-east-1', '美国（弗吉尼亚）', 23),
('aliyun_region', '阿里云区域', 'us-west-1', '美国（硅谷）', 24),
('aliyun_region', '阿里云区域', 'eu-central-1', '德国（法兰克福）', 25),
('aliyun_region', '阿里云区域', 'eu-west-1', '英国（伦敦）', 26),
('aliyun_region', '阿里云区域', 'me-east-1', '阿联酋（迪拜）', 27),
-- 腾讯云区域
('tencent_region', '腾讯云区域', 'ap-guangzhou', '华南地区（广州）', 1),
('tencent_region', '腾讯云区域', 'ap-shenzhen-fsi', '华南地区（深圳金融）', 2),
('tencent_region', '腾讯云区域', 'ap-shanghai', '华东地区（上海）', 3),
('tencent_region', '腾讯云区域', 'ap-shanghai-fsi', '华东地区（上海金融）', 4),
('tencent_region', '腾讯云区域', 'ap-beijing', '华北地区（北京）', 5),
('tencent_region', '腾讯云区域', 'ap-beijing-fsi', '华北地区（北京金融）', 6),
('tencent_region', '腾讯云区域', 'ap-chengdu', '西南地区（成都）', 7),
('tencent_region', '腾讯云区域', 'ap-chongqing', '西南地区（重庆）', 8),
('tencent_region', '腾讯云区域', 'ap-nanjing', '华东地区（南京）', 9),
('tencent_region', '腾讯云区域', 'ap-hongkong', '港澳台地区（香港）', 10),
('tencent_region', '腾讯云区域', 'ap-singapore', '东南亚（新加坡）', 11),
('tencent_region', '腾讯云区域', 'ap-tokyo', '东亚（东京）', 12),
('tencent_region', '腾讯云区域', 'ap-seoul', '东亚（首尔）', 13),
('tencent_region', '腾讯云区域', 'ap-bangkok', '东南亚（曼谷）', 14),
('tencent_region', '腾讯云区域', 'ap-jakarta', '东南亚（雅加达）', 15),
('tencent_region', '腾讯云区域', 'ap-mumbai', '南亚（孟买）', 16),
('tencent_region', '腾讯云区域', 'na-siliconvalley', '北美（硅谷）', 17),
('tencent_region', '腾讯云区域', 'na-ashburn', '北美（弗吉尼亚）', 18),
('tencent_region', '腾讯云区域', 'sa-saopaulo', '南美（圣保罗）', 19),
('tencent_region', '腾讯云区域', 'eu-frankfurt', '欧洲（法兰克福）', 20),
('tencent_region', '腾讯云区域', 'eu-moscow', '欧洲（莫斯科）', 21);

-- 插入默认AI模型配置
INSERT INTO ai_model (name, type, endpoint, api_key, model_name, enabled) VALUES
('GPT-4o', 'openai', 'https://api.openai.com/v1', '', 'gpt-4o', 1),
('GPT-4o-mini', 'openai', 'https://api.openai.com/v1', '', 'gpt-4o-mini', 1),
('Claude 3.5 Sonnet', 'anthropic', 'https://api.anthropic.com', '', 'claude-3-5-sonnet-20241022', 1),
('Claude 3 Haiku', 'anthropic', 'https://api.anthropic.com', '', 'claude-3-haiku-20240307', 1),
('通义千问', 'ali', 'https://dashscope.aliyuncs.com/compatible-mode/v1', '', 'qwen-plus', 1),
('文心一言', 'baidu', 'https://qianfan.baidubce.com/v2', '', 'ernie-4.0-8k', 1),
('豆包', 'byte', 'https://ark.cn-beijing.volces.com/api/v3', '', 'doubao-pro-4k', 1),
('腾讯混元', 'tencent', 'https://hunyuan.tencentcloudapi.com', '', 'hunyuan-pro', 1);

-- ============================================================
-- 云资源表 (主账号纳管子账号, 子账号持有资源)
-- 每类资源独立建表, 统一通过 account_id 关联 cloud_account
-- ============================================================

-- ========== 计算资源 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_cvm (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    instance_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '实例ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '实例名称',
    cpu INT DEFAULT 0 COMMENT 'CPU核数',
    memory INT DEFAULT 0 COMMENT '内存GB',
    public_ip VARCHAR(64) DEFAULT '' COMMENT '公网IP',
    private_ip VARCHAR(64) DEFAULT '' COMMENT '内网IP',
    os_name VARCHAR(255) DEFAULT '' COMMENT '操作系统',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    subnet_id VARCHAR(128) DEFAULT '' COMMENT '所属子网',
    image_id VARCHAR(128) DEFAULT '' COMMENT '镜像ID',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CVM云服务器';

CREATE TABLE IF NOT EXISTS cloud_resource_bms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    instance_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '实例ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '实例名称',
    cpu INT DEFAULT 0 COMMENT 'CPU核数',
    memory INT DEFAULT 0 COMMENT '内存GB',
    public_ip VARCHAR(64) DEFAULT '' COMMENT '公网IP',
    private_ip VARCHAR(64) DEFAULT '' COMMENT '内网IP',
    os_name VARCHAR(255) DEFAULT '' COMMENT '操作系统',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    subnet_id VARCHAR(128) DEFAULT '' COMMENT '所属子网',
    network_card_count INT DEFAULT 0 COMMENT '网卡数量',
    raid_config VARCHAR(255) DEFAULT '' COMMENT 'RAID配置',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BMS裸金属服务器';

-- ========== 存储资源 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_cbs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    disk_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '云盘ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '云盘名称',
    disk_type VARCHAR(32) DEFAULT '' COMMENT '云盘类型(cloud_ssd/cloud_trie)',
    size_gb INT DEFAULT 0 COMMENT '容量GB',
    instance_id VARCHAR(128) DEFAULT '' COMMENT '挂载实例ID',
    zone VARCHAR(64) DEFAULT '' COMMENT '可用区',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_disk_id (disk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CBS云硬盘';

CREATE TABLE IF NOT EXISTS cloud_resource_cfs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    file_system_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '文件系统ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '文件系统名称',
    protocol VARCHAR(32) DEFAULT '' COMMENT '协议类型(NFS/SMB)',
    storage_type VARCHAR(32) DEFAULT '' COMMENT '存储类型(标准/极速)',
    size_gb INT DEFAULT 0 COMMENT '总容量GB',
    used_gb INT DEFAULT 0 COMMENT '已用容量GB',
    mount_point VARCHAR(255) DEFAULT '' COMMENT '挂载点',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '绑定VPC',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CFS文件存储';

CREATE TABLE IF NOT EXISTS cloud_resource_cos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    bucket_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '存储桶名称',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '显示名称',
    storage_class VARCHAR(32) DEFAULT '' COMMENT '存储类型(标准/低频/归档)',
    public_access TINYINT DEFAULT 0 COMMENT '是否开启公共访问',
    object_count INT DEFAULT 0 COMMENT '对象数量',
    size_gb DECIMAL(12,2) DEFAULT 0 COMMENT '存储用量GB',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_bucket_name (bucket_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='COS对象存储';

CREATE TABLE IF NOT EXISTS cloud_resource_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    snapshot_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '快照ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '快照名称',
    disk_id VARCHAR(128) DEFAULT '' COMMENT '关联云盘ID',
    disk_size_gb INT DEFAULT 0 COMMENT '云盘容量GB',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_disk_id (disk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快照';

-- ========== 数据库资源 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_mysql (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    instance_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '实例ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '实例名称',
    engine_version VARCHAR(32) DEFAULT '' COMMENT '引擎版本(5.7/8.0)',
    spec VARCHAR(64) DEFAULT '' COMMENT '规格(2C4G)',
    storage_gb INT DEFAULT 0 COMMENT '存储空间GB',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    subnet_id VARCHAR(128) DEFAULT '' COMMENT '所属子网',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_instance_id (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MySQL数据库';

CREATE TABLE IF NOT EXISTS cloud_resource_oracle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    instance_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '实例ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '实例名称',
    engine_version VARCHAR(32) DEFAULT '' COMMENT '引擎版本',
    spec VARCHAR(64) DEFAULT '' COMMENT '规格',
    storage_gb INT DEFAULT 0 COMMENT '存储空间GB',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    subnet_id VARCHAR(128) DEFAULT '' COMMENT '所属子网',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Oracle数据库';

CREATE TABLE IF NOT EXISTS cloud_resource_redis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    instance_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '实例ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '实例名称',
    engine_version VARCHAR(32) DEFAULT '' COMMENT '引擎版本',
    spec VARCHAR(64) DEFAULT '' COMMENT '规格(2G/4G/8G)',
    storage_gb INT DEFAULT 0 COMMENT '内存容量GB',
    network_type VARCHAR(32) DEFAULT '' COMMENT '网络类型(经典/VPC)',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Redis数据库';

-- ========== 网络资源 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_vpc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    vpc_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'VPC-ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'VPC名称',
    cidr VARCHAR(64) DEFAULT '' COMMENT 'IPv4 CIDR',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认VPC',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_vpc_id (vpc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VPC私有网络';

CREATE TABLE IF NOT EXISTS cloud_resource_vpn (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    vpn_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'VPN网关ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'VPN网关名称',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '绑定VPC',
    public_ip VARCHAR(64) DEFAULT '' COMMENT '公网IP',
    peer_address VARCHAR(255) DEFAULT '' COMMENT '对端地址',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VPN网关';

CREATE TABLE IF NOT EXISTS cloud_resource_nat_gateway (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    nat_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'NAT网关ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'NAT网关名称',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '绑定VPC',
    public_ips VARCHAR(512) DEFAULT '' COMMENT '关联公网IP(逗号分隔)',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NAT网关';

CREATE TABLE IF NOT EXISTS cloud_resource_eip (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    eip_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'EIP实例ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'EIP名称',
    public_ip VARCHAR(64) DEFAULT '' COMMENT '公网IP地址',
    bandwidth INT DEFAULT 0 COMMENT '带宽Mbps',
    instance_id VARCHAR(128) DEFAULT '' COMMENT '绑定实例ID',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_eip_id (eip_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='EIP弹性公网IP';

CREATE TABLE IF NOT EXISTS cloud_resource_clb (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    clb_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'CLB实例ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'CLB名称',
    type VARCHAR(32) DEFAULT '' COMMENT '类型(公网/内网)',
    address VARCHAR(128) DEFAULT '' COMMENT 'CLB域名/VIP',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    subnet_id VARCHAR(128) DEFAULT '' COMMENT '所属子网',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_clb_id (clb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CLB负载均衡';

CREATE TABLE IF NOT EXISTS cloud_resource_cdn (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    domain VARCHAR(255) NOT NULL DEFAULT '' COMMENT '加速域名',
    cname VARCHAR(255) DEFAULT '' COMMENT 'CNAME域名',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '显示名称',
    type VARCHAR(32) DEFAULT '' COMMENT '加速类型(web/download/stream)',
    region VARCHAR(64) DEFAULT '' COMMENT '全球/中国境内/境外',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_domain (domain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDN加速域名';

CREATE TABLE IF NOT EXISTS cloud_resource_subnet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    subnet_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '子网ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '子网名称',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    cidr VARCHAR(64) DEFAULT '' COMMENT 'IPv4 CIDR',
    zone VARCHAR(64) DEFAULT '' COMMENT '可用区',
    available_ip_count INT DEFAULT 0 COMMENT '可用IP数',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_vpc_id (vpc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='子网';

CREATE TABLE IF NOT EXISTS cloud_resource_security_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    security_group_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '安全组ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '安全组名称',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    description VARCHAR(512) DEFAULT '' COMMENT '描述',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_sg_id (security_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全组';

CREATE TABLE IF NOT EXISTS cloud_resource_route_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    route_table_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '路由表ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '路由表名称',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认路由表',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_vpc_id (vpc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路由表';

CREATE TABLE IF NOT EXISTS cloud_resource_direct_connect (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    dc_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '物理专线ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '专线名称',
    circuit_code VARCHAR(128) DEFAULT '' COMMENT '运营商电路编码',
    bandwidth INT DEFAULT 0 COMMENT '带宽Mbps',
    provider_name VARCHAR(64) DEFAULT '' COMMENT '运营商',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物理专线';

CREATE TABLE IF NOT EXISTS cloud_resource_logical_direct_connect (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    ldc_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '逻辑专线ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '逻辑专线名称',
    dc_id VARCHAR(128) DEFAULT '' COMMENT '关联物理专线ID',
    bandwidth INT DEFAULT 0 COMMENT '带宽Mbps',
    vlan INT DEFAULT 0 COMMENT 'VLAN ID',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逻辑专线';

CREATE TABLE IF NOT EXISTS cloud_resource_direct_connect_tunnel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    dct_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '专线通道ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '通道名称',
    dc_id VARCHAR(128) DEFAULT '' COMMENT '关联物理专线ID',
    vlan INT DEFAULT 0 COMMENT 'VLAN ID',
    bandwidth INT DEFAULT 0 COMMENT '带宽Mbps',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专线通道';

CREATE TABLE IF NOT EXISTS cloud_resource_peering_connection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    pc_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '对等连接ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '对等连接名称',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '本端VPC',
    peer_vpc_id VARCHAR(128) DEFAULT '' COMMENT '对端VPC',
    peer_region VARCHAR(64) DEFAULT '' COMMENT '对端地域',
    bandwidth INT DEFAULT 0 COMMENT '带宽Mbps',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对等连接';

CREATE TABLE IF NOT EXISTS cloud_resource_eni (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    eni_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '弹性网卡ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '网卡名称',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    subnet_id VARCHAR(128) DEFAULT '' COMMENT '所属子网',
    private_ips VARCHAR(512) DEFAULT '' COMMENT '内网IP(逗号分隔)',
    mac VARCHAR(32) DEFAULT '' COMMENT 'MAC地址',
    instance_id VARCHAR(128) DEFAULT '' COMMENT '绑定实例ID',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_eni_id (eni_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ENI弹性网卡';

CREATE TABLE IF NOT EXISTS cloud_resource_direct_connect_gateway (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    dcg_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '专线网关ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '网关名称',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '绑定VPC',
    type VARCHAR(32) DEFAULT '' COMMENT '类型(NAT/非NAT)',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专线网关';

-- ========== 堡垒机资源 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_bastion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    bastion_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '堡垒机实例ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '堡垒机名称',
    spec VARCHAR(64) DEFAULT '' COMMENT '规格',
    license_count INT DEFAULT 0 COMMENT '授权资产数',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    public_ip VARCHAR(64) DEFAULT '' COMMENT '公网IP',
    private_ip VARCHAR(64) DEFAULT '' COMMENT '内网IP',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='堡垒机';
