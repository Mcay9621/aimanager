package com.example.aimanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cloud_account")
public class CloudAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String provider;
    private String aliasName;
    private String accessKey;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String accessSecret;
    private Long parentId;
    private String type;
    private String region;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
