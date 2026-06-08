package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_ddos")
public class CloudResourceDdos extends BaseResource {
    private String instanceId;
    private String name;
    private Integer bandwidth;
    private Integer protectionCount;
    private LocalDateTime expireTime;
}
