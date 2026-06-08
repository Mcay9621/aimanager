package com.example.aimanager.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cloud_resource_ssl")
public class CloudResourceSsl extends BaseResource {
    private String certId;
    private String name;
    private String domain;
    private String issuer;
    private String algorithm;
    private LocalDateTime expireTime;
}
