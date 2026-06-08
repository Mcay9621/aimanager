package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_function")
public class CloudResourceFunction extends BaseResource {
    private String functionId;
    private String name;
    private String runtime;
    private Integer memoryMb;
    private Integer timeoutSec;
    private Integer triggerCount;
    private String vpcId;
    private Integer codeSize;
}
