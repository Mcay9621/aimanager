package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_vpc")
public class CloudResourceVpc extends BaseResource {
    private String vpcId;
    private String name;
    private String cidr;
    private Integer isDefault;
}
