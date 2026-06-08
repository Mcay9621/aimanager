package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_eni")
public class CloudResourceEni extends BaseResource {
    private String eniId;
    private String name;
    private String vpcId;
    private String subnetId;
    private String privateIps;
    private String mac;
    private String instanceId;
}
