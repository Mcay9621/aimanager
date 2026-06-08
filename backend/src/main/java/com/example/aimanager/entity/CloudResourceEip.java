package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_eip")
public class CloudResourceEip extends BaseResource {
    private String eipId;
    private String name;
    private String publicIp;
    private Integer bandwidth;
    private String instanceId;
}
