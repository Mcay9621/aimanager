package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_direct_connect")
public class CloudResourceDirectConnect extends BaseResource {
    private String dcId;
    private String name;
    private String circuitCode;
    private Integer bandwidth;
    private String providerName;
}
