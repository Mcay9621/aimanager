package com.example.aimanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimanager.common.Result;
import com.example.aimanager.entity.CloudAccount;
import com.example.aimanager.entity.User;
import com.example.aimanager.service.CloudAccountService;
import com.example.aimanager.service.CloudResourceService;
import com.example.aimanager.service.UserService;
import com.example.aimanager.service.cloud.CloudClientRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.aimanager.common.LogAudit;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/cloud/resources")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class CloudResourceController {

    private final CloudAccountService cloudAccountService;
    private final CloudResourceService cloudResourceService;
    private final UserService userService;
    private final CloudClientRegistry cloudClientRegistry;

    public CloudResourceController(CloudAccountService cloudAccountService,
                                   CloudResourceService cloudResourceService,
                                   UserService userService,
                                   CloudClientRegistry cloudClientRegistry) {
        this.cloudAccountService = cloudAccountService;
        this.cloudResourceService = cloudResourceService;
        this.userService = userService;
        this.cloudClientRegistry = cloudClientRegistry;
    }

    @GetMapping
    public ResponseEntity<?> getResources(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long masterAccountId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long accountId) {

        // 权限检查
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN") || g.getAuthority().equals("ROLE_SUPER_ADMIN"));

        Long targetMasterId = masterAccountId;
        if (!isAdmin) {
            String username = auth.getName();
            User currentUser = userService.getOne(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (currentUser != null && currentUser.getMasterAccountId() != null) {
                targetMasterId = currentUser.getMasterAccountId();
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("resources", Collections.emptyList());
                result.put("stats", Map.of("total", 0L, "running", 0L, "stopped", 0L, "error", 0L));
                return ResponseEntity.ok(Result.success(result));
            }
        }

        List<CloudAccount> accounts;
        if (targetMasterId != null) {
            accounts = cloudAccountService.getSubsByParentId(targetMasterId);
        } else {
            accounts = cloudAccountService.lambdaQuery().eq(CloudAccount::getType, "sub").list();
        }

        if (accountId != null) {
            accounts = accounts.stream().filter(a -> a.getId().equals(accountId)).toList();
        }

        Map<Long, String> masterAliasMap = new HashMap<>();
        cloudAccountService.getMasters().forEach(m -> masterAliasMap.put(m.getId(), m.getAliasName()));

        List<Map<String, Object>> allResources = cloudResourceService.getResourcesByAccounts(accounts, provider);

        Map<Long, Long> accountToMaster = new HashMap<>();
        for (CloudAccount acc : accounts) {
            if (acc.getParentId() != null) {
                accountToMaster.put(acc.getId(), acc.getParentId());
            }
        }
        for (Map<String, Object> res : allResources) {
            String alias = (String) res.get("accountAlias");
            for (CloudAccount acc : accounts) {
                if (acc.getAliasName().equals(alias)) {
                    Long masterId = acc.getParentId();
                    if (masterId != null) {
                        String masterAlias = masterAliasMap.get(masterId);
                        if (masterAlias != null) res.put("masterAlias", masterAlias);
                    }
                    break;
                }
            }
        }

        if (type != null && !type.isEmpty()) {
            allResources = allResources.stream()
                    .filter(r -> type.equals(r.get("resourceType")))
                    .collect(Collectors.toList());
        }

        String finalRegion = region;
        String finalStatus = status;
        String finalKeyword = keyword;
        List<Map<String, Object>> filtered = allResources.stream()
                .filter(r -> finalRegion == null || finalRegion.isEmpty() || r.get("region").equals(finalRegion))
                .filter(r -> finalStatus == null || finalStatus.isEmpty() || r.get("status").equals(finalStatus))
                .filter(r -> finalKeyword == null || finalKeyword.isEmpty()
                        || ((String) r.get("name")).toLowerCase().contains(finalKeyword.toLowerCase())
                        || ((String) r.getOrDefault("publicIp", "")).contains(finalKeyword))
                .collect(Collectors.toList());

        long total = filtered.size();
        long running = filtered.stream().filter(r -> "running".equals(r.get("status")) || "active".equals(r.get("status"))).count();
        long stopped = filtered.stream().filter(r -> "stopped".equals(r.get("status"))).count();
        long error = filtered.stream().filter(r -> "error".equals(r.get("status"))).count();

        Map<String, Object> result = new HashMap<>();
        result.put("resources", filtered);
        result.put("stats", Map.of("total", total, "running", running, "stopped", stopped, "error", error));
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getResourceDetail(@PathVariable String id) {
        Map<String, Object> detail = cloudResourceService.getResourceDetail(id);
        if (detail != null) {
            return ResponseEntity.ok(Result.success(detail));
        }
        return ResponseEntity.ok(Result.success(Map.of("message", "资源不存在")));
    }

    @PostMapping("/{id}/start")
    @LogAudit(action = "START", target = "CloudInstance", detail = "startInstance")
    public ResponseEntity<?> startInstance(@PathVariable String id) {
        return executeInstanceAction(id, "start");
    }

    @PostMapping("/{id}/stop")
    @LogAudit(action = "STOP", target = "CloudInstance", detail = "stopInstance")
    public ResponseEntity<?> stopInstance(@PathVariable String id) {
        return executeInstanceAction(id, "stop");
    }

    @PostMapping("/{id}/restart")
    @LogAudit(action = "RESTART", target = "CloudInstance", detail = "restartInstance")
    public ResponseEntity<?> restartInstance(@PathVariable String id) {
        return executeInstanceAction(id, "restart");
    }

    private ResponseEntity<?> executeInstanceAction(String instanceId, String action) {
        List<CloudAccount> accounts = cloudAccountService.lambdaQuery().eq(CloudAccount::getType, "sub").list();
        boolean executed = false;

        for (CloudAccount account : accounts) {
            if (account.getStatus() != 1) continue;
            try {
                CloudAccount decrypted = cloudAccountService.getDecryptedById(account.getId());
                var client = cloudClientRegistry.getClient(account.getProvider());
                if (client == null) continue;

                switch (action) {
                    case "start" -> client.startInstance(decrypted, instanceId);
                    case "stop" -> client.stopInstance(decrypted, instanceId);
                    case "restart" -> client.rebootInstance(decrypted, instanceId);
                }
                executed = true;
                break;
            } catch (Exception ignored) {
            }
        }

        if (executed) {
            return ResponseEntity.ok(Result.success(Map.of("message", switch (action) {
                case "start" -> "启动";
                case "stop" -> "停止";
                case "restart" -> "重启";
                default -> action;
            } + "指令已发送", "instanceId", instanceId)));
        }
        return ResponseEntity.ok(Result.success(Map.of("message", "未找到可操作的账号", "instanceId", instanceId)));
    }

}
