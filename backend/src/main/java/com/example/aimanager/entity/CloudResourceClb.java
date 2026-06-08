package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_clb")
public class CloudResourceClb extends BaseResource {
    private String clbId;
    private String name;
    private String type;
    private String address;
    private String vpcId;
    private String subnetId;
}
