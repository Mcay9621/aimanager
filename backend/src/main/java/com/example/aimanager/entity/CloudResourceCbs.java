package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_cbs")
public class CloudResourceCbs extends BaseResource {
    private String diskId;
    private String name;
    private String diskType;
    private Integer sizeGb;
    private String instanceId;
    private String zone;
    private LocalDateTime expireTime;
}
