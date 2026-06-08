package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_tke")
public class CloudResourceTke extends BaseResource {
    private String clusterId;
    private String name;
    private String version;
    private Integer nodeCount;
    private Integer cpuTotal;
    private Integer memoryTotal;
    private String vpcId;
    private LocalDateTime expireTime;
}
