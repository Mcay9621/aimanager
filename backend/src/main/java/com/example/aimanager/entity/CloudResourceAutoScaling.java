package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_auto_scaling")
public class CloudResourceAutoScaling extends BaseResource {
    private String scalingGroupId;
    private String name;
    private Integer minSize;
    private Integer maxSize;
    private Integer desiredSize;
    private String vpcId;
}
