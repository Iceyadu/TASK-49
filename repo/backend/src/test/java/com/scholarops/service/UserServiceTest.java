package com.scholarops.service;

import com.scholarops.exception.PasswordPolicyViolationException;
import com.scholarops.exception.UnauthorizedException;
import com.scholarops.model.dto.AdminPasswordResetRequest;
import com.scholarops.model.dto.PasswordResetRequest;
import com.scholarops.model.dto.UserCreateRequest;
import com.scholarops.model.dto.UserUpdateRequest;
import com.scholarops.model.entity.User;
import com.scholarops.repository.UserRepository;
import com.scholarops.security.PasswordPolicyValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordPolicyValidator passwordPolicyValidator;
    @Mock private AuditLogService auditLogService;
    @Mock private RoleService roleService;

    @InjectMocks
    private UserService userService;

    @Test
    void testCreateUser() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("StrongPass1!xy");
        request.setFullName("New User");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        User result = userService.createUser(request, 99L);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        verify(passwordPolicyValidator).validate("StrongPass1!xy");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUserWeakPassword() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("weakuser");
        request.setEmail("weak@test.com");
        request.setPassword("weak");

        when(userRepository.existsByUsername("weakuser")).thenReturn(false);
        when(userRepository.existsByEmail("weak@test.com")).thenReturn(false);
        doThrow(new PasswordPolicyViolationException(java.util.List.of("too short")))
                .when(passwordPolicyValidator).validate("weak");

        assertThrows(PasswordPolicyViolationException.class,
                () -> userService.createUser(request, 99L));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdateUser() {
        User existing = User.builder().id(1L).username("user1")
                .email("old@test.com").fullName("Old Name")
                .enabled(true).accountLocked(false).build();

        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("new@test.com");
        request.setFullName("New Name");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(1L, request, 99L);

        assertEquals("new@test.com", result.getEmail());
        assertEquals("New Name", result.getFullName());
    }

    @Test
    void testDeleteUser() {
        User existing = User.builder().id(1L).username("user1")
                .email("user@test.com").enabled(true).accountLocked(false).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.deleteUser(1L, 99L);

        assertFalse(existing.getEnabled());
        assertTrue(existing.getAccountLocked());
        verify(userRepository).save(existing);
    }

    @Test
    void testAdminResetPasswordWithWorkstationId() {
        User existing = User.builder().id(1L).username("user1")
                .email("user@test.com").passwordHash("old-hash")
                .enabled(true).accountLocked(false).build();

        AdminPasswordResetRequest request = new AdminPasswordResetRequest();
        request.setNewPassword("NewStrongPass1!");
        request.setWorkstationId("WS-001");
        request.setReason("User locked out");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("NewStrongPass1!")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.adminResetPassword(1L, request, 99L, "WS-001");

        assertEquals("new-hash", existing.getPasswordHash());
        verify(passwordPolicyValidator).validate("NewStrongPass1!");
        verify(auditLogService).log(eq(99L), any(), any(), any(), any(), any(), eq("WS-001"));
    }

    @Test
    void testAdminResetPasswordMissingWorkstationId() {
        AdminPasswordResetRequest request = new AdminPasswordResetRequest();
        request.setNewPassword("NewStrongPass1!");
        request.setWorkstationId("");

        assertThrows(IllegalArgumentException.class,
                () -> userService.adminResetPassword(1L, request, 99L, null));
    }

    @Test
    void testResetPasswordWrongOldPassword() {
        User existing = User.builder().id(1L).username("user1")
                .passwordHash("hashed-old").enabled(true).accountLocked(false).build();

        PasswordResetRequest request = new PasswordResetRequest();
        request.setOldPassword("wrongold");
        request.setNewPassword("NewStrongPass1!");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrongold", "hashed-old")).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> userService.resetPassword(1L, request));
    }
}
