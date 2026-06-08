package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_cos")
public class CloudResourceCos extends BaseResource {
    private String bucketName;
    private String name;
    private String storageClass;
    private Integer publicAccess;
    private Integer objectCount;
    private java.math.BigDecimal sizeGb;
}
