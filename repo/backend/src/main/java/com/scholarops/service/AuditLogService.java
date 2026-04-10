package com.scholarops.service;

import com.scholarops.model.entity.AuditLog;
import com.scholarops.model.entity.PermissionChangeHistory;
import com.scholarops.model.entity.User;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.AuditLogRepository;
import com.scholarops.repository.PermissionChangeHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final PermissionChangeHistoryRepository permissionChangeHistoryRepository;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           PermissionChangeHistoryRepository permissionChangeHistoryRepository) {
        this.auditLogRepository = auditLogRepository;
        this.permissionChangeHistoryRepository = permissionChangeHistoryRepository;
    }

    @Transactional
    public AuditLog log(Long userId, AuditAction action, String entityType, Long entityId,
                        String details, String ipAddress, String workstationId) {
        AuditLog auditLog = AuditLog.builder()
                .user(userId != null ? User.builder().id(userId).build() : null)
                .action(action.name())
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress)
                .workstationId(workstationId)
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        logger.info("Audit log created: action={}, entityType={}, entityId={}, userId={}",
                action, entityType, entityId, userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable, String action, Long userId,
                                        LocalDateTime start, LocalDateTime end) {
        if (action != null && userId != null && start != null && end != null) {
            return auditLogRepository.findByActionAndUserIdAndCreatedAtBetween(
                    action, userId, start, end, pageable);
        } else if (action != null && userId != null) {
            return auditLogRepository.findByActionAndUserId(action, userId, pageable);
        } else if (action != null && start != null && end != null) {
            return auditLogRepository.findByActionAndCreatedAtBetween(action, start, end, pageable);
        } else if (userId != null && start != null && end != null) {
            return auditLogRepository.findByUserIdAndCreatedAtBetween(userId, start, end, pageable);
        } else if (action != null) {
            return auditLogRepository.findByAction(action, pageable);
        } else if (userId != null) {
            return auditLogRepository.findByUserId(userId, pageable);
        } else if (start != null && end != null) {
            return auditLogRepository.findByCreatedAtBetween(start, end, pageable);
        }
        return auditLogRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<PermissionChangeHistory> getPermissionChangeHistory(Pageable pageable, Long targetUserId) {
        return permissionChangeHistoryRepository.findByTargetUserIdOrderByCreatedAtDesc(targetUserId, pageable);
    }
}
