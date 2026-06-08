package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_nat_gateway")
public class CloudResourceNatGateway extends BaseResource {
    private String natId;
    private String name;
    private String vpcId;
    private String publicIps;
}
