package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_direct_connect_tunnel")
public class CloudResourceDirectConnectTunnel extends BaseResource {
    private String dctId;
    private String name;
    private String dcId;
    private Integer vlan;
    private Integer bandwidth;
}
