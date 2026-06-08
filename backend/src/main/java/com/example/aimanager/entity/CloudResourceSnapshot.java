package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_snapshot")
public class CloudResourceSnapshot extends BaseResource {
    private String snapshotId;
    private String name;
    private String diskId;
    private Integer diskSizeGb;
}
