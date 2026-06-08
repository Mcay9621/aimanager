package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_bms")
public class CloudResourceBms extends BaseResource {
    private String instanceId;
    private String name;
    private Integer cpu;
    private Integer memory;
    private String publicIp;
    private String privateIp;
    private String osName;
    private String vpcId;
    private String subnetId;
    private Integer networkCardCount;
    private String raidConfig;
    private LocalDateTime expireTime;
}
