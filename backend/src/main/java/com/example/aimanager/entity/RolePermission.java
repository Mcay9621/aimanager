package com.example.aimanager.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role_permission")
public class RolePermission {
    private Long roleId;
    private Long permissionId;
    private LocalDateTime createTime;
}
