package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_mq")
public class CloudResourceMq extends BaseResource {
    private String instanceId;
    private String name;
    private Integer topicCount;
    private String queueType;
    private Integer maxStorageGb;
    private String vpcId;
}
