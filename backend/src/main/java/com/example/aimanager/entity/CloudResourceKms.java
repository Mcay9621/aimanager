package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_kms")
public class CloudResourceKms extends BaseResource {
    private String keyId;
    private String name;
    private String algorithm;
    private String keySpec;
    private Integer rotationEnabled;
}
