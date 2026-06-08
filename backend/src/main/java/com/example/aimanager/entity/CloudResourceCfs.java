package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_cfs")
public class CloudResourceCfs extends BaseResource {
    private String fileSystemId;
    private String name;
    private String protocol;
    private String storageType;
    private Integer sizeGb;
    private Integer usedGb;
    private String mountPoint;
    private String vpcId;
}
