package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_dts")
public class CloudResourceDts extends BaseResource {
    private String taskId;
    private String name;
    private String sourceType;
    private String targetType;
    private String migrationType;
}
