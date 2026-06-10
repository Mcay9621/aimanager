package com.example.aimanager.config;

import com.example.aimanager.common.LogAudit;
import com.example.aimanager.entity.AuditLog;
import com.example.aimanager.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    public AuditLogAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @AfterReturning("@annotation(com.example.aimanager.common.LogAudit)")
    public void logAnnotatedOperation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogAudit annotation = method.getAnnotation(LogAudit.class);

        String action = annotation.action();
        String target = annotation.target();
        String detail = annotation.detail();
        String targetId = extractId(joinPoint.getArgs());

        saveLog(action, target, targetId, detail);
    }

    @AfterReturning(pointcut = "execution(* com.example.aimanager.controller.AuthController.login(..))", returning = "result")
    public void logLogin(JoinPoint joinPoint, Object result) {
        if (result instanceof java.util.Map<?, ?> map && map.containsKey("token")) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> loginReq = (java.util.Map<String, Object>) joinPoint.getArgs()[0];
            String username = (String) loginReq.getOrDefault("username", "unknown");
            String loginType = (String) loginReq.getOrDefault("loginType", "front");
            saveLog("LOGIN", "Auth", username, loginType + "登录");
        }
    }

    private void saveLog(String action, String target, String targetId, String detail) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
                ? auth.getName() : "anonymous";

        AuditLog log = new AuditLog();
        log.setUsername(username);
        log.setAction(action);
        log.setTarget(target);
        log.setTargetId(targetId);
        log.setDetail(detail);

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            log.setIp(getClientIp(request));
        }

        auditLogService.save(log);
    }

    private String extractId(Object[] args) {
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Long id) return String.valueOf(id);
                if (arg instanceof Integer i) return String.valueOf(i);
                if (arg instanceof String s && !s.isEmpty()) return s;
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
