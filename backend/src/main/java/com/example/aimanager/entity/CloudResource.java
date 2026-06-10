package com.example.aimanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cloud_resource")
public class CloudResource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private String provider;
    private String resourceType;
    private String resourceId;
    private String name;
    private String region;
    private String zone;
    private String status;
    private String extra;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
