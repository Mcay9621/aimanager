package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_subnet")
public class CloudResourceSubnet extends BaseResource {
    private String subnetId;
    private String name;
    private String vpcId;
    private String cidr;
    private String zone;
    private Integer availableIpCount;
}
