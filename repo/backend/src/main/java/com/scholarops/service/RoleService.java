package com.scholarops.service;

import com.scholarops.exception.ConflictException;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.model.entity.*;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.PermissionChangeHistoryRepository;
import com.scholarops.repository.RoleRepository;
import com.scholarops.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionChangeHistoryRepository permissionChangeHistoryRepository;
    private final AuditLogService auditLogService;

    public RoleService(RoleRepository roleRepository,
                       UserRepository userRepository,
                       PermissionChangeHistoryRepository permissionChangeHistoryRepository,
                       AuditLogService auditLogService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.permissionChangeHistoryRepository = permissionChangeHistoryRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    public UserRole assignRole(Long userId, Long roleId, Long assignedById) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        User assignedBy = userRepository.findById(assignedById)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", assignedById));

        // Check if the user already has this role
        Optional<UserRole> existingMapping = user.getUserRoles().stream()
                .filter(ur -> ur.getRole().getId().equals(roleId))
                .findFirst();

        if (existingMapping.isPresent()) {
            throw new ConflictException("User already has role: " + role.getName());
        }

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .assignedBy(assignedBy)
                .build();

        user.getUserRoles().add(userRole);
        userRepository.save(user);

        // Record permission change history
        PermissionChangeHistory history = PermissionChangeHistory.builder()
                .targetUser(user)
                .changedBy(assignedBy)
                .changeType("ROLE_ASSIGN")
                .role(role)
                .oldValue(null)
                .newValue(role.getName())
                .reason("Role assigned by user " + assignedById)
                .build();
        permissionChangeHistoryRepository.save(history);

        auditLogService.log(
                assignedById,
                AuditAction.ROLE_ASSIGN,
                "UserRole",
                userId,
                String.format("Assigned role '%s' to user '%s'", role.getName(), user.getUsername()),
                null,
                null
        );

        logger.info("Role '{}' assigned to user '{}' by userId={}", role.getName(), user.getUsername(), assignedById);
        return userRole;
    }

    @Transactional
    public void revokeRole(Long userId, Long roleId, Long revokedById) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        User revokedBy = userRepository.findById(revokedById)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", revokedById));

        UserRole userRole = user.getUserRoles().stream()
                .filter(ur -> ur.getRole().getId().equals(roleId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User does not have role: " + role.getName()));

        user.getUserRoles().remove(userRole);
        userRepository.save(user);

        // Record permission change history
        PermissionChangeHistory history = PermissionChangeHistory.builder()
                .targetUser(user)
                .changedBy(revokedBy)
                .changeType("ROLE_REVOKE")
                .role(role)
                .oldValue(role.getName())
                .newValue(null)
                .reason("Role revoked by user " + revokedById)
                .build();
        permissionChangeHistoryRepository.save(history);

        auditLogService.log(
                revokedById,
                AuditAction.ROLE_REVOKE,
                "UserRole",
                userId,
                String.format("Revoked role '%s' from user '%s'", role.getName(), user.getUsername()),
                null,
                null
        );

        logger.info("Role '{}' revoked from user '{}' by userId={}", role.getName(), user.getUsername(), revokedById);
    }
}
