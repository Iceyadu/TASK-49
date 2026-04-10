package com.scholarops.repository;

import com.scholarops.model.entity.PermissionChangeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionChangeHistoryRepository extends JpaRepository<PermissionChangeHistory, Long> {

    Page<PermissionChangeHistory> findByTargetUserId(Long targetUserId, Pageable pageable);

    Page<PermissionChangeHistory> findByChangedById(Long changedById, Pageable pageable);

    Page<PermissionChangeHistory> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId, Pageable pageable);
}
