package com.example.aimanager.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_role")
public class UserRole {
    private Long userId;
    private Long roleId;
    private LocalDateTime createTime;
}
