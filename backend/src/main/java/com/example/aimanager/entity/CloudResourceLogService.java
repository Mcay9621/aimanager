package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_log_service")
public class CloudResourceLogService extends BaseResource {
    private String logsetId;
    private String logsetName;
    private Integer topicCount;
    private BigDecimal storageGb;
    private Integer retentionDays;
}
