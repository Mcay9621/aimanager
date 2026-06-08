package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_route_table")
public class CloudResourceRouteTable extends BaseResource {
    private String routeTableId;
    private String name;
    private String vpcId;
    private Integer isDefault;
}
