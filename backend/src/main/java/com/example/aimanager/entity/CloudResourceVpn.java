package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_vpn")
public class CloudResourceVpn extends BaseResource {
    private String vpnId;
    private String name;
    private String vpcId;
    private String publicIp;
    private String peerAddress;
}
