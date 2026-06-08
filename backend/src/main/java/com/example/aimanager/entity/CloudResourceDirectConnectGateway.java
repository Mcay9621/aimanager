package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_direct_connect_gateway")
public class CloudResourceDirectConnectGateway extends BaseResource {
    private String dcgId;
    private String name;
    private String vpcId;
    private String type;
}
