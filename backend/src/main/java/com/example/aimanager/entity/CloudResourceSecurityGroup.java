package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_security_group")
public class CloudResourceSecurityGroup extends BaseResource {
    private String securityGroupId;
    private String name;
    private String vpcId;
    private String description;
}
