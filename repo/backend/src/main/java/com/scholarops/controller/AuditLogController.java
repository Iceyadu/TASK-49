package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.entity.AuditLog;
import com.scholarops.model.entity.PermissionChangeHistory;
import com.scholarops.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMINISTRATOR') and hasPermission(null, 'AUDIT_VIEW')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            Pageable pageable,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.getAuditLogs(pageable, action, userId, startDate, endDate)));
    }

    @GetMapping("/permission-change-history")
    @PreAuthorize("hasRole('ADMINISTRATOR') and hasPermission(null, 'AUDIT_VIEW')")
    public ResponseEntity<ApiResponse<Page<PermissionChangeHistory>>> getPermissionChangeHistory(
            Pageable pageable, @RequestParam(required = false) Long targetUserId) {
        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.getPermissionChangeHistory(pageable, targetUserId)));
    }
}
