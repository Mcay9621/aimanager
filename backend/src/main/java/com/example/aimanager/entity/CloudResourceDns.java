package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_dns")
public class CloudResourceDns extends BaseResource {
    private String zoneId;
    private String domain;
    private Integer recordCount;
    private String dnsType;
}
