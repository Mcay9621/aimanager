package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_image_registry")
public class CloudResourceImageRegistry extends BaseResource {
    private String registryId;
    private String name;
    private String registryType;
    private Integer repoCount;
    private BigDecimal storageGb;
}
