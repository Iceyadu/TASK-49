package com.scholarops.service;

import com.scholarops.exception.ConflictException;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.model.entity.Role;
import com.scholarops.model.entity.User;
import com.scholarops.model.entity.UserRole;
import com.scholarops.repository.PermissionChangeHistoryRepository;
import com.scholarops.repository.RoleRepository;
import com.scholarops.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;
    @Mock private PermissionChangeHistoryRepository permissionChangeHistoryRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private RoleService roleService;

    private User buildUser(Long id, String username) {
        User user = User.builder().id(id).username(username)
                .email(username + "@test.com").enabled(true).accountLocked(false).build();
        user.setUserRoles(new HashSet<>());
        return user;
    }

    private Role buildRole(Long id, String name) {
        return Role.builder().id(id).name(name).build();
    }

    @Test
    void getAllRolesReturnsList() {
        when(roleRepository.findAll()).thenReturn(List.of(
                buildRole(1L, "STUDENT"),
                buildRole(2L, "INSTRUCTOR")));

        List<Role> result = roleService.getAllRoles();

        assertEquals(2, result.size());
        verify(roleRepository).findAll();
    }

    @Test
    void assignRoleSuccessfully() {
        User targetUser = buildUser(10L, "student1");
        Role role = buildRole(2L, "INSTRUCTOR");
        User admin = buildUser(99L, "admin");

        when(userRepository.findById(10L)).thenReturn(Optional.of(targetUser));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);
        when(permissionChangeHistoryRepository.save(any())).thenReturn(null);

        UserRole result = roleService.assignRole(10L, 2L, 99L);

        assertNotNull(result);
        assertEquals("INSTRUCTOR", result.getRole().getName());
        verify(auditLogService).log(eq(99L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void assignRoleThrowsConflictWhenAlreadyAssigned() {
        Role role = buildRole(2L, "INSTRUCTOR");
        User targetUser = buildUser(10L, "student1");
        UserRole existingMapping = UserRole.builder().user(targetUser).role(role).build();
        targetUser.getUserRoles().add(existingMapping);

        User admin = buildUser(99L, "admin");

        when(userRepository.findById(10L)).thenReturn(Optional.of(targetUser));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));

        assertThrows(ConflictException.class, () -> roleService.assignRole(10L, 2L, 99L));
        verify(userRepository, never()).save(any());
    }

    @Test
    void assignRoleThrowsNotFoundForMissingUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roleService.assignRole(99L, 1L, 1L));
    }

    @Test
    void assignRoleThrowsNotFoundForMissingRole() {
        User user = buildUser(10L, "user1");
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roleService.assignRole(10L, 99L, 1L));
    }

    @Test
    void revokeRoleSuccessfully() {
        Role role = buildRole(2L, "INSTRUCTOR");
        User targetUser = buildUser(10L, "student1");
        UserRole existingMapping = UserRole.builder().user(targetUser).role(role).build();
        targetUser.getUserRoles().add(existingMapping);

        User admin = buildUser(99L, "admin");

        when(userRepository.findById(10L)).thenReturn(Optional.of(targetUser));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);
        when(permissionChangeHistoryRepository.save(any())).thenReturn(null);

        roleService.revokeRole(10L, 2L, 99L);

        assertTrue(targetUser.getUserRoles().isEmpty());
        verify(auditLogService).log(eq(99L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void revokeRoleThrowsNotFoundWhenUserDoesNotHaveRole() {
        Role role = buildRole(2L, "INSTRUCTOR");
        User targetUser = buildUser(10L, "student1");
        User admin = buildUser(99L, "admin");

        when(userRepository.findById(10L)).thenReturn(Optional.of(targetUser));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));

        assertThrows(ResourceNotFoundException.class, () -> roleService.revokeRole(10L, 2L, 99L));
    }
}
