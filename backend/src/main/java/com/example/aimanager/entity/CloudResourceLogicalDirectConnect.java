package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_logical_direct_connect")
public class CloudResourceLogicalDirectConnect extends BaseResource {
    private String ldcId;
    private String name;
    private String dcId;
    private Integer bandwidth;
    private Integer vlan;
}
