-- Merge all cloud_resource_* tables into single cloud_resource table
CREATE TABLE cloud_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT,
    provider VARCHAR(50),
    resource_type VARCHAR(50),
    resource_id VARCHAR(255),
    name VARCHAR(255),
    region VARCHAR(100),
    zone VARCHAR(100),
    status VARCHAR(50),
    extra JSON,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_account (account_id),
    INDEX idx_type (resource_type),
    INDEX idx_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Migrate data from each type-specific table to cloud_resource
-- Example for cvm:
-- INSERT INTO cloud_resource (account_id, provider, resource_type, resource_id, name, region, status, extra, create_time, update_time)
-- SELECT a.id, a.provider, 'cvm', c.instance_id, c.name, c.region, c.status,
--        JSON_OBJECT('cpu', c.cpu, 'memory', c.memory, 'public_ip', c.public_ip, 'private_ip', c.private_ip, 'os_name', c.os_name),
--        c.create_time, c.update_time
-- FROM cloud_resource_cvm c
-- JOIN cloud_account a ON c.account_id = a.id;

-- Repeat for each type-specific table...
-- After migration verified, DROP tables:
-- DROP TABLE cloud_resource_cvm, cloud_resource_bms, ...;
