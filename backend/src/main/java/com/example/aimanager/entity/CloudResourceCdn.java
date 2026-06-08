package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_cdn")
public class CloudResourceCdn extends BaseResource {
    private String domain;
    private String cname;
    private String name;
    private String type;
}
