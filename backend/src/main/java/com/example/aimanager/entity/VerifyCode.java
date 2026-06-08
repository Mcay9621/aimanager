package com.example.aimanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_verify_code")
public class VerifyCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String target;
    private String code;
    private Integer type;
    private Integer used;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
