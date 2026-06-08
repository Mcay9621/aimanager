package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_bastion")
public class CloudResourceBastion extends BaseResource {
    private String bastionId;
    private String name;
    private String spec;
    private Integer licenseCount;
    private String vpcId;
    private String publicIp;
    private String privateIp;
    private LocalDateTime expireTime;
}
