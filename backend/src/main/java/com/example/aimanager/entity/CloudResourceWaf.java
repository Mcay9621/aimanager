package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_waf")
public class CloudResourceWaf extends BaseResource {
    private String instanceId;
    private String name;
    private String mode;
    private Integer domainCount;
    private Integer ruleCount;
    private LocalDateTime expireTime;
}
