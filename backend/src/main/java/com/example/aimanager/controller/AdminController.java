package com.example.aimanager.controller;

import com.example.aimanager.common.Result;
import com.example.aimanager.entity.Role;
import com.example.aimanager.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminController {

    private final RoleService roleService;

    public AdminController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getRoles() {
        List<Role> roles = roleService.list();
        return ResponseEntity.ok(Result.success(roles));
    }

    @PostMapping("/roles")
    public ResponseEntity<?> addRole(@RequestBody Role role) {
        if (role.getName() == null || role.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Result.badRequest("角色名称不能为空"));
        }
        if (role.getCode() == null || role.getCode().isBlank()) {
            return ResponseEntity.badRequest().body(Result.badRequest("角色编码不能为空"));
        }
        roleService.save(role);
        return ResponseEntity.ok(Result.success(Map.of("message", "添加成功")));
    }

    @PutMapping("/roles/{id}")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        roleService.updateById(role);
        return ResponseEntity.ok(Result.success(Map.of("message", "更新成功")));
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        roleService.removeById(id);
        return ResponseEntity.ok(Result.success(Map.of("message", "删除成功")));
    }
}
