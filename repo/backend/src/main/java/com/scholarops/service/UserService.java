package com.scholarops.service;

import com.scholarops.exception.ConflictException;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.exception.UnauthorizedException;
import com.scholarops.model.dto.AdminPasswordResetRequest;
import com.scholarops.model.dto.PasswordResetRequest;
import com.scholarops.model.dto.UserCreateRequest;
import com.scholarops.model.dto.UserUpdateRequest;
import com.scholarops.model.entity.User;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.UserRepository;
import com.scholarops.security.PasswordPolicyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final AuditLogService auditLogService;
    private final RoleService roleService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordPolicyValidator passwordPolicyValidator,
                       AuditLogService auditLogService,
                       RoleService roleService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.auditLogService = auditLogService;
        this.roleService = roleService;
    }

    @Transactional
    public User createUser(UserCreateRequest request, Long currentUserId) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email '" + request.getEmail() + "' is already registered");
        }

        passwordPolicyValidator.validate(request.getPassword());

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .fullName(request.getFullName())
                .enabled(true)
                .accountLocked(false)
                .build();

        User savedUser = userRepository.save(user);

        auditLogService.log(
                currentUserId,
                AuditAction.USER_CREATE,
                "User",
                savedUser.getId(),
                "Created user: " + savedUser.getUsername(),
                null,
                null
        );

        logger.info("User '{}' created by userId={}", savedUser.getUsername(), currentUserId);
        return savedUser;
    }

    @Transactional
    public User updateUser(Long id, UserUpdateRequest request, Long currentUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        StringBuilder changes = new StringBuilder();

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Email '" + request.getEmail() + "' is already registered");
            }
            changes.append("email: ").append(user.getEmail()).append(" -> ").append(request.getEmail()).append("; ");
            user.setEmail(request.getEmail());
        }

        if (request.getFullName() != null && !request.getFullName().equals(user.getFullName())) {
            changes.append("fullName: ").append(user.getFullName()).append(" -> ").append(request.getFullName()).append("; ");
            user.setFullName(request.getFullName());
        }

        if (request.getEnabled() != null && !request.getEnabled().equals(user.getEnabled())) {
            changes.append("enabled: ").append(user.getEnabled()).append(" -> ").append(request.getEnabled()).append("; ");
            user.setEnabled(request.getEnabled());
        }

        User updatedUser = userRepository.save(user);

        auditLogService.log(
                currentUserId,
                AuditAction.USER_UPDATE,
                "User",
                id,
                "Updated user: " + user.getUsername() + ". Changes: " + changes,
                null,
                null
        );

        logger.info("User '{}' updated by userId={}", user.getUsername(), currentUserId);
        return updatedUser;
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional(readOnly = true)
    public Page<User> listUsers(Pageable pageable, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return userRepository.searchByKeyword(keyword, pageable);
        }
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void deleteUser(Long id, Long currentUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Soft-delete: disable the user rather than removing the record
        user.setEnabled(false);
        user.setAccountLocked(true);
        userRepository.save(user);

        auditLogService.log(
                currentUserId,
                AuditAction.USER_DELETE,
                "User",
                id,
                "Soft-deleted user: " + user.getUsername(),
                null,
                null
        );

        logger.info("User '{}' soft-deleted by userId={}", user.getUsername(), currentUserId);
    }

    @Transactional
    public void resetPassword(Long userId, PasswordResetRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        passwordPolicyValidator.validate(request.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.log(
                userId,
                AuditAction.USER_PASSWORD_RESET,
                "User",
                userId,
                "User reset their own password",
                null,
                null
        );

        logger.info("Password reset by user: userId={}", userId);
    }

    @Transactional
    public void adminResetPassword(Long userId, AdminPasswordResetRequest request,
                                    Long adminId, String workstationId) {
        if (workstationId == null || workstationId.isBlank()) {
            throw new IllegalArgumentException("Workstation ID is required for admin password reset");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        passwordPolicyValidator.validate(request.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        String details = String.format(
                "Admin password reset for user '%s' by adminId=%d from workstation='%s' at %s. Reason: %s",
                user.getUsername(), adminId, workstationId,
                LocalDateTime.now(), request.getReason() != null ? request.getReason() : "N/A"
        );

        auditLogService.log(
                adminId,
                AuditAction.USER_ADMIN_PASSWORD_RESET,
                "User",
                userId,
                details,
                null,
                workstationId
        );

        logger.info("Admin password reset for userId={} by adminId={} from workstation={}",
                userId, adminId, workstationId);
    }
}
