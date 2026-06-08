-- 新增14个云资源类型表
-- 部署到已有数据库时，直接执行此文件

-- ========== DNS解析 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_dns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    zone_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'DNS Zone ID',
    domain VARCHAR(255) NOT NULL DEFAULT '' COMMENT '主域名',
    record_count INT DEFAULT 0 COMMENT '解析记录数',
    dns_type VARCHAR(32) DEFAULT '' COMMENT '类型(public/private)',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_domain (domain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DNS解析';

-- ========== SSL证书 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_ssl (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    cert_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '证书ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '证书名称',
    domain VARCHAR(255) DEFAULT '' COMMENT '绑定域名',
    issuer VARCHAR(128) DEFAULT '' COMMENT '颁发机构',
    algorithm VARCHAR(32) DEFAULT '' COMMENT '加密算法(RSA/ECC)',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_cert_id (cert_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SSL证书';

-- ========== WAF Web应用防火墙 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_waf (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    instance_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '实例ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '实例名称',
    mode VARCHAR(32) DEFAULT '' COMMENT '模式(detection/blocking)',
    domain_count INT DEFAULT 0 COMMENT '防护域名数',
    rule_count INT DEFAULT 0 COMMENT '规则数',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WAF Web应用防火墙';

-- ========== DDoS防护 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_ddos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    instance_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '实例ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '实例名称',
    bandwidth INT DEFAULT 0 COMMENT '防护带宽Gbps',
    protection_count INT DEFAULT 0 COMMENT '防护IP数',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DDoS防护';

-- ========== 容器服务(TKE/ACK) ==========
CREATE TABLE IF NOT EXISTS cloud_resource_tke (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    cluster_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '集群ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '集群名称',
    version VARCHAR(32) DEFAULT '' COMMENT '集群版本',
    node_count INT DEFAULT 0 COMMENT '节点数',
    cpu_total INT DEFAULT 0 COMMENT 'CPU总量(核)',
    memory_total INT DEFAULT 0 COMMENT '内存总量GB',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_cluster_id (cluster_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='容器服务(TKE/ACK)';

-- ========== 镜像仓库 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_image_registry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    registry_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '实例ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '实例名称',
    registry_type VARCHAR(32) DEFAULT '' COMMENT '类型(public/private)',
    repo_count INT DEFAULT 0 COMMENT '镜像仓库数',
    storage_gb DECIMAL(12,2) DEFAULT 0 COMMENT '存储用量GB',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='镜像仓库';

-- ========== AS 弹性伸缩 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_auto_scaling (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    scaling_group_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '伸缩组ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '伸缩组名称',
    min_size INT DEFAULT 0 COMMENT '最小实例数',
    max_size INT DEFAULT 0 COMMENT '最大实例数',
    desired_size INT DEFAULT 0 COMMENT '期望实例数',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AS弹性伸缩';

-- ========== 函数计算(SCF/FC) ==========
CREATE TABLE IF NOT EXISTS cloud_resource_function (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    function_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '函数ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '函数名称',
    runtime VARCHAR(32) DEFAULT '' COMMENT '运行时(Python3/Nodejs)',
    memory_mb INT DEFAULT 128 COMMENT '内存MB',
    timeout_sec INT DEFAULT 3 COMMENT '超时时间秒',
    trigger_count INT DEFAULT 0 COMMENT '触发器数量',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    code_size INT DEFAULT 0 COMMENT '代码大小KB',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='函数计算(SCF/FC)';

-- ========== 消息队列 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_mq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    instance_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '实例ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '实例名称',
    topic_count INT DEFAULT 0 COMMENT 'Topic数量',
    queue_type VARCHAR(32) DEFAULT '' COMMENT '类型(kafka/rocketmq/rabbitmq)',
    max_storage_gb INT DEFAULT 0 COMMENT '最大存储GB',
    vpc_id VARCHAR(128) DEFAULT '' COMMENT '所属VPC',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息队列';

-- ========== 日志服务 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_log_service (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    logset_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '日志集ID',
    logset_name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '日志集名称',
    topic_count INT DEFAULT 0 COMMENT '日志主题数',
    storage_gb DECIMAL(12,2) DEFAULT 0 COMMENT '存储用量GB',
    retention_days INT DEFAULT 30 COMMENT '保留天数',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志服务';

-- ========== 云监控(告警策略) ==========
CREATE TABLE IF NOT EXISTS cloud_resource_cloud_monitor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    alarm_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '告警策略ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '策略名称',
    metric VARCHAR(128) DEFAULT '' COMMENT '监控指标(CPU/内存)',
    threshold VARCHAR(64) DEFAULT '' COMMENT '告警阈值',
    stat_period INT DEFAULT 300 COMMENT '统计周期秒',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云监控(告警策略)';

-- ========== 全球加速 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_global_acceleration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    accelerator_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '加速器ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '加速器名称',
    bandwidth INT DEFAULT 0 COMMENT '带宽Mbps',
    concurrent_connections INT DEFAULT 0 COMMENT '最大并发连接数',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    expire_time DATETIME DEFAULT NULL COMMENT '到期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全球加速';

-- ========== KMS 密钥管理服务 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_kms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    key_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '密钥ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '密钥名称',
    algorithm VARCHAR(32) DEFAULT '' COMMENT '加密算法(AES/RSA)',
    key_spec VARCHAR(32) DEFAULT '' COMMENT '密钥规格(256/2048)',
    rotation_enabled TINYINT DEFAULT 0 COMMENT '是否开启轮转',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id),
    INDEX idx_key_id (key_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KMS密钥管理';

-- ========== DTS 数据传输服务 ==========
CREATE TABLE IF NOT EXISTS cloud_resource_dts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL COMMENT '所属子账号ID',
    provider VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'aliyun/tencent',
    task_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '任务ID',
    name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '任务名称',
    source_type VARCHAR(32) DEFAULT '' COMMENT '源类型(mysql/oracle)',
    target_type VARCHAR(32) DEFAULT '' COMMENT '目标类型(mysql/kafka)',
    migration_type VARCHAR(32) DEFAULT '' COMMENT '迁移类型(full/incremental/full+incr)',
    region VARCHAR(64) DEFAULT '' COMMENT '地域',
    status VARCHAR(32) DEFAULT '' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DTS数据传输';
