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

    // ========== 保留旧 pointcut 表达式作为过渡，逐步迁移 @LogAudit 注解 ==========

    @AfterReturning(pointcut = "execution(* com.example.aimanager.controller.UserController.*(..)) && !execution(* com.example.aimanager.controller.UserController.getUsers(..)) && !execution(* com.example.aimanager.controller.UserController.getUser(..)) && !execution(* com.example.aimanager.controller.UserController.getUserCount(..)) && !execution(* com.example.aimanager.controller.UserController.getUserRoles(..)) && !execution(* com.example.aimanager.controller.UserController.updateUserRoles(..))", returning = "result")
    public void logUserOperation(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String action = switch (methodName) {
            case "addUser" -> "CREATE";
            case "updateUser" -> "UPDATE";
            case "deleteUser" -> "DELETE";
            case "toggleStatus" -> "UPDATE";
            default -> "OTHER";
        };
        saveLog(action, "User", extractId(joinPoint.getArgs()), "用户管理操作");
    }

    @AfterReturning(pointcut = "execution(* com.example.aimanager.controller.AdminController.*(..)) && !execution(* com.example.aimanager.controller.AdminController.getRoles(..))", returning = "result")
    public void logRoleOperation(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String action = switch (methodName) {
            case "addRole" -> "CREATE";
            case "updateRole" -> "UPDATE";
            case "deleteRole" -> "DELETE";
            default -> "OTHER";
        };
        saveLog(action, "Role", extractId(joinPoint.getArgs()), "角色管理操作");
    }

    @AfterReturning(pointcut = "execution(* com.example.aimanager.controller.ModelController.addModel(..)) || execution(* com.example.aimanager.controller.ModelController.updateModel(..)) || execution(* com.example.aimanager.controller.ModelController.deleteModel(..))", returning = "result")
    public void logModelOperation(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String action = switch (methodName) {
            case "addModel" -> "CREATE";
            case "updateModel" -> "UPDATE";
            case "deleteModel" -> "DELETE";
            default -> "OTHER";
        };
        saveLog(action, "Model", extractId(joinPoint.getArgs()), "模型管理操作");
    }

    @AfterReturning(pointcut = "execution(* com.example.aimanager.controller.CloudResourceController.startInstance(..)) || execution(* com.example.aimanager.controller.CloudResourceController.stopInstance(..)) || execution(* com.example.aimanager.controller.CloudResourceController.restartInstance(..))", returning = "result")
    public void logCloudOperation(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String action = switch (methodName) {
            case "startInstance" -> "START";
            case "stopInstance" -> "STOP";
            case "restartInstance" -> "RESTART";
            default -> "OTHER";
        };
        String instanceId = extractId(joinPoint.getArgs());
        saveLog(action, "CloudInstance", instanceId, "云实例 " + action + " 操作");
    }

    @AfterReturning(pointcut = "execution(* com.example.aimanager.controller.CloudAccountController.addAccount(..)) || execution(* com.example.aimanager.controller.CloudAccountController.updateAccount(..)) || execution(* com.example.aimanager.controller.CloudAccountController.deleteAccount(..))", returning = "result")
    public void logCloudAccountOperation(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String action = switch (methodName) {
            case "addAccount" -> "CREATE";
            case "updateAccount" -> "UPDATE";
            case "deleteAccount" -> "DELETE";
            default -> "OTHER";
        };
        saveLog(action, "CloudAccount", extractId(joinPoint.getArgs()), "云账号管理操作");
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
