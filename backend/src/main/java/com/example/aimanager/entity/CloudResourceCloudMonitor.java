package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_cloud_monitor")
public class CloudResourceCloudMonitor extends BaseResource {
    private String alarmId;
    private String name;
    private String metric;
    private String threshold;
    private Integer statPeriod;
}
