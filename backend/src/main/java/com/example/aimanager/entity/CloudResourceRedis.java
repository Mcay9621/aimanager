package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_redis")
public class CloudResourceRedis extends BaseResource {
    private String instanceId;
    private String name;
    private String engineVersion;
    private String spec;
    private Integer storageGb;
    private String networkType;
    private LocalDateTime expireTime;
}
