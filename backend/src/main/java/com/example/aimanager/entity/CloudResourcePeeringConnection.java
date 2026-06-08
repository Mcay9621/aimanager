package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_peering_connection")
public class CloudResourcePeeringConnection extends BaseResource {
    private String pcId;
    private String name;
    private String vpcId;
    private String peerVpcId;
    private String peerRegion;
    private Integer bandwidth;
}
