package com.scholarops.repository;

import com.scholarops.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<AuditLog> findByActionAndUserId(String action, Long userId, Pageable pageable);

    Page<AuditLog> findByActionAndCreatedAtBetween(String action, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<AuditLog> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<AuditLog> findByActionAndUserIdAndCreatedAtBetween(String action, Long userId, LocalDateTime start, LocalDateTime end, Pageable pageable);
}
