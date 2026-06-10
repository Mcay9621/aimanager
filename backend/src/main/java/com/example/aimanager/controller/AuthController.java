package com.example.aimanager.controller;

import com.example.aimanager.common.Result;
import com.example.aimanager.dto.LoginRequest;
import com.example.aimanager.entity.User;
import com.example.aimanager.service.UserService;
import com.example.aimanager.service.VerifyCodeService;
import com.example.aimanager.util.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final VerifyCodeService verifyCodeService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, UserService userService,
                          VerifyCodeService verifyCodeService, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.verifyCodeService = verifyCodeService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        var authorities = authentication.getAuthorities();
        boolean isAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if ("admin".equals(loginRequest.getLoginType()) && !isAdmin) {
            return ResponseEntity.badRequest().body(Result.badRequest("无后台管理权限"));
        }

        String token = jwtUtil.generateToken(loginRequest.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(loginRequest.getUsername());
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("refreshToken", refreshToken);
        response.put("username", loginRequest.getUsername());
        response.put("isAdmin", isAdmin);
        response.put("authorities", authorities.stream().map(a -> a.getAuthority()).toList());
        return ResponseEntity.ok(Result.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("refreshToken 不能为空"));
        }
        if (!jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.badRequest().body(Result.badRequest("refreshToken 无效或已过期"));
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        String newToken = jwtUtil.generateToken(username);
        String newRefreshToken = jwtUtil.generateRefreshToken(username);

        Map<String, Object> response = new HashMap<>();
        response.put("token", newToken);
        response.put("refreshToken", newRefreshToken);
        return ResponseEntity.ok(Result.success(response));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userService.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body(Result.badRequest("用户名已存在"));
        }
        if (user.getEmail() != null && userService.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(Result.badRequest("邮箱已被使用"));
        }
        try {
            userService.register(user);
            return ResponseEntity.ok(Result.success(Map.of("message", "注册成功")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Result.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, Object> request) {
        String target = (String) request.get("target");
        Integer type = (Integer) request.get("type");
        String code = verifyCodeService.generateCode(target, type);
        log.info("验证码发送 - target: {}, type: {}, code: {}", target, type, code);
        return ResponseEntity.ok(Result.success(Map.of("message", "验证码已发送")));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String target = request.get("target");
        String code = request.get("code");
        Integer type = Integer.parseInt(request.get("type"));
        boolean valid = verifyCodeService.verifyCode(target, code, type);
        if (valid) {
            return ResponseEntity.ok(Result.success(Map.of("valid", true, "message", "验证成功")));
        }
        return ResponseEntity.badRequest().body(Result.badRequest("验证码错误或已过期"));
    }

    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(Authentication authentication) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", authentication.getName());
        userInfo.put("authorities", authentication.getAuthorities());
        return ResponseEntity.ok(Result.success(userInfo));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Result.success(Map.of("message", "登出成功")));
    }
}
