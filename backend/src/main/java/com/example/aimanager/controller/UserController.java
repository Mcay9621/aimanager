package com.example.aimanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimanager.common.Result;
import com.example.aimanager.entity.Role;
import com.example.aimanager.entity.User;
import com.example.aimanager.entity.UserRole;
import com.example.aimanager.mapper.UserRoleMapper;
import com.example.aimanager.service.RoleService;
import com.example.aimanager.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleMapper userRoleMapper;
    private final RoleService roleService;

    public UserController(UserService userService, PasswordEncoder passwordEncoder,
                          UserRoleMapper userRoleMapper, RoleService roleService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.userRoleMapper = userRoleMapper;
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<?> getUsers() {
        List<User> users = userService.list(
                new LambdaQueryWrapper<User>()
                        .orderByDesc(User::getCreateTime)
                        .select(User.class, info -> !"password".equals(info.getColumn()))
        );
        return ResponseEntity.ok(Result.success(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return ResponseEntity.status(404).body(Result.notFound("用户不存在"));
        }
        user.setPassword(null);
        return ResponseEntity.ok(Result.success(user));
    }

    @PostMapping
    public ResponseEntity<?> addUser(@RequestBody User user) {
        if (userService.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body(Result.badRequest("用户名已存在"));
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        userService.save(user);
        return ResponseEntity.ok(Result.success(Map.of("message", "添加成功")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userService.updateById(user);
        return ResponseEntity.ok(Result.success(Map.of("message", "更新成功")));
    }

    @PutMapping("/{id}/master-account")
    public ResponseEntity<?> updateMasterAccount(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long masterAccountId = body.get("masterAccountId");
        User user = userService.getById(id);
        if (user == null) {
            return ResponseEntity.status(404).body(Result.notFound("用户不存在"));
        }
        user.setMasterAccountId(masterAccountId);
        userService.updateById(user);
        return ResponseEntity.ok(Result.success(Map.of("message", "主账号分配成功", "masterAccountId", masterAccountId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return ResponseEntity.ok(Result.success(Map.of("message", "删除成功")));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return ResponseEntity.status(404).body(Result.notFound("用户不存在"));
        }
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userService.updateById(user);
        return ResponseEntity.ok(Result.success(Map.of("message", "状态更新成功", "status", user.getStatus())));
    }

    @GetMapping("/count")
    public ResponseEntity<?> getUserCount() {
        long count = userService.count();
        return ResponseEntity.ok(Result.success(Map.of("count", count)));
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<?> getUserRoles(@PathVariable Long id) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, id));
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
        return ResponseEntity.ok(Result.success(Map.of("roleIds", roleIds)));
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<?> updateUserRoles(@PathVariable Long id, @RequestBody Map<String, List<Number>> request) {
        List<Number> newRoleIds = request.get("roleIds");
        if (newRoleIds == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("roleIds 不能为空"));
        }

        userRoleMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, id));

        for (Number roleIdNum : newRoleIds) {
            UserRole userRole = new UserRole();
            userRole.setUserId(id);
            userRole.setRoleId(roleIdNum.longValue());
            userRoleMapper.insert(userRole);
        }

        return ResponseEntity.ok(Result.success(Map.of("message", "角色分配更新成功")));
    }
}
