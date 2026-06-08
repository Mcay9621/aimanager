package com.example.aimanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimanager.common.Result;
import com.example.aimanager.dto.PasswordChangeRequest;
import com.example.aimanager.dto.ProfileUpdateRequest;
import com.example.aimanager.entity.User;
import com.example.aimanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class ProfileController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        User user = userService.getOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) {
            return ResponseEntity.status(404).body(Result.notFound("用户不存在"));
        }
        user.setPassword(null);
        return ResponseEntity.ok(Result.success(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest request, Authentication auth) {
        User user = userService.getOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) {
            return ResponseEntity.status(404).body(Result.notFound("用户不存在"));
        }

        if (request.getEmail() != null) {
            User existing = userService.getOne(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
            if (existing != null && !existing.getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Result.badRequest("邮箱已被其他用户使用"));
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        userService.updateById(user);
        user.setPassword(null);
        return ResponseEntity.ok(Result.success(user));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request, Authentication auth) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Result.badRequest("两次密码输入不一致"));
        }

        User user = userService.getOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) {
            return ResponseEntity.status(404).body(Result.notFound("用户不存在"));
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Result.badRequest("旧密码错误"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.updateById(user);

        return ResponseEntity.ok(Result.success(Map.of("message", "密码修改成功")));
    }
}
