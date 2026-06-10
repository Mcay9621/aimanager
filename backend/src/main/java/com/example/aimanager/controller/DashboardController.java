package com.example.aimanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimanager.common.Result;
import com.example.aimanager.entity.*;
import com.example.aimanager.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class DashboardController {

    private final UserService userService;
    private final AiModelService aiModelService;
    private final AuditLogService auditLogService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final CloudAccountService cloudAccountService;

    public DashboardController(UserService userService, AiModelService aiModelService,
                                AuditLogService auditLogService,
                                ChatSessionService chatSessionService,
                                ChatMessageService chatMessageService,
                                CloudAccountService cloudAccountService) {
        this.userService = userService;
        this.aiModelService = aiModelService;
        this.auditLogService = auditLogService;
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
        this.cloudAccountService = cloudAccountService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long userCount = userService.count();
        long modelCount = aiModelService.count();
        long enabledModelCount = aiModelService.count(
                new LambdaQueryWrapper<AiModel>().eq(AiModel::getEnabled, 1));
        long auditLogCount = auditLogService.count();
        long todayLogCount = auditLogService.count(
                new LambdaQueryWrapper<AuditLog>()
                        .apply("DATE(create_time) = CURDATE()"));
        long sessionCount = chatSessionService.count();
        long messageCount = chatMessageService.count();
        long cloudAccountCount = cloudAccountService.count();
        long enabledCloudAccountCount = cloudAccountService.count(
                new LambdaQueryWrapper<CloudAccount>().eq(CloudAccount::getStatus, 1));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userCount", userCount);
        stats.put("modelCount", modelCount);
        stats.put("enabledModelCount", enabledModelCount);
        stats.put("sessionCount", sessionCount);
        stats.put("messageCount", messageCount);
        stats.put("auditLogCount", auditLogCount);
        stats.put("todayLogCount", todayLogCount);
        stats.put("cloudAccountCount", cloudAccountCount);
        stats.put("enabledCloudAccountCount", enabledCloudAccountCount);
        return ResponseEntity.ok(Result.success(stats));
    }

    @GetMapping("/model-usage")
    public ResponseEntity<?> getModelUsage() {
        // 按模型类型统计会话数
        List<ChatSession> sessions = chatSessionService.list();
        Map<String, Long> usage = sessions.stream()
                .filter(s -> s.getModelName() != null)
                .collect(Collectors.groupingBy(
                        ChatSession::getModelName, LinkedHashMap::new, Collectors.counting()));

        List<Map<String, Object>> result = usage.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/activity")
    public ResponseEntity<?> getActivity() {
        // 最近7天的消息量趋势
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = chatMessageService.count(
                    new LambdaQueryWrapper<ChatMessage>()
                            .apply("DATE(create_time) = {0}", date.toString()));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
            item.put("count", count);
            result.add(item);
        }
        return ResponseEntity.ok(Result.success(result));
    }
}
