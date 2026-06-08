package com.example.aimanager.controller;

import com.example.aimanager.common.Result;
import com.example.aimanager.dto.CloudAccountRequest;
import com.example.aimanager.entity.CloudAccount;
import com.example.aimanager.service.CloudAccountService;
import com.example.aimanager.service.cloud.CloudClientRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/cloud/accounts")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class CloudAccountController {

    private final CloudAccountService cloudAccountService;
    private final CloudClientRegistry cloudClientRegistry;

    public CloudAccountController(CloudAccountService cloudAccountService,
                                  CloudClientRegistry cloudClientRegistry) {
        this.cloudAccountService = cloudAccountService;
        this.cloudClientRegistry = cloudClientRegistry;
    }

    @GetMapping
    public ResponseEntity<?> getAccounts(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long parentId) {
        List<CloudAccount> accounts;
        if ("master".equals(type)) {
            accounts = cloudAccountService.getMasters();
        } else if (parentId != null) {
            accounts = cloudAccountService.getSubsByParentId(parentId);
        } else {
            accounts = cloudAccountService.list();
        }
        accounts.forEach(a -> a.setAccessSecret(null));
        return ResponseEntity.ok(Result.success(accounts));
    }

    @PostMapping
    public ResponseEntity<?> addAccount(@RequestBody CloudAccountRequest request) {
        String type = request.getType() != null ? request.getType() : "sub";
        if ("sub".equals(type)) {
            if (request.getProvider() == null || request.getProvider().isEmpty()) {
                return ResponseEntity.badRequest().body(Result.badRequest("子账号必须选择云厂商"));
            }
            if (request.getAccessKey() == null || request.getAccessKey().isEmpty()) {
                return ResponseEntity.badRequest().body(Result.badRequest("子账号必须填写 AccessKey ID"));
            }
            if (request.getAccessSecret() == null || request.getAccessSecret().isEmpty()) {
                return ResponseEntity.badRequest().body(Result.badRequest("子账号必须填写 AccessKey Secret"));
            }
            if (request.getParentId() == null) {
                return ResponseEntity.badRequest().body(Result.badRequest("子账号必须选择所属主账号"));
            }
        }

        CloudAccount account = new CloudAccount();
        account.setType(type);
        account.setParentId("sub".equals(type) ? request.getParentId() : null);
        account.setProvider(request.getProvider() != null ? request.getProvider() : "");
        account.setAliasName(request.getAliasName());
        account.setAccessKey(request.getAccessKey() != null ? request.getAccessKey() : "");
        account.setAccessSecret(request.getAccessSecret() != null ? request.getAccessSecret() : "");
        account.setRegion(request.getRegion() != null ? request.getRegion() : "");
        account.setStatus(1);
        cloudAccountService.save(account);
        account.setAccessSecret(null);
        return ResponseEntity.ok(Result.success(account));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAccount(@PathVariable Long id, @RequestBody CloudAccountRequest request) {
        CloudAccount account = cloudAccountService.getById(id);
        if (account == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("账号不存在"));
        }
        String type = request.getType() != null ? request.getType() : account.getType();
        account.setType(type);
        if ("master".equals(type)) {
            account.setParentId(null);
            account.setProvider("");
            account.setAccessKey("");
            account.setAccessSecret("");
            account.setRegion("");
        } else {
            if (request.getParentId() != null) account.setParentId(request.getParentId());
            if (request.getProvider() != null) account.setProvider(request.getProvider());
            if (request.getAccessKey() != null) account.setAccessKey(request.getAccessKey());
            if (request.getAccessSecret() != null && !request.getAccessSecret().isEmpty()) {
                account.setAccessSecret(request.getAccessSecret());
            }
            if (request.getRegion() != null) account.setRegion(request.getRegion());
        }
        if (request.getAliasName() != null) account.setAliasName(request.getAliasName());
        cloudAccountService.updateById(account);
        account.setAccessSecret(null);
        return ResponseEntity.ok(Result.success(account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long id) {
        CloudAccount account = cloudAccountService.getById(id);
        if (account == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("账号不存在"));
        }
        if ("master".equals(account.getType()) && cloudAccountService.hasSubAccounts(id)) {
            return ResponseEntity.badRequest().body(Result.badRequest("该主账号下存在子账号，请先删除所有子账号后再操作"));
        }
        cloudAccountService.removeById(id);
        return ResponseEntity.ok(Result.success(Map.of("message", "删除成功")));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<?> testConnection(@PathVariable Long id) {
        CloudAccount account = cloudAccountService.getDecryptedById(id);
        if (account == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("账号不存在"));
        }

        var client = cloudClientRegistry.getClient(account.getProvider());
        if (client == null) {
            return ResponseEntity.ok(Result.success(Map.of("message", "连接成功（模拟）", "connected", true)));
        }

        boolean connected = client.testConnection(account);
        if (connected) {
            return ResponseEntity.ok(Result.success(Map.of("message", "连接成功", "connected", true)));
        } else {
            return ResponseEntity.ok(Result.success(Map.of("message", "连接失败：请检查 AccessKey 和 Secret 是否正确", "connected", false)));
        }
    }
}
