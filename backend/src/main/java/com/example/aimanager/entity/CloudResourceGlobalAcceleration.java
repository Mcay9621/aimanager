package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_global_acceleration")
public class CloudResourceGlobalAcceleration extends BaseResource {
    private String acceleratorId;
    private String name;
    private Integer bandwidth;
    private Integer concurrentConnections;
    private LocalDateTime expireTime;
}
